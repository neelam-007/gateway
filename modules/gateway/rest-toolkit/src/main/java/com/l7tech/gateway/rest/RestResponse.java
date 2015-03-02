package com.l7tech.gateway.rest;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;

/**
 * This is used to encapsulate the rest response.
 */
public class RestResponse {
    /**
     * The response input stream.
     */
    private InputStream inputStream;
    /**
     * The response content type
     */
    private String contentType;
    /**
     * The response http status
     */
    private int status;
    /**
     * The response headers
     */
    private MultivaluedMap<String, Object> headers;

    /**
     * Creates a new response with the given properties.
     *
     * @param inputStream The response input stream
     * @param contentType The response content type
     * @param status      The response http status
     * @param headers     The response headers.
     */
    public RestResponse(InputStream inputStream, String contentType, int status, MultivaluedMap<String, Object> headers) {
        this.inputStream = inputStream;
        this.contentType = contentType;
        this.status = status;
        this.headers = headers;
    }

    /**
     * Returns the response input stream
     *
     * @return The response input stream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Returns the response content type
     *
     * @return The response content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the response http Status
     *
     * @return The response http status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the response headers
     *
     * @return The response headers
     */
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }
}