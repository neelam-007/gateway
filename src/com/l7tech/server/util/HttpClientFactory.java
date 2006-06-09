/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.GeneralSecurityException;

/**
 * @author mike
 */
public class HttpClientFactory implements ApplicationContextAware {
    private SSLContext sslContext;
    private ApplicationContext spring;

    private static final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    static {
        connectionManager.setMaxConnectionsPerHost(100);
        connectionManager.setMaxTotalConnections(1000);
    }

    private ThreadLocal<GenericHttpClient> localHttpClient = new ThreadLocal<GenericHttpClient>() {
        protected GenericHttpClient initialValue() {
            return createHttpClient();
        }
    };

    public GenericHttpClient getThreadLocalHttpClient() {
        return localHttpClient.get();
    }

    public GenericHttpClient createHttpClient() {
        return new CommonsHttpClient(connectionManager) {
            public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                final String proto = params.getTargetUrl().getProtocol();
                if ("https".equalsIgnoreCase(proto)) {
                    try {
                        params.setSslSocketFactory(getSslContext().getSocketFactory());
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
    private synchronized SSLContext getSslContext() throws GeneralSecurityException {
        // no harm done if multiple threads try to create it the very first time.  s'all good.
        if (sslContext != null) return sslContext;
        SSLContext sc = SSLContext.getInstance("SSL");
        KeystoreUtils keystore = (KeystoreUtils)spring.getBean("keystore");
        SslClientTrustManager trustManager = (SslClientTrustManager)spring.getBean("httpRoutingAssertionTrustManager");
        KeyManager[] keyman = keystore.getSSLKeyManagerFactory().getKeyManagers();
        sc.init(keyman, new TrustManager[]{trustManager}, null);
        final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                               HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT);
        sc.getClientSessionContext().setSessionTimeout(timeout);
        return sslContext = sc;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.spring = applicationContext;
    }
}
