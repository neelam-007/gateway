package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.ConfigFactory;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import javax.net.ssl.*;
import java.security.GeneralSecurityException;

/**
 * A GenericHttpClientFactory that runs in the SSG server context, and creates clients that will automatically
 * configure requests to use the appropriate SSL context factory for requests to SSL URLs.
 */
public class HttpClientFactory implements GenericHttpClientFactory {
    private final Object initLock = new Object();
    private SSLContext sslContext;

    private final DefaultKey keystore;
    private final X509TrustManager trustManager;
    private final HostnameVerifier hostnameVerifier;

    private static final String PROP_MAX_CONN_PER_HOST = HttpClientFactory.class.getName() + ".maxConnectionsPerHost";
    private static final String PROP_MAX_TOTAL_CONN = HttpClientFactory.class.getName() + ".maxTotalConnections";

    private static final int MAX_CONNECTIONS_PER_HOST = ConfigFactory.getIntProperty( PROP_MAX_CONN_PER_HOST, 100 );
    private static final int MAX_CONNECTIONS = ConfigFactory.getIntProperty( PROP_MAX_TOTAL_CONN, 1000 );

    private static final PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
    static {
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_HOST);
        connectionManager.setMaxTotal(MAX_CONNECTIONS);
    }

    public HttpClientFactory(final DefaultKey keystore,
                             final X509TrustManager trustManager,
                             final HostnameVerifier hostnameVerifier) {
        if (keystore==null) throw new IllegalArgumentException("keystore must not be null");
        if (trustManager==null) throw new IllegalArgumentException("trustManager must not be null");
        if (hostnameVerifier==null) throw new IllegalArgumentException("hostnameVerifier must not be null");

        this.keystore = keystore;
        this.trustManager = trustManager;
        this.hostnameVerifier = hostnameVerifier;
    }

    public GenericHttpClient createHttpClient() {
        return createHttpClient(-1, -1, -1, -1, null);
    }

    /**
     * @param hostConnections IGNORED this factory uses 100
     * @param totalConnections IGNORED this factory uses 1000
     * @param connectTimeout -1 for default
     * @param timeout -1 for default
     * @param identity IGNORED this factory does not support identity binding
     */
    public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
        return new HttpComponentsClient(connectionManager, connectTimeout, timeout) {
            public GenericHttpRequest createRequest(HttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                final String proto = params.getTargetUrl().getProtocol();
                if ("https".equalsIgnoreCase(proto)) {
                    try {
                        params.setSslSocketFactory(getSslContext().getSocketFactory());
                        params.setHostnameVerifier(hostnameVerifier);
                    } catch (GeneralSecurityException e) {
                        // TODO is it OK to continue with the default trust manager?
                        throw new GenericHttpException(e);
                    }
                }

                return super.createRequest(method, params);
            }
        };
    }

    /**
     * Get the process-wide shared SSL context, creating it if this thread is the first one
     * to need it.
     *
     * @return the current SSL context.  Never null.
     * @throws java.security.GeneralSecurityException  if an SSL context is needed but can't be created because the current server
     *                                   configuration is incomplete or invalid (keystores, truststores, and whatnot)
     */
    private SSLContext getSslContext() throws GeneralSecurityException {
        synchronized(initLock) {
            // no harm done if multiple threads try to create it the very first time.  s'all good.
            if (sslContext != null) return sslContext;
            SSLContext sc = SSLContext.getInstance("TLS");
            KeyManager[] keyman = keystore.getSslKeyManagers();
            sc.init(keyman, new TrustManager[]{trustManager}, null);
            final int timeout = ConfigFactory.getIntProperty( HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT );
            sc.getClientSessionContext().setSessionTimeout(timeout);
            return sslContext = sc;
        }
    }
}
