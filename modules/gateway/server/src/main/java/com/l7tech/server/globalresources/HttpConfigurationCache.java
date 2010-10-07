package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.NtlmAuthentication;
import com.l7tech.common.io.SSLSocketFactoryWrapper;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache for HTTP Configuration data
 */
public class HttpConfigurationCache implements ApplicationListener, InitializingBean, PropertyChangeListener {

    //- PUBLIC

    public HttpConfigurationCache( final Config config,
                                   final DefaultHttpProxyManager defaultHttpProxyManager,
                                   final HttpConfigurationManager httpConfigurationManager,
                                   final SecurePasswordManager securePasswordManager,
                                   final HostnameVerifier hostnameVerifier,
                                   final SsgKeyStoreManager ssgKeyStoreManager,
                                   final DefaultKey defaultKey,
                                   final X509TrustManager trustManager ) {
        this.config = config;
        this.defaultHttpProxyManager = defaultHttpProxyManager;
        this.httpConfigurationManager = httpConfigurationManager;
        this.securePasswordManager = securePasswordManager;
        this.hostnameVerifier = hostnameVerifier;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.defaultKey = defaultKey;
        this.trustManager = trustManager;
    }

    public void configure( final GenericHttpRequestParams httpRequestParameters,
                           final boolean useSslKeyForDefault ) {
        final URL url = httpRequestParameters.getTargetUrl();
        final HttpConfig httpConfig = configurationRef.get();

        boolean configured = false;
        if ( url != null && httpConfig != null ) {
            final HttpConfiguration httpConfiguration = httpConfig.getHttpConfiguration( url );
            if ( httpConfiguration != null ) {
                configured = true;

                if ( httpConfiguration.getUsername() != null ) {
                    if ( httpConfiguration.getNtlmDomain() != null ) {
                        httpRequestParameters.setNtlmAuthentication( new NtlmAuthentication(
                            httpConfiguration.getUsername(),
                            httpConfig.getPassword(httpConfiguration.getPasswordOid()),
                            httpConfiguration.getNtlmDomain(),
                            httpConfiguration.getNtlmHost()==null ? config.getProperty( "clusterHost", "localhost" ): httpConfiguration.getNtlmHost()
                        ) );
                    } else {
                        httpRequestParameters.setPasswordAuthentication( new PasswordAuthentication(
                            httpConfiguration.getUsername(),
                            httpConfig.getPassword(httpConfiguration.getPasswordOid())
                        ) );
                    }
                }

                try {
                    //TODO [steve] implement correctly if possible (needs to be dynamic)
                    final SSLContext sslContext = getSSLContext( httpConfiguration );
                    SSLSocketFactory socketFactory = sslContext.getSocketFactory();
                    if ( httpConfiguration.getTlsVersion() != null ) {
                        final String tlsVersion = httpConfiguration.getTlsVersion();
                        socketFactory = new SSLSocketFactoryWrapper( socketFactory ) {
                            @Override
                            protected Socket notifySocket( final Socket socket ) {
                                if ( socket instanceof SSLSocket ) {
                                    final SSLSocket sslSocket = (SSLSocket) socket;
                                    sslSocket.setEnabledProtocols( new String[]{ tlsVersion } );
                                }
                                return socket;
                            }
                        };
                    }

                    httpRequestParameters.setSslSocketFactory( socketFactory );
                } catch ( NoSuchAlgorithmException e ) {
                    logger.log( Level.WARNING, "Error creating SSL context.", e );
                } catch ( KeyManagementException e ) {
                    logger.log( Level.WARNING, "Error creating SSL context.", e );
                }

                httpRequestParameters.setConnectionTimeout( httpConfiguration.getConnectTimeout() );
                httpRequestParameters.setReadTimeout( httpConfiguration.getReadTimeout() );
                httpRequestParameters.setFollowRedirects( httpConfiguration.isFollowRedirects() );
                httpRequestParameters.setUseDefaultProxy( httpConfiguration.getProxyUse() != HttpConfiguration.Option.NONE );        
            }
        }

        if ( !configured ) { // set defaults
            httpRequestParameters.setUseDefaultProxy( true );
            try {
                final SSLContext sslContext = getDefaultSSLContext(useSslKeyForDefault);
                httpRequestParameters.setSslSocketFactory( sslContext.getSocketFactory() );
            } catch ( NoSuchAlgorithmException e ) {
                logger.log( Level.WARNING, "Error creating SSL context.", e );
            } catch ( KeyManagementException e ) {
                logger.log( Level.WARNING, "Error creating SSL context.", e );
            }
        }

        httpRequestParameters.setHostnameVerifier( hostnameVerifier );
    }

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        if ( evt.getPropertyName().startsWith( "keyStore" ) ) {
            logger.config( "Invalidating cached SSL context for default key." );
            defaultKeySSLContext.set( null ); // invalidate cached SSLContext
        } else {
            loadConfiguration();
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityInvalidationEvent ) {
            final EntityInvalidationEvent invalidationEvent = (EntityInvalidationEvent) event;
            if ( HttpConfiguration.class.isAssignableFrom( invalidationEvent.getEntityClass() ) ||
                 SecurePassword.class.isAssignableFrom( invalidationEvent.getEntityClass() ) ) {
                loadConfiguration();
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        loadConfiguration();

        installProxySelector( new HttpConfigProxySelector( configurationRef ) );
        installAuthenticator( new HttpConfigAuthenticator( configurationRef ) );
    }

    //- PROTECTED

    protected void installProxySelector( final ProxySelector proxySelector ) {
        ProxySelector.setDefault( proxySelector );
    }

    protected void installAuthenticator( final Authenticator authenticator ) {
        Authenticator.setDefault( authenticator );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( HttpConfigurationCache.class.getName() );

    private final Config config;
    private final DefaultHttpProxyManager defaultHttpProxyManager;
    private final HttpConfigurationManager httpConfigurationManager;
    private final SecurePasswordManager securePasswordManager;
    private final HostnameVerifier hostnameVerifier;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final DefaultKey defaultKey;
    private final X509TrustManager trustManager;

    private final AtomicReference<SSLContext> defaultSSLContext = new AtomicReference<SSLContext>();
    private final AtomicReference<SSLContext> defaultKeySSLContext = new AtomicReference<SSLContext>();
    private final AtomicReference<HttpConfig> configurationRef = new AtomicReference<HttpConfig>();

    private void loadConfiguration() {
        logger.config( "(Re)Loading HTTP configuration." );
        try {
            configurationRef.set( new HttpConfig(
                    defaultHttpProxyManager.getDefaultHttpProxyConfiguration(),
                    httpConfigurationManager.findAll(),
                    securePasswordManager.findAll(),
                    new Functions.Binary<char[],String,String>(){
                        @Override
                        public char[] call( final String name, final String encryptedPassword ) {
                            char[] password = new char[0];
                            try {
                                password = securePasswordManager.decryptPassword( encryptedPassword );
                            } catch ( FindException e ) {
                                logger.log( Level.WARNING,
                                        "Unable to decrypt secure password '"+name+"' : " + ExceptionUtils.getMessage(e),
                                        ExceptionUtils.getDebugException(e) );
                            } catch ( ParseException e ) {
                                logger.log( Level.WARNING,
                                        "Unable to decrypt secure password '"+name+"' : " + ExceptionUtils.getMessage(e),
                                        ExceptionUtils.getDebugException(e) );
                            }
                            return password;
                        }
                    }
            ) );
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error loading HTTP configuration.", e );
        }
    }

    @SuppressWarnings({ "UseOfArchaicSystemPropertyAccessors" })
    private SSLContext getSSLContext( final HttpConfiguration httpConfiguration ) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = null;
        try {
            switch ( httpConfiguration.getTlsKeyUse() ) {
                case NONE:
                    sslContext = getDefaultSSLContext( false );
                    break;
                case CUSTOM:
                    //TODO [steve] cache custom SSL contexts
                    sslContext = SSLContext.getInstance( "TLS" );
                    final int timeout = Integer.getInteger( HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                            HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT );
                    sslContext.getClientSessionContext().setSessionTimeout( timeout );

                    final String keyAlias = httpConfiguration.getTlsKeystoreAlias()==null ? "" : httpConfiguration.getTlsKeystoreAlias();
                    final long keystoreId = httpConfiguration.getTlsKeystoreOid();
                    final SsgKeyEntry keyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias( keyAlias, keystoreId );
                    final X509Certificate[] certChain = keyEntry.getCertificateChain();
                    final PrivateKey privateKey = keyEntry.getPrivateKey();
                    final KeyManager[] keyManagers = new KeyManager[]{ new SingleCertX509KeyManager( certChain, privateKey ) };
                    sslContext.init( keyManagers, new TrustManager[]{ trustManager }, null );
                    break;
            }
        } catch ( KeyStoreException e ) {
            logger.log( Level.WARNING, "Error getting key for SSL.", e );
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error getting key for SSL.", e );
        } catch ( UnrecoverableKeyException e ) {
            logger.log( Level.WARNING, "Error getting key for SSL.", e );
        }

        if ( sslContext == null ) {
            sslContext = getDefaultSSLContext( true );
        }

        return sslContext;
    }

    private SSLContext getDefaultSSLContext( final boolean useSslKeyForDefault ) throws NoSuchAlgorithmException, KeyManagementException {
        final AtomicReference<SSLContext> ref = useSslKeyForDefault ? defaultKeySSLContext : defaultSSLContext;

        SSLContext sslContext = ref.get();
        if ( sslContext == null ) {
            sslContext = buildDefaultSSLContext( useSslKeyForDefault );
            ref.compareAndSet( null, sslContext );
        }

        return sslContext;
    }

    @SuppressWarnings({ "UseOfArchaicSystemPropertyAccessors" })
    private SSLContext buildDefaultSSLContext( final boolean useSslKeyForDefault ) throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final int timeout = Integer.getInteger( HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT);
        sslContext.getClientSessionContext().setSessionTimeout(timeout);
        sslContext.init( useSslKeyForDefault ? defaultKey.getSslKeyManagers() : null, new TrustManager[]{trustManager}, null );
        return sslContext;
    }

