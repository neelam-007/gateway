/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

/**
 * A generic HTTP client interface.
 */
public interface GenericHttpClient {
    public static GenericHttpMethod GET = new GenericHttpMethod();
    public static GenericHttpMethod POST = new GenericHttpMethod();

    public static class GenericHttpMethod {
        private GenericHttpMethod() {}
    }

    /**
     * Create an HTTP request.
     *
     * @param method the request method to use.  May be one of {@link #GET} or {@link #POST}.
     * @param params the request params.  Must not be null.
     * @return the HTTP request object, ready to proceed.  Never null.
     * @throws GenericHttpException if there is a configuration, network, or HTTP problem.
     */
    GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException;
}
