/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.http;

/**
 * A generic HTTP client interface.
 */
public interface GenericHttpClient {
    String METHOD_GET = "GET";
    String METHOD_POST = "POST";
    String METHOD_PUT = "PUT";
    String METHOD_DELETE = "DELETE";
    public static GenericHttpMethod GET = new GenericHttpMethod(METHOD_GET, false);
    public static GenericHttpMethod POST = new GenericHttpMethod(METHOD_POST, true);
    public static GenericHttpMethod PUT = new GenericHttpMethod(METHOD_PUT, true);
    public static GenericHttpMethod DELETE = new GenericHttpMethod(METHOD_DELETE, false);

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