    private static final class HttpConfig {
        private final HttpProxyConfiguration httpProxyConfiguration;
        private final Map<String,Collection<HttpConfiguration>> httpConfigurationsByHost;
        private final Map<Long,char[]> passwordMap;

        HttpConfig( final HttpProxyConfiguration httpProxyConfiguration,
                    final Collection<HttpConfiguration> httpConfigurations,
                    final Collection<SecurePassword> passwords,
                    final Functions.Binary<char[],String,String> passwordDecryptor ) {
            this.httpProxyConfiguration = httpProxyConfiguration==null||httpProxyConfiguration.getHost()==null ?
                    null :
                    new HttpProxyConfiguration( httpProxyConfiguration, true );
            this.httpConfigurationsByHost = Collections.unmodifiableMap( Functions.reduce( httpConfigurations, new HashMap<String,Collection<HttpConfiguration>>(), new Functions.Binary<Map<String,Collection<HttpConfiguration>>,Map<String,Collection<HttpConfiguration>>,HttpConfiguration>(){
                @Override
                public Map<String, Collection<HttpConfiguration>> call( final Map<String, Collection<HttpConfiguration>> map, final HttpConfiguration httpConfiguration ) {
                    if ( httpConfiguration.getHost() != null ) {
                        final String host = httpConfiguration.getHost().toLowerCase();
                        Collection<HttpConfiguration> configurationsForHost = map.get( host );
                        if ( configurationsForHost == null ) {
                            configurationsForHost = new ArrayList<HttpConfiguration>();
                            map.put( host, configurationsForHost );
                        }
                        configurationsForHost.add( httpConfiguration );
                    }

                    return map;
                }
            } ) );
            this.passwordMap = Collections.unmodifiableMap( Functions.reduce( passwords, new HashMap<Long,char[]>(), new Functions.Binary<Map<Long,char[]>,Map<Long,char[]>,SecurePassword>(){
                @Override
                public Map<Long, char[]> call( final Map<Long, char[]> map, final SecurePassword securePassword ) {
                    map.put( securePassword.getOid(), passwordDecryptor.call( securePassword.getName(), securePassword.getEncodedPassword() ) );
                    return map;
                }
            } ) );
        }

