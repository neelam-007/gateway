package com.l7tech.server.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.SSLSocketFactoryWrapper;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.event.EntityClassEvent;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Functions;
import com.l7tech.util.SyspropUtil;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initialize the default SSL Context.
 *
 * <p>This configures the default SSL context to use the Gateway key and trust
 * managers.</p>
 */
public class SslContextInitializer {

    //- PUBLIC

    /**
     *
     */
    public SslContextInitializer( final X509TrustManager trustManager,
                                  final SsgKeyStoreManager ssgKeyStoreManager,
                                  final ApplicationEventProxy applicationEventProxy ) {
        this.trustManager = trustManager;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.applicationEventProxy = applicationEventProxy;
    }

    public void init() {
        if ( INSTALL_DEFAULT_SSL_CONTEXT ) {
            updateDefaultSslContext();
            applicationEventProxy.addApplicationListener( applicationListener );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( SslContextInitializer.class.getName() );

    private static final String PROP_INSTALL_DEFAULT_SSL_CONTEXT = "com.l7tech.server.security.ssl.installDefaultSslContext";
    private static final String PROP_DEFAULT_SSL_PROTOCOL = "com.l7tech.server.security.cert.sslProtocolDefault";
    private static final String PROP_SUPPORTED_SSL_PROTOCOLS  = "com.l7tech.server.security.cert.sslProtocolsEnabled";

    private static final boolean INSTALL_DEFAULT_SSL_CONTEXT = SyspropUtil.getBoolean(PROP_INSTALL_DEFAULT_SSL_CONTEXT, false );
    private static final String DEFAULT_SSL_PROTOCOL = SyspropUtil.getString( PROP_DEFAULT_SSL_PROTOCOL, "TLSv1" );
    private static final String[] SUPPORTED_SSL_PROTOCOLS = SyspropUtil.getString( PROP_SUPPORTED_SSL_PROTOCOLS, "SSLv3, TLSv1" ).split( "[,\\s]{1,128}" );

    private final X509TrustManager trustManager;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final ApplicationEventProxy applicationEventProxy;

    private ApplicationListener applicationListener = new ApplicationListener(){
        @Override
        public void onApplicationEvent( final ApplicationEvent applicationEvent) {
            if ( applicationEvent instanceof EntityClassEvent) {
                final EntityClassEvent event = (EntityClassEvent) applicationEvent;
                if ( KeystoreFile.class.equals(event.getEntityClass()) || TrustedCert.class.equals(event.getEntityClass()) ) {
                    updateDefaultSslContext();
                }
            }
        }
    };

    /**
     * Update the default SSL Context.
     *
     * <p>It is not necessary to recreate the context with some SSL providers
     * (e.g. Sun), but is required with others (e.g. SSL-J) since they load all 
     * the private key information when the context is first initialized.</p>
     */
    private void updateDefaultSslContext() {
        logger.info( "(Re)Initializing default SSL context." );
        try {
            final SSLContext context = SSLContext.getInstance( DEFAULT_SSL_PROTOCOL );
            context.init(
                    new KeyManager[]{ new SsgKeyStoreKeyManager(ssgKeyStoreManager) },
                    new TrustManager[]{ trustManager },
                    null );

            SSLContext.setDefault( new SSLContext( new DelegatingSSLContextSpi(context){
                @Override
                protected SSLSocketFactory engineGetSocketFactory() {
                    logger.finer( "Getting socket factory" );

                    final SSLSocketFactory sslSocketFactory = super.engineGetSocketFactory();
                    return new SSLSocketFactoryWrapper(sslSocketFactory){
                        @Override
                        protected Socket notifySocket( final Socket socket ) {
                            logger.finer( "Getting socket " + ( socket instanceof SSLSocket ) );

                            if ( socket instanceof SSLSocket ) {
                                final SSLSocket sslSocket = (SSLSocket) socket;
                                sslSocket.setEnabledProtocols( SUPPORTED_SSL_PROTOCOLS.clone() );
                            }

                            return socket;
                        }
                    };
                }
            }, context.getProvider(), context.getProtocol() ){} );
        } catch ( NoSuchAlgorithmException e ) {
            logger.log( Level.WARNING, "Error initializing SSL context", e );
        } catch ( KeyManagementException e ) {
            logger.log( Level.WARNING, "Error initializing SSL context", e );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error initializing SSL context", e );
        }
    }

    private static class SsgKeyStoreKeyManager extends X509ExtendedKeyManager {

        private final SsgKeyStoreManager ssgKeyStoreManager;

        private SsgKeyStoreKeyManager( final SsgKeyStoreManager ssgKeyStoreManager ) {
            this.ssgKeyStoreManager = ssgKeyStoreManager;
        }

        @Override
        public String[] getClientAliases( final String keyType,
                                          final Principal[] issuers ) {
            if ( logger.isLoggable( Level.FINE )) {
                logger.log( Level.FINE, "Getting client aliases " + keyType + " " + (issuers==null ? "[]" :  Arrays.asList( issuers )));
            }

            final List<String> aliases = new ArrayList<String>();
            try {
                for ( final SsgKeyFinder ssgKeyFinder : ssgKeyStoreManager.findAll() ) {
                    for ( final String alias : ssgKeyFinder.getAliases() ) {
                        final SsgKeyEntry entry = ssgKeyFinder.getCertificateChain( alias );
                        if ( entry.isPrivateKeyAvailable() &&
                             !CertUtils.isCertCaCapable( entry.getCertificate() ) &&
                             (keyType == null || keyType.equals( entry.getPrivateKey().getAlgorithm() )) &&
                             issuerMatches( issuers, entry.getCertificate().getIssuerX500Principal() ) ) {
                            aliases.add( alias );
                        }
                    }
                }
            } catch ( KeyStoreException e ) {
                logger.log( Level.WARNING, "Error getting client aliases.", e);
            } catch ( ObjectNotFoundException e ) {
                logger.log( Level.WARNING, "Error getting client aliases.", e);
            } catch ( FindException e ) {
                logger.log( Level.WARNING, "Error getting client aliases.", e);
            } catch ( UnrecoverableKeyException e ) {
                logger.log( Level.WARNING, "Error getting client aliases.", e);
            }

            return aliases.isEmpty() ? null : aliases.toArray( new String[ aliases.size() ] );
        }

        private boolean issuerMatches( final Principal[] issuers,
                                       final X500Principal issuerX500Principal ) {
            boolean match = false;

            if ( issuers == null ) {
                match = true;
            } else {
                for ( final Principal principal : issuers ) {
                    if ( issuerX500Principal.equals( principal ) ) {
                        match = true;
                        break;
                    } else if ( logger.isLoggable( Level.FINEST )) {
                        logger.log( Level.FINEST,  "Principal does not match " + principal.getName() + " / " + issuerX500Principal.getName() );
                    }
                }

            }
            return match;
        }

        @Override
        public String chooseClientAlias( final String[] keyTypes,
                                         final Principal[] issuers,
                                         final Socket socket ) {
            return doChooseClientAlias( keyTypes, issuers );
        }

        @Override
        public String chooseEngineClientAlias( final String[] keyTypes,
                                               final Principal[] issuers,
                                               final SSLEngine sslEngine ) {
            return doChooseClientAlias( keyTypes, issuers );
        }

        private String doChooseClientAlias( final String[] keyTypes,
                                            final Principal[] issuers ) {
            String alias = null;

            if ( keyTypes != null ) {
                for ( final String keyType : keyTypes ) {
                    final String[] aliases = getClientAliases( keyType, issuers );
                    if ( aliases != null && aliases.length > 0 && aliases[0] != null ) {
                        alias = aliases[0];
                        break;
                    }
                }
            }

            return alias;
        }

        @Override
        public X509Certificate[] getCertificateChain( final String alias ) {
            if ( logger.isLoggable( Level.FINE )) {
                logger.log( Level.FINE, "Getting certificate chain for alias '" + alias + "'." );
            }

            try {
                return ssgKeyStoreManager.lookupKeyByKeyAlias( alias, GoidEntity.DEFAULT_GOID).getCertificateChain();
            } catch ( ObjectNotFoundException e ) {
                logger.log( Level.INFO, "Error getting certificate chain (not found for alias '"+alias+"').");
            } catch ( FindException e ) {
                logger.log( Level.WARNING, "Error getting certificate chain.", e);
            } catch ( KeyStoreException e ) {
                logger.log( Level.WARNING, "Error getting certificate chain.", e);
            }

            return null;
        }

        @Override
        public PrivateKey getPrivateKey( final String alias ) {
            if ( logger.isLoggable( Level.FINE )) {
                logger.log( Level.FINE, "Getting private key for alias '" + alias + "'." );
            }

            try {
                return ssgKeyStoreManager.lookupKeyByKeyAlias( alias, GoidEntity.DEFAULT_GOID ).getPrivateKey();
            } catch ( ObjectNotFoundException e ) {
                logger.log( Level.INFO, "Error getting private key (not found for alias '"+alias+"').");
            } catch ( UnrecoverableKeyException e ) {
                logger.log( Level.WARNING, "Error getting private key.", e);
            } catch ( FindException e ) {
                logger.log( Level.WARNING, "Error getting private key.", e);
            } catch ( KeyStoreException e ) {
                logger.log( Level.WARNING, "Error getting private key.", e);
            }

            return null;
        }

        @Override
        public String[] getServerAliases( final String keyType, final Principal[] issuers ) {
            return getClientAliases( keyType, issuers );
        }

        @Override
        public String chooseServerAlias( final String keyType, final Principal[] issuers, final Socket socket ) {
            return chooseClientAlias( new String[]{keyType}, issuers, socket );
        }

        @Override
        public String chooseEngineServerAlias( final String keyType, final Principal[] issuers, final SSLEngine sslEngine ) {
            return chooseEngineClientAlias( new String[]{keyType}, issuers, sslEngine );
        }
    }

    private static class DelegatingSSLContextSpi extends SSLContextSpi {
        private final SSLContext sslContext;

        private DelegatingSSLContextSpi( final SSLContext sslContext ) {
            this.sslContext = sslContext;
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            return sslContext.createSSLEngine();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine( final String s, final int i ) {
            return sslContext.createSSLEngine( s, i );
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return new SSLSocketFactoryWrapper( new Functions.Nullary<SSLSocketFactory>(){
                @Override
                public SSLSocketFactory call() {
                    return sslContext.getSocketFactory();
                }
            } );
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return sslContext.getServerSocketFactory();
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return sslContext.getServerSessionContext();
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return sslContext.getClientSessionContext();
        }

        @Override
        protected void engineInit( final KeyManager[] keyManagers,
                                   final TrustManager[] trustManagers,
                                   final SecureRandom secureRandom ) throws KeyManagementException {
            sslContext.init( keyManagers, trustManagers, secureRandom );
        }
    }
}
