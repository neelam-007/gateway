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

    GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParamsImpl params) throws GenericHttpException;
}