        /**
         * Get the HTTP proxy configuration for the given URI.
         *
         * @param uri The URI to access proxy configuration for
         * @return The proxy configuration or null for none
         */
        public HttpProxyConfiguration getHttpProxyConfiguration( final URI uri ) {
            if ( !uri.isAbsolute() ) return null;
            return getHttpProxyConfiguration( uri.getScheme().toLowerCase(), uri.getHost(), uri.getPort(), uri.getRawPath() );
        }

        /**
         * Get the HTTP proxy configuration for the given URL.
         *
         * @param url The URL to access proxy configuration for
         * @return The proxy configuration or null for none
         */
        public HttpProxyConfiguration getHttpProxyConfiguration( final URL url ) {
            return getHttpProxyConfiguration( url.getProtocol().toLowerCase(), url.getHost(), url.getPort(), url.getPath() );   
        }

        /**
         * Get the HTTP proxy configuration for the given URI.
         *
         * @param protocol The protocol to access proxy configuration for (may be null)
         * @param host The host to access proxy configuration for (required)
         * @param port The port to access proxy configuration for
         * @param path The path to access proxy configuration for (may be null)
         * @return The proxy configuration or null for none
         */
        public HttpProxyConfiguration getHttpProxyConfiguration( final String protocol,
                                                                 final String host,
                                                                 final int port,
                                                                 final String path ) {
            HttpProxyConfiguration proxyConfiguration = null;

            final HttpConfiguration httpConfiguration = getHttpConfiguration( protocol, host, port, path );
            boolean skipProxy = false;

            if ( httpConfiguration != null ) {
                if ( httpConfiguration != null ) {
                    switch ( httpConfiguration.getProxyUse() ) {
                        case NONE:
                            skipProxy = true;
                            break;
                        case CUSTOM:
                            final HttpProxyConfiguration httpProxyConfiguration = httpConfiguration.getProxyConfiguration();
                            if ( httpProxyConfiguration.getHost() != null ) {
                                proxyConfiguration = httpProxyConfiguration;
                            }
                            break;
                    }
                }
            }

            if ( proxyConfiguration == null && !skipProxy ) {
                proxyConfiguration = httpProxyConfiguration;
            }

            return proxyConfiguration;
        }

