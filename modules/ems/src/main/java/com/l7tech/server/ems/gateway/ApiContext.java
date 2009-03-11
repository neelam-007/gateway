package com.l7tech.server.ems.gateway;

import com.l7tech.server.DefaultKey;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds common code used by ESM clients of APIs offered by the Gateway or ProcessController.
 */
public abstract class ApiContext {
    private static final String PROP_CONN_TIMEOUT = "com.l7tech.server.ems.gateway.connectTimeout";
    private static final String PROP_READ_TIMEOUT = "com.l7tech.server.ems.gateway.readTimeout";

    protected final Logger logger;
    protected final String cookie;
    protected final DefaultKey defaultKey;
    protected final String host;

    protected long connectionTimeout;
    protected long readTimeout;

    /**
     * Create an API context that will use the specified settings for authenticating the ESM to the API
     * provider, and for user mapping.
     *
     * @param defaultKey the ESM's SSL private key.  Required so the ESM can authenticate to the API provider.
     * @param host The hostname or IP address of the host that will be providing the API(s).  Required.
     * @param esmId The ID for the EM.  Required so the ESM can authenticate to the API provider.
     * @param userId The ID for the EM user, so the provider can map our users to its users for access control purposes,
     *               or null if this ApiContext will not be consuming the API on behalf of an EM user.
     */
    protected ApiContext(final Logger logger, final DefaultKey defaultKey, final String host, final String esmId, final String userId) {
        this.logger = logger;
        cookie = buildCookie( esmId, userId );
        this.defaultKey = defaultKey;
        this.host = host;
        this.connectionTimeout = SyspropUtil.getLong(PROP_CONN_TIMEOUT, 30000);
        this.readTimeout = SyspropUtil.getLong(PROP_READ_TIMEOUT, 60000);
    }

    public static boolean isNetworkException( final Exception exception ) {
        return ExceptionUtils.causedBy( exception, ConnectException.class ) ||
               ExceptionUtils.causedBy( exception, NoRouteToHostException.class ) ||
               ExceptionUtils.causedBy( exception, UnknownHostException.class ) ||
               ExceptionUtils.causedBy( exception, SocketTimeoutException.class );
    }

    public static boolean isConfigurationException( final Exception exception ) {
        boolean isConfigurationException = false;

        if ( "Access Denied".equals(exception.getMessage()) ) {
            isConfigurationException = true;
        } else if ( "Authentication Required".equals(exception.getMessage()) ){
            isConfigurationException = true;
        } else if ( "Not Licensed".equals(exception.getMessage()) ) {
            isConfigurationException = true;
        }

        return isConfigurationException;
    }

    /**
     * Get the connection timeout (milliseconds)
     *
     * @return The connection timeout.
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set the connection timeout (milliseconds).
     *
     * <p>The timeout will be used on subsequent API access, the change will
     * not update any existing APIs.</p>
     *
     * @param connectionTimeout The timeout to use
     */
    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Get the read timeout (milliseconds)
     *
     * @return The read timeout.
     */
    public long getReadTimeout() {
        return readTimeout;
    }

    /**
     * Set the read timeout (milliseconds).
     *
     * <p>The timeout will be used on subsequent API access, the change will
     * not update any existing APIs.</p>
     *
     * @param readTimeout The timeout to use
     */
    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    protected String buildCookie( final String esmId, final String userId ) {
        String cookie =  "EM-UUID=" + esmId;
        if ( userId != null ) {
            cookie += "; EM-USER-UUID=" + userId;
        }
        return cookie;
    }

    /**
     * Get an API, returning a cached API if one exists; otherwise, creating a new one using the specified
     * URL, private key, and interface class.
     *
     * @param apiHolder  an AtomicReferene to be used as a cache for the API.  If this already references an API instance,
     *                   this method will return immediately without taking further action.  Required.
     * @param apiClass   The remote interface to consume.  Required.
     * @param url        The URL that will provide the remote interface.  Required.
     * @return A proxy object hooked up to the remote interface.  Never null.
     */
    @SuppressWarnings({"unchecked"})
    protected <T> T getApi( final AtomicReference<T> apiHolder, final Class<T> apiClass, final String url ) {
        T ret = apiHolder.get();
        if (ret != null)
            return ret;

        synchronized (this) {
            ret = apiHolder.get();
            if (ret != null)
                return ret;

            ret = initApi(apiClass, url);
            apiHolder.set(ret);
            return ret;
        }
    }

    private <T> T initApi(Class<T> apiClass, String url) {
        JaxWsClientFactoryBean cfb = new JaxWsClientFactoryBean();
        cfb.setServiceClass( apiClass );
        cfb.setAddress( url );

        Client c = cfb.create();
        if ( logger.isLoggable( Level.FINEST ) ) {
            c.getInInterceptors().add( new LoggingInInterceptor() );
            c.getOutInterceptors().add( new LoggingOutInterceptor() );
            c.getInFaultInterceptors().add( new LoggingInInterceptor() );
            c.getOutFaultInterceptors().add( new LoggingOutInterceptor() );
        }

        HTTPConduit hc = (HTTPConduit) c.getConduit();
        HTTPClientPolicy policy = hc.getClient();
        policy.setCookie( cookie );
        policy.setConnectionTimeout( connectionTimeout );
        policy.setReceiveTimeout( readTimeout );
        hc.setTlsClientParameters(new TLSClientParameters() {
            @Override
            public TrustManager[] getTrustManagers() {
                return new TrustManager[] { new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }};
            }

            @Override
            public KeyManager[] getKeyManagers() {
                return defaultKey==null ? new KeyManager[0] : defaultKey.getSslKeyManagers();
            }

            @Override
            public boolean isDisableCNCheck() {
                return true;
            }
        });

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean(cfb);
        //noinspection unchecked
        return (T) factory.create();
    }
}
