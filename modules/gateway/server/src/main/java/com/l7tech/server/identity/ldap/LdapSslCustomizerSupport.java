package com.l7tech.server.identity.ldap;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.FilterClassLoader;
import com.l7tech.util.Pair;
import javassist.*;

import javax.naming.NamingException;
import javax.net.ssl.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Support class for LDAP SSL configuration.
 */
public class LdapSslCustomizerSupport {

    //- PUBLIC

    public static class LdapConfigException extends Exception {
        public LdapConfigException(String message) {
            super(message);
        }

        public LdapConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Get an SSLSocketFactory that is initialized to use the given key.
     *
     * @param keystoreIdStr The ID of the keystore to use (or null for none)
     * @param alias The alias of the key to use (or null for none)
     * @return The SSLSocketFactory or null
     * @throws com.l7tech.server.transport.jms.JmsConfigException If an error occurs
     */
    public synchronized static SSLSocketFactory getSocketFactory( final String keystoreIdStr, final String alias ) throws LdapConfigException {
        return doGetSocketFactory( keystoreIdStr, alias );
    }

    /**
     * Get an SSLContext that is initialized to use the given key (or no client auth).
     *
     * @param keystoreIdStr The ID of the keystore to use (or null for none)
     * @param alias The alias of the key to use (or null for none)
     * @return The SSLContext or null
     * @throws com.l7tech.server.transport.jms.JmsConfigException If an error occurs
     */
    public synchronized static SSLContext getSSLContext( final String keystoreIdStr, final String alias ) throws LdapConfigException {
        return doGetSSLContext( keystoreIdStr, alias );
    }

    /**
     * Get the hostname verifier to use for LDAPS connections.
     *
     * @return The hostname verifier or null if one is not in use
     */
    public synchronized static HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Configuration of outbound trust
     */
    public synchronized static void setTrustManager( final X509TrustManager trustManager ) {
        LdapSslCustomizerSupport.trustManager = trustManager;
    }

    /**
     * Configuration of key manager
     */
    public synchronized static void setSsgKeyStoreManager( final SsgKeyStoreManager ssgKeyStoreManager ) {
        LdapSslCustomizerSupport.ssgKeyStoreManager = ssgKeyStoreManager;
    }

    /**
     * Configuration of hostname verifiier
     */
    public synchronized static void setHostnameVerifier( final HostnameVerifier hostnameVerifier ) {
        LdapSslCustomizerSupport.hostnameVerifier = hostnameVerifier;
    }

    /**
     * Get the name for the SSLSocketFactory class with the given options.
     *
     * @param useCertCert True if client certificate authentication should be enabled.
     * @param keystoreOid The keystore OID (null for default key)
     * @param keyAlias The alias for the key in the keystore
     * @return The classname to use
     */
    public static String getSSLSocketFactoryClassname( final boolean useCertCert, final Long keystoreOid, final String keyAlias ) {
        String classname;

        if ( useCertCert && (keystoreOid == null || keystoreOid == -1) && keyAlias == null ) {
            classname = SslClientHostnameAwareSocketFactory.class.getName(); // uses default key
        } else if ( !useCertCert ) {
            classname = LdapSSLSocketFactory.class.getPackage().getName() + ".generated.SSLSocketFactory";
        } else {
            classname = LdapSSLSocketFactory.class.getPackage().getName() + ".generated.SSLSocketFactory_"+keystoreOid+"_"+keyAlias;
        }

        return classname;
    }

