package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.PermissiveX509TrustManager;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
import com.l7tech.util.MockConfig;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.l7tech.gateway.common.resources.HttpConfiguration.Option.*;
import static com.l7tech.gateway.common.resources.HttpConfiguration.Protocol.HTTP;
import static org.junit.Assert.*;

/**
 * 
 */
public class HttpConfigurationCacheTest {

    @Test
    public void testProxySelector() throws Exception {
        final HttpProxyConfiguration defaultProxy = newHttpProxyConfiguration( "default-proxy-host", 8888, "default-proxy-user", 1 );
        final HttpProxyConfiguration customProxy = newHttpProxyConfiguration( "custom-proxy-host", 99, "custom-proxy-user", 2 );
        final HttpConfiguration[] httpConfigurations = new HttpConfiguration[3];
        httpConfigurations[0] = newHttpConfiguration( "host1", DEFAULT, null );
        httpConfigurations[1] = newHttpConfiguration( "host2", NONE, null );
        httpConfigurations[2] = newHttpConfiguration( "host3", CUSTOM, customProxy );

        final ProxySelector[] proxySelectorHolder = new ProxySelector[1];
        buildCache( defaultProxy, httpConfigurations, null, proxySelectorHolder );
        final ProxySelector proxySelector = proxySelectorHolder[0];

        {
            final List<Proxy> proxyList = proxySelector.select( URI.create( "http://otherhost/" ) );
            assertNotNull( "proxy list", proxyList );
            assertEquals( "Default proxy", proxy( defaultProxy ), proxyList  );
        }
        {
            final List<Proxy> proxyList = proxySelector.select( URI.create( "http://host1/" ) );
            assertNotNull( "proxy list", proxyList );
            assertEquals( "Default proxy", proxy( defaultProxy ), proxyList  );
        }
        {
            final List<Proxy> proxyList = proxySelector.select( URI.create( "http://host2/" ) );
            assertNotNull( "proxy list", proxyList );
            assertEquals( "Default proxy", Collections.singletonList(Proxy.NO_PROXY), proxyList  );
        }
        {
            final List<Proxy> proxyList = proxySelector.select( URI.create( "http://host3/" ) );
            assertNotNull( "proxy list", proxyList );
            assertEquals( "Default proxy", proxy( customProxy ), proxyList  );
        }
    }