        public HttpConfiguration getHttpConfiguration( final URI uri ) {
            if ( !uri.isAbsolute() ) return null;
            return getHttpConfiguration( uri.getScheme().toLowerCase(), uri.getHost(), uri.getPort(), uri.getRawPath() );
        }
        
        public HttpConfiguration getHttpConfiguration( final URL url ) {
            return getHttpConfiguration( url.getProtocol().toLowerCase(), url.getHost(), url.getPort(), url.getPath() );
        }


        public HttpConfiguration getHttpConfiguration( final String protocol,
                                                       final String host,
                                                       final int port,
                                                       final String path ) {
            HttpConfiguration httpConfiguration = null;

            if ( host != null ) {
                final Collection<HttpConfiguration> httpConfigurations = httpConfigurationsByHost.get( host.toLowerCase() );
                if ( httpConfigurations != null ) {
                    final int realPort = port==-1 ?
                        "http".equals( protocol ) ? 80 : 443 :
                        port;

                    final Pair<HttpConfiguration,Long> httpConfigurationAndScore =
                            Functions.reduce( httpConfigurations, null, new Functions.Binary<Pair<HttpConfiguration,Long>,Pair<HttpConfiguration,Long>,HttpConfiguration>(){
                        @Override
                        public Pair<HttpConfiguration,Long> call( final Pair<HttpConfiguration,Long> currentMatch,
                                                                  final HttpConfiguration httpConfiguration ) {
                            Pair<HttpConfiguration,Long> bestMatch = currentMatch;

                            final long score = match( httpConfiguration, protocol, realPort, path );
                            if ( score > 0 && (currentMatch == null || score > currentMatch.right) ) {
                                bestMatch = new Pair<HttpConfiguration,Long>( httpConfiguration, score );
                            }

                            return bestMatch;
                        }
                    } );

                    if ( httpConfigurationAndScore != null ) {
                        httpConfiguration = httpConfigurationAndScore.left;
                    }
                }
            }

            return httpConfiguration;
        }

        public synchronized char[] getPassword( final long passwordOid ) {
            char[] password = passwordMap.get( passwordOid );

            if ( password == null ) {
                password = new char[0];
            }

            return password;
        }