    /**
     * Access the ClassLoader to use for loading SSLSocketFactory classes.
     *
     * @return The ClassLoader to use
     * @throws NamingException If an error occurs
     */
    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public static ClassLoader getSSLSocketFactoryClassLoader() throws NamingException {
        if ( classLoader == null ) {
            Translator translator = new Translator(){
                @Override
                public void onLoad( final ClassPool classPool, final String classname ) throws NotFoundException, CannotCompileException {
                    if ( classname.contains(".generated.SSLSocketFactory") ) {
                        try {
                            classPool.get(classname);
                        } catch ( NotFoundException nfe ){
                            final String[] className_params = classname.split("_", 3);
                            final String keystoreIdvalue;
                            final String aliasValue;
                            if ( className_params.length==3 && className_params[0].endsWith(".generated.SSLSocketFactory") ) {
                                keystoreIdvalue = '"' + className_params[1] + '"';
                                aliasValue = '"' + className_params[2] + '"';
                                logger.info("Generating SSLSocketFactory for keystore "+keystoreIdvalue+", alias "+aliasValue+".");
                            } else if ( className_params.length==1 && className_params[0].endsWith(".generated.SSLSocketFactory")  ) {
                                keystoreIdvalue = "null";
                                aliasValue = "null";
                                logger.info("Generating SSLSocketFactory for anonymous SSL.");
                            } else {
                                throw nfe;
                            }

                            CtClass ctClass = classPool.getAndRename( LdapSSLSocketFactory.class.getName(), classname );
                            ctClass.removeMethod(ctClass.getMethod("buildSSLContext", "()Ljavax/net/ssl/SSLContext;"));
                            String methodBody = "protected javax.net.ssl.SSLContext buildSSLContext() { try { return "+LdapSslCustomizerSupport.class.getName()+".getSSLContext( "+keystoreIdvalue+", "+aliasValue+" ); } catch ( Exception lce ) { throw new RuntimeException( lce ); } }";
                            ctClass.addMethod(CtMethod.make(methodBody,ctClass));
                        }
                    }
                }

                @Override
                public void start(ClassPool classPool) throws NotFoundException, CannotCompileException {}
            };

            Loader loader = new Loader( LdapIdentityProviderImpl.class.getClassLoader(), pool );
            loader.delegateLoadingOf( LdapSslCustomizerSupport.class.getName() );
            loader.setDomain(LdapSslCustomizerSupport.class.getProtectionDomain());
            try {
                loader.addTranslator(pool, translator);
            } catch (NotFoundException e) {
                throw (NamingException)new NamingException( "Error in SSL initialization.").initCause(e);
            } catch (CannotCompileException e) {
                throw (NamingException)new NamingException( "Error in SSL initialization.").initCause(e);
            }

            classLoader = new FilterClassLoader( LdapIdentityProviderImpl.class.getClassLoader(), loader, Collections.singleton(LdapSSLSocketFactory.class.getPackage().getName() + ".generated.SSLSocketFactory"), true );
        }

        return classLoader;
    }


    //- PRIVATE

    private static final Logger logger = Logger.getLogger( LdapSslCustomizerSupport.class.getName() );

    /**
     * This name is used for backwards compatibility, do not change.
     */
    private static final String PROP_SSL_SESSION_TIMEOUT = SslClientSocketFactory.class.getName() + ".sslSessionTimeoutSeconds";
    private static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;
    private static final Map<Pair<Long,String>, SSLContext> instancesByKeyEntryId = new HashMap<Pair<Long,String>, SSLContext>();
    private static final ClassPool pool = new ClassPool();
    static {
        pool.insertClassPath(new ClassClassPath(LdapSSLSocketFactory.class));
    }

    private static SsgKeyStoreManager ssgKeyStoreManager;
    private static X509TrustManager trustManager;
    private static HostnameVerifier hostnameVerifier;
    private static ClassLoader classLoader;

    /**
     *
     */
    private static SSLSocketFactory doGetSocketFactory( final String keystoreIdStr, final String alias ) throws LdapConfigException {
        return doGetSSLContext(keystoreIdStr, alias).getSocketFactory();
    }

    /**
     *
     */
    private static SSLContext doGetSSLContext( final String keystoreIdStr, final String alias ) throws LdapConfigException {
        if (ssgKeyStoreManager == null) throw new IllegalStateException("SSG Keystore Manager must be set first");
        if (trustManager == null) throw new IllegalStateException("TrustManager must be set before first use");

        final Pair<Long,String> keyId;
        if ( keystoreIdStr==null && alias == null ) {
            keyId = new Pair<Long,String>(-1L, "");
        } else {
            // process keystore
            long keystoreId;
            try {
                keystoreId = Long.parseLong( keystoreIdStr );
            } catch ( NumberFormatException nfe ) {
                throw new LdapConfigException("Bad keystore ID: " + keystoreIdStr);
            }

            keyId = new Pair<Long,String>(keystoreId, alias);
        }

        SSLContext instance = instancesByKeyEntryId.get(keyId);
        if (instance == null) {
            try {
                KeyManager[] keyManagers;
                if ( keystoreIdStr==null && alias == null ) {
                    keyManagers = null;
                } else {
                    SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keyId.left);
                    KeyManager keyManager = new SingleCertX509KeyManager(entry.getCertificateChain(), entry.getPrivateKey());
                    keyManagers = new KeyManager[] { keyManager };
                }

                final SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagers,
                             new TrustManager[] { trustManager } ,
                             null);
                int timeout = ConfigFactory.getIntProperty( PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT );
                context.getClientSessionContext().setSessionTimeout(timeout);
                instance = context;
            } catch (GeneralSecurityException e) {
                throw new LdapConfigException("Couldn't initialize LDAP client SSL context", e);
            } catch (ObjectModelException e) {
                throw new LdapConfigException("Couldn't initialize LDAP client SSL context", e);
            }

            instancesByKeyEntryId.put(keyId, instance);
        }

        return instance;
    }
}
