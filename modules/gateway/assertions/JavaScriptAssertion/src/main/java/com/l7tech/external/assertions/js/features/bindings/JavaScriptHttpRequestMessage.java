package com.l7tech.external.assertions.js.features.bindings;

/**
 * Interface for all HTTP Request message specific properties
 */
@SuppressWarnings("unused")
public interface JavaScriptHttpRequestMessage extends JavaScriptMessage {

    /**
     * Returns the HTTP version. For eg. 1.1 or 1.0
     * @return HTTP version
     */
    String getHttpVersion();

    /**
     * Returns the HTTP method
     * @return HTTP method
     */
    String getMethod();

    /**
     * Returns the full URL called including the query string
     * @return url
     */
    String getUrl();

    /**
     * Gets the parameters as a JavaScript JSON object
     * @return parameters
     */
    Object getParameters();
}
