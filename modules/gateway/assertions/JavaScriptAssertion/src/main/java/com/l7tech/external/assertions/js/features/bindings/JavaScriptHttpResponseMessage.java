package com.l7tech.external.assertions.js.features.bindings;

/**
 * Interface for all HTTP Response message specific properties
 */
@SuppressWarnings("unused")
public interface JavaScriptHttpResponseMessage extends JavaScriptMessage {

    /**
     * Gets the status code in case the message is HTTP Response
     * @return status code of HTTP response
     */
    int getStatusCode();

    /**
     * Sets the content and status code to the HTTP Response
     * @param content
     * @param contentType
     * @param statusCode
     */
    void setContent(Object content, String contentType, int statusCode);
}
