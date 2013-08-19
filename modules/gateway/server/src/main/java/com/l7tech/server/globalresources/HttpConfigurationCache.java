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
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.EntityClassEvent;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.net.ssl.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache for HTTP Configuration data
 */
@ManagedResource(description="HTTP Configuration Cache", objectName="l7tech:type=HttpConfigurationCache")
public class HttpConfigurationCache implements PostStartupApplicationListener, InitializingBean, PropertyChangeListener {

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

        HttpProxyConfiguration proxyConfiguration = null;
        boolean configured = false;
        if ( url != null && httpConfig != null ) {
            final HttpConfiguration httpConfiguration = httpConfig.getHttpConfiguration( url );
            if ( httpConfiguration != null ) {
                configured = true;

                if ( httpConfiguration.getUsername() != null ) {
                    if ( httpConfiguration.getNtlmDomain() != null ) {
                        httpRequestParameters.setNtlmAuthentication( new NtlmAuthentication(
                            httpConfiguration.getUsername(),
                            httpConfig.getPassword(httpConfiguration.getPasswordGoid()),
                            httpConfiguration.getNtlmDomain(),
                            httpConfiguration.getNtlmHost()==null ? config.getProperty( "clusterHost", "localhost" ): httpConfiguration.getNtlmHost()
                        ) );
                    } else {
                        httpRequestParameters.setPasswordAuthentication( new PasswordAuthentication(
                            httpConfiguration.getUsername(),
                            httpConfig.getPassword(httpConfiguration.getPasswordGoid())
                        ) );
                    }
                }

                try {
                    SSLSocketFactory socketFactory = getSSLSocketFactory( httpConfiguration );
                    httpRequestParameters.setSslSocketFactory( socketFactory );
                } catch ( NoSuchAlgorithmException e ) {
                    logger.log( Level.WARNING, "Error creating SSL context.", e );
                } catch ( KeyManagementException e ) {
                    logger.log( Level.WARNING, "Error creating SSL context.", e );
                }

                httpRequestParameters.setConnectionTimeout( httpConfiguration.getConnectTimeout() );
                httpRequestParameters.setReadTimeout( httpConfiguration.getReadTimeout() );
                httpRequestParameters.setFollowRedirects( httpConfiguration.isFollowRedirects() );

                switch ( httpConfiguration.getProxyUse() ) {
                    case DEFAULT:
                        proxyConfiguration = httpConfig.httpProxyConfiguration;
                        break;
                    case CUSTOM:
                        proxyConfiguration = httpConfiguration.getProxyConfiguration();
                        break;
                }
            }
        }

        if ( !configured ) { // set defaults
            try {
                final SSLSocketFactory sslSocketFactory = getDefaultSSLSocketFactory(useSslKeyForDefault);
                httpRequestParameters.setSslSocketFactory( sslSocketFactory );
            } catch ( NoSuchAlgorithmException e ) {
                logger.log( Level.WARNING, "Error creating SSL context.", e );
            } catch ( KeyManagementException e ) {
                logger.log( Level.WARNING, "Error creating SSL context.", e );
            }
            proxyConfiguration = httpConfig!=null ? httpConfig.httpProxyConfiguration : null;
        }

        httpRequestParameters.setHostnameVerifier( hostnameVerifier );

