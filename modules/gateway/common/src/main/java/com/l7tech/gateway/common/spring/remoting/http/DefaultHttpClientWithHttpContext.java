package com.l7tech.gateway.common.spring.remoting.http;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * Extension of HttpClient that provides access to a configured HttpContext
 */
public class DefaultHttpClientWithHttpContext extends DefaultHttpClient{

    public DefaultHttpClientWithHttpContext(ClientConnectionManager conman) {
        super(conman);
    }

    @Override
    protected HttpContext createHttpContext() {
        return super.createHttpContext();
    }
}
