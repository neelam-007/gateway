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
    public static GenericHttpMethod GET = new GenericHttpMethod("GET", false);
    public static GenericHttpMethod POST = new GenericHttpMethod("POST", true);

    public static class GenericHttpMethod {
        private final String name;
        private final boolean needsRequestBody;

        private GenericHttpMethod(String name, boolean needsRequestBody) {
            this.name = name;
            this.needsRequestBody = needsRequestBody;
        }

        public boolean needsRequestBody() {
            return needsRequestBody;
        }

        public String toString() {
            return name;
        }
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