    /**
     * NOTE: secure password manager stub lower cases passwords to indicate decryption.
     */
    @Test
    public void testProxyAuthenticator() throws Exception {
        final HttpProxyConfiguration defaultProxy = newHttpProxyConfiguration( "default-proxy-host", 8888, "default-proxy-user", 1 );
        final HttpProxyConfiguration customProxy = newHttpProxyConfiguration( "custom-proxy-host", 99, "custom-proxy-user", 2 );
        final HttpProxyConfiguration customProxy2 = newHttpProxyConfiguration( "custom-proxy-host2", 77, null, 0 );
        final HttpConfiguration[] httpConfigurations = new HttpConfiguration[4];
        httpConfigurations[0] = newHttpConfiguration( "host1", DEFAULT, null );
        httpConfigurations[1] = newHttpConfiguration( "host2", NONE, null );
        httpConfigurations[2] = newHttpConfiguration( "host3", CUSTOM, customProxy );
        httpConfigurations[3] = newHttpConfiguration( "host4", CUSTOM, customProxy2 );

        final Authenticator[] authenticatorHolder = new Authenticator[1];
        buildCache( defaultProxy, httpConfigurations, authenticatorHolder, null );
        final Authenticator authenticator = authenticatorHolder[0];

        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://otherhost/" ), true );
            assertNotNull( "proxy authentication", passwordAuthentication );
            assertEquals( "Default proxy username", "default-proxy-user", passwordAuthentication.getUserName()  );
            assertArrayEquals( "Default proxy password", "test-password1".toCharArray(), passwordAuthentication.getPassword()  );
        }
        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://host1/" ), true );
            assertNotNull( "proxy authentication", passwordAuthentication );
            assertEquals( "Default proxy username", "default-proxy-user", passwordAuthentication.getUserName()  );
            assertArrayEquals( "Default proxy password", "test-password1".toCharArray(), passwordAuthentication.getPassword()  );
        }
        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://host2/" ), true );
            assertNull( "host2 proxy authentication", passwordAuthentication );
        }
        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://host3/" ), true );
            assertNotNull( "proxy authentication", passwordAuthentication );
            assertEquals( "Custom proxy username", "custom-proxy-user", passwordAuthentication.getUserName()  );
            assertArrayEquals( "Custom proxy password", "test-password2".toCharArray(), passwordAuthentication.getPassword()  );
        }
        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://host4/" ), true );
            assertNull( "host4 proxy authentication", passwordAuthentication );
        }
    }

    @Test
    public void testAuthenticator() throws Exception {
        final HttpConfiguration[] httpConfigurations = new HttpConfiguration[3];
        httpConfigurations[0] = newHttpConfiguration( "host1", "username1", 1, null );
        httpConfigurations[1] = newHttpConfiguration( "host2", "username2", 2, "TEST-DOMAIN" );
        httpConfigurations[2] = newHttpConfiguration( "host3", null, 0, null );

        final Authenticator[] authenticatorHolder = new Authenticator[1];
        buildCache( null, httpConfigurations, authenticatorHolder, null );
        final Authenticator authenticator = authenticatorHolder[0];

        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://otherhost/" ), false );
            assertNull( "authentication no config", passwordAuthentication );
        }
        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://host1/" ), false );
            assertNotNull( "authentication host1", passwordAuthentication );
            assertEquals( "host1 username", "username1", passwordAuthentication.getUserName()  );
            assertArrayEquals( "host1 password", "test-password1".toCharArray(), passwordAuthentication.getPassword()  );
        }
        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://host2/" ), false );
            assertNotNull( "authentication host2", passwordAuthentication );
            assertEquals( "host2 username", "TEST-DOMAIN\\username2", passwordAuthentication.getUserName()  );
            assertArrayEquals( "host1 password", "test-password2".toCharArray(), passwordAuthentication.getPassword()  );
        }
        {
            final PasswordAuthentication passwordAuthentication = auth( authenticator, new URL( "http://host3/" ), false );
            assertNull( "authentication host3", passwordAuthentication );
        }
    }

    @Test
    public void testHttpConfigurationPreference() throws Exception {
        final HttpConfiguration[] httpConfigurations = new HttpConfiguration[5];
        httpConfigurations[0] = newHttpConfiguration( "host1", 0, null, null, "username1", 1 );
        httpConfigurations[1] = newHttpConfiguration( "host1", 80, null, null, "username2", 1  );
        httpConfigurations[2] = newHttpConfiguration( "host1", 0, HTTP, null, "username3", 1  );
        httpConfigurations[3] = newHttpConfiguration( "host1", 0, HTTP, "/folder1", "username4", 1 );
        httpConfigurations[4] = newHttpConfiguration( "host1", 0, HTTP, "/folder1/folder2", "username5", 1 );

        final HttpConfigurationCache cache = buildCache( null, httpConfigurations, null, null );

        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("https://host1") );
            cache.configure( parameters, true );
            assertNotNull( "HTTPS host1 credentials", parameters.getPasswordAuthentication() );
            assertEquals( "HTTPS host1 username", "username1", parameters.getPasswordAuthentication().getUserName() );
        }
        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("http://host1") );
            cache.configure( parameters, true );
            assertNotNull( "HTTP host1 credentials", parameters.getPasswordAuthentication() );
            assertEquals( "HTTP host1 username", "username2", parameters.getPasswordAuthentication().getUserName() );
        }
        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("http://host1:88") );
            cache.configure( parameters, true );
            assertNotNull( "HTTP host1:88 credentials", parameters.getPasswordAuthentication() );
            assertEquals( "HTTP host1:88 username", "username3", parameters.getPasswordAuthentication().getUserName() );
        }
        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("http://host1:88/folder1/") );
            cache.configure( parameters, true );
            assertNotNull( "HTTP host1 /folder1/ credentials", parameters.getPasswordAuthentication() );
            assertEquals( "HTTP host1 /folder1/ username", "username4", parameters.getPasswordAuthentication().getUserName() );
        }
        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("http://host1/folder1/") );
            cache.configure( parameters, true );
            assertNotNull( "HTTP host1:80 /folder1/ credentials", parameters.getPasswordAuthentication() );
            assertEquals( "HTTP host1:80 /folder1/ username", "username2", parameters.getPasswordAuthentication().getUserName() );
        }
        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("http://host1:88/folder1/folder2/more") );
            cache.configure( parameters, true );
            assertNotNull( "HTTP host1 /folder1/folder2/more credentials", parameters.getPasswordAuthentication() );
            assertEquals( "HTTP host1 /folder1/folder2/more username", "username5", parameters.getPasswordAuthentication().getUserName() );
        }
    }


    @Test
    public void testHttpConfiguration() throws Exception {
        final HttpProxyConfiguration defaultProxy = newHttpProxyConfiguration( "default-proxy-host", 8888, "default-proxy-user", 1 );
        final HttpProxyConfiguration customProxy = newHttpProxyConfiguration( "custom-proxy-host", 99, "custom-proxy-user", 2 );

        final HttpConfiguration[] httpConfigurations = new HttpConfiguration[3];
        httpConfigurations[0] = new HttpConfiguration();
        httpConfigurations[0].setGoid( nextGoid() );
        httpConfigurations[0].setHost( "host1" );
        httpConfigurations[0].setUsername( "username1" );
        httpConfigurations[0].setPasswordOid( 1L );
        httpConfigurations[0].setConnectTimeout( 1000 );
        httpConfigurations[0].setReadTimeout( 2000 );
        httpConfigurations[0].setFollowRedirects( true );
        httpConfigurations[0].setProxyUse( NONE );

        httpConfigurations[1] = new HttpConfiguration();
        httpConfigurations[1].setGoid( nextGoid() );
        httpConfigurations[1].setHost( "host2" );
        httpConfigurations[1].setUsername( "username2" );
        httpConfigurations[1].setPasswordOid( 2L );
        httpConfigurations[1].setNtlmDomain( "TEST" );
        httpConfigurations[1].setNtlmHost( "ntlmhost" );
        httpConfigurations[1].setConnectTimeout( -1 );
        httpConfigurations[1].setReadTimeout( -1 );
        httpConfigurations[1].setFollowRedirects( false );
        httpConfigurations[1].setProxyUse( DEFAULT );

        httpConfigurations[2] = new HttpConfiguration();
        httpConfigurations[2].setGoid( nextGoid() );
        httpConfigurations[2].setHost( "host3" );
        httpConfigurations[2].setProxyUse( CUSTOM );
        httpConfigurations[2].setProxyConfiguration( customProxy );

        final HttpConfigurationCache cache = buildCache( defaultProxy, httpConfigurations, null, null );

        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("https://host1") );
            cache.configure( parameters, true );
            assertNotNull( "host1 credentials", parameters.getPasswordAuthentication() );
            assertEquals( "host1 username", "username1", parameters.getPasswordAuthentication().getUserName() );
            assertArrayEquals( "host1 password", "test-password1".toCharArray(), parameters.getPasswordAuthentication().getPassword() );
            assertNull( "host1 ntlm credentials", parameters.getNtlmAuthentication() );
            assertEquals( "host1 connect timeout", 1000, parameters.getConnectionTimeout() );
            assertEquals( "host1 read timeout", 2000, parameters.getReadTimeout() );
            assertEquals( "host1 redirects", true, parameters.isFollowRedirects() );
            assertNull( "host1 proxy host", parameters.getProxyHost() );
        }
        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("https://host2") );
            cache.configure( parameters, true );
            assertNull( "host2 credentials", parameters.getPasswordAuthentication() );
            assertNotNull( "host2 ntlm credentials", parameters.getNtlmAuthentication() );
            assertEquals( "host2 username", "username2", parameters.getNtlmAuthentication().getUsername() );
            assertArrayEquals( "host2 password", "test-password2".toCharArray(), parameters.getNtlmAuthentication().getPassword() );
            assertEquals( "host2 ntlm domain", "TEST", parameters.getNtlmAuthentication().getDomain() );
            assertEquals( "host2 ntlm host", "ntlmhost", parameters.getNtlmAuthentication().getHost() );
            assertEquals( "host2 connect timeout", -1, parameters.getConnectionTimeout() );
            assertEquals( "host2 read timeout", -1, parameters.getReadTimeout() );
            assertEquals( "host2 redirects", false, parameters.isFollowRedirects() );
            assertEquals( "host2 proxy host", "default-proxy-host", parameters.getProxyHost() );
            assertEquals( "host2 proxy port", 8888, parameters.getProxyPort() );
            assertNotNull( "host2 proxy authentication", parameters.getProxyAuthentication() );
            assertEquals( "host2 proxy username", "default-proxy-user", parameters.getProxyAuthentication().getUserName() );
            assertEquals( "host2 proxy password", "test-password1", new String(parameters.getProxyAuthentication().getPassword()) );
        }
        {
            final GenericHttpRequestParams parameters = new GenericHttpRequestParams( new URL("https://host3") );
            cache.configure( parameters, true );
            assertNull( "host3 credentials", parameters.getPasswordAuthentication() );
            assertEquals( "host3 proxy host", "custom-proxy-host", parameters.getProxyHost() );
            assertEquals( "host3 proxy port", 99, parameters.getProxyPort() );
            assertNotNull( "host3 proxy authentication", parameters.getProxyAuthentication() );
            assertEquals( "host3 proxy username", "custom-proxy-user", parameters.getProxyAuthentication().getUserName() );
            assertEquals( "host3 proxy password", "test-password2", new String(parameters.getProxyAuthentication().getPassword()) );
        }
    }

    private PasswordAuthentication auth( final Authenticator authenticator,
                                         final URL url,
                                         final boolean proxy ) throws Exception {
        set(authenticator, "requestingHost", url.getHost());
        set(authenticator, "requestingSite", null);
        set(authenticator, "requestingPort", url.getPort());
        set(authenticator, "requestingProtocol", url.getProtocol());
        set(authenticator, "requestingPrompt", "");
        set(authenticator, "requestingScheme", "");
        set(authenticator, "requestingURL", url);
        set(authenticator, "requestingAuthType", proxy ? Authenticator.RequestorType.PROXY : Authenticator.RequestorType.SERVER);
        final Method method = Authenticator.class.getDeclaredMethod( "getPasswordAuthentication" );
        method.setAccessible( true );
        return (PasswordAuthentication) method.invoke( authenticator );
    }

    private void set( final Authenticator authenticator,
                      final String fieldName,
                      final Object value ) throws Exception {
        final Field field = Authenticator.class.getDeclaredField( fieldName );
        field.setAccessible( true );
        field.set( authenticator, value );
    }

    private List<Proxy> proxy( final HttpProxyConfiguration proxyConfiguration ) {
        return Collections.singletonList(
                new Proxy( Proxy.Type.HTTP,
                           new InetSocketAddress( proxyConfiguration.getHost(), proxyConfiguration.getPort() ) )
        );
    }

    private static long httpConfigurationCountLow = 0;
    private static Goid nextGoid() {
        return new Goid(0, httpConfigurationCountLow++);
    }


    private HttpConfiguration newHttpConfiguration( final String host,
                                                    final HttpConfiguration.Option proxyUse,
                                                    final HttpProxyConfiguration proxyConfig ) {
        final HttpConfiguration config = new HttpConfiguration();
        config.setGoid( nextGoid() );
        config.setHost( host );
        config.setProxyUse( proxyUse );
        if (proxyConfig != null) config.setProxyConfiguration( proxyConfig );
        return new HttpConfiguration( config, true );
    }

    private HttpConfiguration newHttpConfiguration( final String host,
                                                    final String username,
                                                    final long passwordOid,
                                                    final String ntlmDomain ) {
        final HttpConfiguration config = new HttpConfiguration();
        config.setGoid( nextGoid() );
        config.setHost( host );
        config.setUsername( username );
        config.setPasswordOid( passwordOid );
        config.setNtlmDomain( ntlmDomain );
        return new HttpConfiguration( config, true );
    }

    private HttpConfiguration newHttpConfiguration( final String host,
                                                    final int port,
                                                    final HttpConfiguration.Protocol protocol,
                                                    final String path,
                                                    final String username,
                                                    final long passwordOid ) {
        final HttpConfiguration config = new HttpConfiguration();
        config.setGoid( nextGoid() );
        config.setHost( host );
        config.setPort( port );
        config.setProtocol( protocol );
        config.setPath( path );
        config.setUsername( username );
        config.setPasswordOid( passwordOid );
        return new HttpConfiguration( config, true );
    }

    private HttpProxyConfiguration newHttpProxyConfiguration( final String host,
                                                              final int port,
                                                              final String username,
                                                              final long passwordOid ) {
        final HttpProxyConfiguration proxy = new HttpProxyConfiguration();
        proxy.setHost( host );
        proxy.setPort( port );
        proxy.setUsername( username );
        proxy.setPasswordOid( passwordOid );
        return new HttpProxyConfiguration( proxy, true );
    }

    private HttpConfigurationCache buildCache( final HttpProxyConfiguration httpProxyConfiguration,
                                               final HttpConfiguration[] httpConfigurations,
                                               final Authenticator[] authenticatorHolder,
                                               final ProxySelector[] proxyHolder ) throws Exception {
        final DefaultHttpProxyManager defaultHttpProxyManager =
                new DefaultHttpProxyManager( new MockClusterPropertyManager() );        
        if ( httpProxyConfiguration != null ) {
            defaultHttpProxyManager.setDefaultHttpProxyConfiguration( httpProxyConfiguration );
        }

        final HttpConfigurationCache cache = new HttpConfigurationCache(
            new MockConfig( new Properties() ),
            defaultHttpProxyManager,
            new HttpConfigurationManagerStub(httpConfigurations==null ? new HttpConfiguration[0] : httpConfigurations),
            new SecurePasswordManagerStub(
                    new SecurePassword("test1"){{ setOid(1); setEncodedPassword("TEST-PASSWORD1"); } },
                    new SecurePassword("test2"){{ setOid(2); setEncodedPassword("TEST-PASSWORD2"); } }
            ),
            new PermissiveHostnameVerifier(),
            new SsgKeyStoreManagerStub(),
            new TestDefaultKey(),
            new PermissiveX509TrustManager()
        ){
            @Override
            protected void installAuthenticator( final Authenticator authenticator ) {
                if ( authenticatorHolder != null ) authenticatorHolder[0] = authenticator;
            }

            @Override
            protected void installProxySelector( final ProxySelector proxySelector ) {
                if ( proxyHolder != null ) proxyHolder[0] = proxySelector;
            }
        };
        cache.afterPropertiesSet();
        return cache;
    }
}
