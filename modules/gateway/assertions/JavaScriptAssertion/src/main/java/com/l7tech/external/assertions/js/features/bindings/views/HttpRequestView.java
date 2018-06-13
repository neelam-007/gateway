package com.l7tech.external.assertions.js.features.bindings.views;

import com.l7tech.external.assertions.js.features.bindings.JavaScriptHttpRequestMessage;

/**
 * Provides the HTTP Request view of the HTTP message. Can be accessed using context.getVariable('request:http');
 */
@SuppressWarnings("unused")
public class HttpRequestView extends HttpView {

    private final String httpVersion;
    private final String method;
    private final String url;
    private final Object parameters;

    public HttpRequestView(final JavaScriptHttpRequestMessage javaScriptMessage) {
        super(javaScriptMessage);
        this.httpVersion = javaScriptMessage.getHttpVersion();
        this.method = javaScriptMessage.getMethod();
        this.url = javaScriptMessage.getUrl();
        this.parameters = javaScriptMessage.getParameters();
    }

    /**
     * Gets the HTTP version of the HTTP Request.
     * @return http version
     */
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * Gets the HTTP method of the HTTP Request.
     * @return method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the full URL of the HTTP Request including the query string.
     * @return http version
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the parameters of the HTTP Request as the JavaScript JSON object
     * @return
     */
    public Object getParameters() {
        return parameters;
    }
}
