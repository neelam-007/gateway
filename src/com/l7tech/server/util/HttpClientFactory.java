/*
 * Copyright (C) 2005-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.KeystoreUtils;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.net.ssl.*;
import java.security.GeneralSecurityException;

/**
 * A GenericHttpClientFactory that runs in the SSG server context, and creates clients that will automatically
 * configure requests to use the appropriate SSL context factory for requests to SSL URLs.
 */
public class HttpClientFactory implements GenericHttpClientFactory, ApplicationContextAware {
    private final Object initLock = new Object();
    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;
    private ApplicationContext spring;

    private static final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    static {
        HttpConnectionManagerParams params = connectionManager.getParams();
        params.setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, 100);
        params.setMaxTotalConnections(1000);
    }

    private ThreadLocal<GenericHttpClient> localHttpClient = new ThreadLocal<GenericHttpClient>() {
        protected GenericHttpClient initialValue() {
            return createHttpClient();
        }
    };

    public HttpClientFactory() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.spring = applicationContext;
    }

    public GenericHttpClient getThreadLocalHttpClient() {
        return localHttpClient.get();
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
        return new CommonsHttpClient(connectionManager, connectTimeout, timeout) {
            public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                final String proto = params.getTargetUrl().getProtocol();
                if ("https".equalsIgnoreCase(proto)) {
                    try {
                        params.setSslSocketFactory(getSslContext().getSocketFactory());
                        params.setHostnameVerifier(getHostnameVerifier());
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
        ApplicationContext applicationContext = spring;
        synchronized(initLock) {
            // no harm done if multiple threads try to create it the very first time.  s'all good.
            if (sslContext != null) return sslContext;
            SSLContext sc = SSLContext.getInstance("SSL");
            KeystoreUtils keystore = (KeystoreUtils)applicationContext.getBean("keystore");
            X509TrustManager trustManager = (X509TrustManager)applicationContext.getBean("trustManager");
            KeyManager[] keyman = keystore.getSSLKeyManagerFactory().getKeyManagers();
            sc.init(keyman, new TrustManager[]{trustManager}, null);
            final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                   HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT);
            sc.getClientSessionContext().setSessionTimeout(timeout);
            return sslContext = sc;
        }
    }

    /**
     * Get the process-wide shared hostname verifier, creating it if necessary.
     *
     * @return The HostnameVerifier. Never null.
     */
    private HostnameVerifier getHostnameVerifier() {
        ApplicationContext applicationContext = spring;
        synchronized(initLock) {
            if (hostnameVerifier != null) return hostnameVerifier;
            HostnameVerifier verifier = (HostnameVerifier)applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
            return hostnameVerifier = verifier;
        }
    }
}