        /**
         * The given configuration must be a host match.
         */
        private static long match( final HttpConfiguration configuration,
                            final String protocol,
                            final int port,
                            final String path ) {
            long score = 0;

            if ( (configuration.getProtocol()==null || (configuration.getProtocol().matches(protocol))) &&
                 (configuration.getPort()==0 || configuration.getPort()==port) &&
                 (configuration.getPath()==null || (path!=null && path.startsWith(configuration.getPath())) ) ) {
                score = 1; // 1 for any match
                score = score << 1;
                score += configuration.getPort()!=0 ? 1 : 0;
                score = score << 1;
                score += configuration.getProtocol()!=null ? 1 : 0;
                score = score << 16;  // max path length currently 4096
                score += configuration.getPath()!=null ? configuration.getPath().length() : 0;
            }

            return score;
        }
    }

    private static final class HttpConfigProxySelector extends ProxySelector {
        private static final List<Proxy> NO_PROXY = Collections.singletonList(Proxy.NO_PROXY);
        private final AtomicReference<HttpConfig> configurationRef;

        private HttpConfigProxySelector( final AtomicReference<HttpConfig> configurationRef ) {
            this.configurationRef = configurationRef;
        }

        @Override
        public void connectFailed( final URI uri, final SocketAddress sa, final IOException e ) {
            // failover here when supported
            logger.log( Level.WARNING, 
                    "Connection failed for proxy '"+sa+"', due to : " + ExceptionUtils.getMessage( e ),
                    ExceptionUtils.getDebugException( e ) );
        }

        @Override
        public List<Proxy> select( final URI uri ) {
            List<Proxy> proxies = NO_PROXY;
            final HttpConfig httpConfig = configurationRef.get();

            if ( httpConfig != null && (uri.getScheme()!=null && uri.getScheme().toLowerCase().startsWith( "http" )) ) {
                final HttpProxyConfiguration proxyConfiguration = httpConfig.getHttpProxyConfiguration( uri );
                if ( proxyConfiguration != null ) {
                    final Proxy proxy = new Proxy(
                            Proxy.Type.HTTP,
                            new InetSocketAddress(proxyConfiguration.getHost(), proxyConfiguration.getPort()) );
                    proxies = Collections.singletonList( proxy );
                }
            }

            return proxies;
        }
    }

    private static final class HttpConfigAuthenticator extends Authenticator {
        private final AtomicReference<HttpConfig> configurationRef;

        private HttpConfigAuthenticator( final AtomicReference<HttpConfig> configurationRef ) {
            this.configurationRef = configurationRef;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            PasswordAuthentication passwordAuthentication = null;

            final HttpConfig httpConfig = configurationRef.get();
            final RequestorType type = this.getRequestorType();
            final URL requestingUrl = this.getRequestingURL();
            final String requestingHost = this.getRequestingHost();
            final int requestingPort = this.getRequestingPort();

            if ( httpConfig != null ) {
                if ( RequestorType.PROXY == type ) {
                    HttpProxyConfiguration proxyConfiguration = null;
                    if ( requestingUrl != null ) {
                        proxyConfiguration = httpConfig.getHttpProxyConfiguration( requestingUrl );
                    } else if ( requestingHost != null ) {
                        proxyConfiguration = httpConfig.getHttpProxyConfiguration( null, requestingHost, requestingPort, null );
                    }

                    if ( proxyConfiguration != null && proxyConfiguration.getUsername() != null ) {
                        passwordAuthentication = new PasswordAuthentication(
                                proxyConfiguration.getUsername(),
                                httpConfig.getPassword(proxyConfiguration.getPasswordOid()) );
                    }
                } else if ( RequestorType.SERVER == type ) {
                    HttpConfiguration httpConfiguration = null;
                    if ( requestingUrl != null ) {
                        httpConfiguration = httpConfig.getHttpConfiguration( requestingUrl );
                    } else if ( requestingHost != null ) {
                        httpConfiguration = httpConfig.getHttpConfiguration( null, requestingHost, requestingPort, null );
                    }

                    if ( httpConfiguration != null && httpConfiguration.getUsername() != null ) {
                        String username = httpConfiguration.getUsername();
                        if ( httpConfiguration.getNtlmDomain() != null ) {
                            // The authenticator API was written before NTLM was supported
                            // so you have to add the domain as a prefix.
                            // http://download.oracle.com/javase/6/docs/technotes/guides/net/http-auth.html
                            username = httpConfiguration.getNtlmDomain() + "\\" + username;
                        }
                        passwordAuthentication = new PasswordAuthentication(
                                username,
                                httpConfig.getPassword(httpConfiguration.getPasswordOid()) );
                    }
                }
            }

            return passwordAuthentication;
        }
    }
}