        if ( proxyConfiguration != null &&
             proxyConfiguration.getHost() != null ) {
            httpRequestParameters.setProxyHost( proxyConfiguration.getHost() );
            httpRequestParameters.setProxyPort( proxyConfiguration.getPort() );
            if ( proxyConfiguration.getUsername() != null &&
                 proxyConfiguration.getPasswordGoid() != null ) {
                httpRequestParameters.setProxyAuthentication(
                        new PasswordAuthentication(
                                proxyConfiguration.getUsername(),
                                httpConfig.getPassword (proxyConfiguration.getPasswordGoid() )) );
            } else {
                httpRequestParameters.setProxyAuthentication( null );
            }
        }
    }

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        if ( evt.getPropertyName().startsWith( "keyStore" ) ) {
            logger.config( "Invalidating cached SSL context for default key." );
            defaultKeySSLSocketFactory.set( null ); // invalidate cached SSLContext
        } else {
            loadConfiguration();
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityClassEvent) {
            final EntityClassEvent invalidationEvent = (EntityClassEvent) event;
            if ( HttpConfiguration.class.isAssignableFrom( invalidationEvent.getEntityClass() ) ||
                 SecurePassword.class.isAssignableFrom( invalidationEvent.getEntityClass() ) ) {
                loadConfiguration();
            } else if ( KeystoreFile.class.isAssignableFrom( invalidationEvent.getEntityClass() )) {
                defaultKeySSLSocketFactory.set( null );
                sslSocketFactoryMap.clear();
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        loadConfiguration();

        installProxySelector( new HttpConfigProxySelector( configurationRef ) );
        installAuthenticator( new HttpConfigAuthenticator( configurationRef ) );
    }

    @ManagedOperation(description="Rebuild HTTP Configuration Cache")
    public void resetCache() {
        loadConfiguration();
        defaultSSLSocketFactory.set( null );
        defaultKeySSLSocketFactory.set( null );
        sslSocketFactoryMap.clear();
    }

    @ManagedAttribute(description="HTTP Configuration Cache Size", currencyTimeLimit=5)
    public int getHttpConfigurationCacheSize() {
        final HttpConfig config = configurationRef.get();
        return config == null ? 0 : Functions.reduce( config.httpConfigurationsByHost.values(), 0, new Functions.Binary<Integer, Integer, Collection<HttpConfiguration>>(){
            @Override
            public Integer call( final Integer count, final Collection<HttpConfiguration> httpConfigurations ) {
                return count + httpConfigurations.size();
            }
        });
    }    

    @ManagedAttribute(description="SSL Socket Factory Cache Size", currencyTimeLimit=5)
    public int getSSLSocketFactoryCacheSize() {
        return sslSocketFactoryMap.size();
    }

    @ManagedAttribute(description="Password Cache Size", currencyTimeLimit=5)
    public int getPasswordCacheSize() {
        final HttpConfig config = configurationRef.get();
        return config == null ? 0 : config.passwordMap.size();
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

    private final AtomicReference<SSLSocketFactory> defaultSSLSocketFactory = new AtomicReference<SSLSocketFactory>();
    private final AtomicReference<SSLSocketFactory> defaultKeySSLSocketFactory = new AtomicReference<SSLSocketFactory>();
    private final Map<SslSocketFactoryKey,SSLSocketFactory> sslSocketFactoryMap = new ConcurrentHashMap<SslSocketFactoryKey,SSLSocketFactory>();
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
                            if ( encryptedPassword!=null && encryptedPassword.length()>0 ) {
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
                            }
                            return password;
                        }
                    }
            ) );
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error loading HTTP configuration.", e );
        }
    }

    private SSLSocketFactory getSSLSocketFactory( final HttpConfiguration httpConfiguration ) throws NoSuchAlgorithmException, KeyManagementException {
        final SslSocketFactoryKey key = new SslSocketFactoryKey(
                httpConfiguration.getTlsKeyUse(),
                httpConfiguration.getTlsVersion(),
                httpConfiguration.getTlsKeystoreGoid(),
                httpConfiguration.getTlsKeystoreAlias(),
                httpConfiguration.getTlsCipherSuites() );
        SSLSocketFactory sslSocketFactory = sslSocketFactoryMap.get( key );

        if ( sslSocketFactory == null ) {
            try {
                switch ( httpConfiguration.getTlsKeyUse() ) {
                    case NONE:
                        sslSocketFactory = getDefaultSSLSocketFactory( false );
                        break;
                    case CUSTOM:
                        // We cache the non-versioned non-cipher-suited SSLSocketFactory since the versioned and/or suited
                        // ones can all wrap the same underlying SSLSocketFactory
                        final SslSocketFactoryKey keyNoVersion = new SslSocketFactoryKey(
                                        httpConfiguration.getTlsKeyUse(),
                                        null,
                                        httpConfiguration.getTlsKeystoreGoid(),
                                        httpConfiguration.getTlsKeystoreAlias(),
                                        null );
                        final Goid keystoreId = httpConfiguration.getTlsKeystoreGoid();
                        final String keyAlias = httpConfiguration.getTlsKeystoreAlias()==null ? "" : httpConfiguration.getTlsKeystoreAlias();
                        sslSocketFactory = sslSocketFactoryMap.get( keyNoVersion );
                        if ( sslSocketFactory == null ) {
                            SSLContext sslContext = SSLContext.getInstance( "TLS" );
                            final int timeout = ConfigFactory.getIntProperty( HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT );
                            sslContext.getClientSessionContext().setSessionTimeout( timeout );
                            final SsgKeyEntry keyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias( keyAlias, keystoreId );
                            final X509Certificate[] certChain = keyEntry.getCertificateChain();
                            final PrivateKey privateKey = keyEntry.getPrivateKey();
                            final KeyManager[] keyManagers = new KeyManager[]{ new SingleCertX509KeyManager( certChain, privateKey ) };
                            sslContext.init( keyManagers, new TrustManager[]{ trustManager }, null );
                            sslSocketFactory = sslContext.getSocketFactory();
                            sslSocketFactoryMap.put( keyNoVersion, sslSocketFactory );
                        }
                        break;
                }
            } catch ( KeyStoreException e ) {
                logger.log( Level.WARNING, "Error getting key for SSL.", e );
            } catch ( FindException e ) {
                logger.log( Level.WARNING, "Error getting key for SSL.", e );
            } catch ( UnrecoverableKeyException e ) {
                logger.log( Level.WARNING, "Error getting key for SSL.", e );
            }

            if ( sslSocketFactory == null ) {
                sslSocketFactory = getDefaultSSLSocketFactory( true );
            }

            if ( httpConfiguration.getTlsVersion() != null || httpConfiguration.getTlsCipherSuites() != null ) {
                final String tlsVersion = httpConfiguration.getTlsVersion();
                final String tlsCipherSuites = httpConfiguration.getTlsCipherSuites();
                String[] tlsVersionArray = tlsVersion == null ? null : new String[] { tlsVersion };
                String[] tlsCipherSuitesArray = tlsCipherSuites == null ? null : tlsCipherSuites.trim().split("\\s*,\\s*");
                sslSocketFactory = SSLSocketFactoryWrapper.wrapAndSetTlsVersionAndCipherSuites(sslSocketFactory, tlsVersionArray, tlsCipherSuitesArray);
                sslSocketFactoryMap.put( key, sslSocketFactory );
            } 
        }

        return sslSocketFactory;
    }

    private SSLSocketFactory getDefaultSSLSocketFactory( final boolean useSslKeyForDefault ) throws NoSuchAlgorithmException, KeyManagementException {
        final AtomicReference<SSLSocketFactory> ref = useSslKeyForDefault ? defaultKeySSLSocketFactory : defaultSSLSocketFactory;

        SSLSocketFactory sslSocketFactory = ref.get();
        if ( sslSocketFactory == null ) {
            sslSocketFactory = buildDefaultSSLSocketFactory( useSslKeyForDefault );
            ref.compareAndSet( null, sslSocketFactory );
        }

        return sslSocketFactory;
    }

    private SSLSocketFactory buildDefaultSSLSocketFactory( final boolean useSslKeyForDefault ) throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final int timeout = ConfigFactory.getIntProperty( HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT );
        sslContext.getClientSessionContext().setSessionTimeout(timeout);
        sslContext.init( useSslKeyForDefault ? defaultKey.getSslKeyManagers() : null, new TrustManager[]{trustManager}, null );
        return sslContext.getSocketFactory();
    }

    private static final class HttpConfig {
        private final HttpProxyConfiguration httpProxyConfiguration;
        private final Map<String,Collection<HttpConfiguration>> httpConfigurationsByHost;
        private final Map<Goid,char[]> passwordMap;

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
            this.passwordMap = Collections.unmodifiableMap( Functions.reduce( passwords, new HashMap<Goid,char[]>(), new Functions.Binary<Map<Goid,char[]>,Map<Goid,char[]>,SecurePassword>(){
                @Override
                public Map<Goid, char[]> call( final Map<Goid, char[]> map, final SecurePassword securePassword ) {
                    map.put( securePassword.getGoid(), passwordDecryptor.call( securePassword.getName(), securePassword.getEncodedPassword() ) );
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

        public synchronized char[] getPassword( final Goid passwordGoid ) {
            char[] password = passwordMap.get( passwordGoid );

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

                    if ( proxyConfiguration != null &&
                         proxyConfiguration.getUsername() != null &&
                         proxyConfiguration.getPasswordGoid() != null ) {
                        passwordAuthentication = new PasswordAuthentication(
                                proxyConfiguration.getUsername(),
                                httpConfig.getPassword(proxyConfiguration.getPasswordGoid()) );
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
                                httpConfig.getPassword(httpConfiguration.getPasswordGoid()) );
                    }
                }
            }

            return passwordAuthentication;
        }
    }

    private static final class SslSocketFactoryKey {
        private final HttpConfiguration.Option tlsUse;
        private final String tlsVersion;
        private final Goid keyStoreGoid;
        private final String keyStoreAlias;
        private final String tlsCipherSuites;

        private SslSocketFactoryKey( final HttpConfiguration.Option tlsUse,
                                     final String tlsVersion,
                                     final Goid keyStoreGoid,
                                     final String keyStoreAlias,
                                     final String tlsCipherSuites ) {
            if ( tlsUse==null ) throw new IllegalArgumentException( "use is required" );
            this.tlsUse = tlsUse;
            this.tlsVersion = tlsVersion;
            this.keyStoreGoid = keyStoreGoid;
            this.keyStoreAlias = keyStoreAlias;
            this.tlsCipherSuites = tlsCipherSuites;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final SslSocketFactoryKey that = (SslSocketFactoryKey) o;

            if ( !Goid.equals(keyStoreGoid, that.keyStoreGoid )) return false;
            if ( keyStoreAlias != null ? !keyStoreAlias.equals( that.keyStoreAlias ) : that.keyStoreAlias != null )
                return false;
            if ( tlsUse != that.tlsUse ) return false;
            if ( tlsVersion != null ? !tlsVersion.equals( that.tlsVersion ) : that.tlsVersion != null ) return false;
            if ( tlsCipherSuites != null ? !tlsCipherSuites.equals( that.tlsCipherSuites ) : that.tlsCipherSuites != null ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = tlsUse.hashCode();
            result = 31 * result + (tlsVersion != null ? tlsVersion.hashCode() : 0);
            result = 31 * result + (keyStoreGoid != null ? keyStoreGoid.hashCode() : 0);
            result = 31 * result + (keyStoreAlias != null ? keyStoreAlias.hashCode() : 0);
            result = 31 * result + (tlsCipherSuites != null ? tlsCipherSuites.hashCode() : 0);
            return result;
        }
    }
}
