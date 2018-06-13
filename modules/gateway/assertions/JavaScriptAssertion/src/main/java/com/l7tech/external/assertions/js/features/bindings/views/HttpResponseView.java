package com.l7tech.external.assertions.js.features.bindings.views;

import com.l7tech.external.assertions.js.features.bindings.JavaScriptHttpResponseMessage;

/**
 * Provides the HTTP Response view of the HTTP message. Can be accessed using context.getVariable('response:http');
 */
@SuppressWarnings("unused")
public class HttpResponseView extends HttpView {

    private int statusCode;

    public HttpResponseView(final JavaScriptHttpResponseMessage javaScriptMessage) {
        super(javaScriptMessage);
        this.statusCode = javaScriptMessage.getStatusCode();
    }

    /**
     * Gets the HTTP status code of the HTTP Response.
     * @return status
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the content and status code to the HTTP Response
     * @param content
     * @param contentType
     * @param statusCode
     */
    public void setContent(Object content, String contentType, int statusCode) {
        ((JavaScriptHttpResponseMessage) javaScriptMessage).setContent(content, contentType, statusCode);
    }
}
