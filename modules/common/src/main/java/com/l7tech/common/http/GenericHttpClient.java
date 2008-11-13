/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

/**
 * A generic HTTP client interface.
 */
public interface GenericHttpClient {
    /**
     * Create an HTTP request.
     *
     * @param method the request method to use.  May be one of {@link HttpMethod#GET} or {@link HttpMethod#POST}.
     * @param params the request params.  Must not be null.
     * @return the HTTP request object, ready to proceed.  Never null.
     * @throws GenericHttpException if there is a configuration, network, or HTTP problem.
     */
    GenericHttpRequest createRequest(HttpMethod method, GenericHttpRequestParams params) throws GenericHttpException;
}
