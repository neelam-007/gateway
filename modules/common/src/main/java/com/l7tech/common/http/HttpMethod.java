/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

/** @author alex */
public enum HttpMethod {
    GET(false),
    POST(true),
    PUT(true),
    DELETE(false),

    /**
     * Other methods are possible, but enums aren't extensible.
     * @see HttpServletRequestKnob#getMethod
     * */
    OTHER(false);

    private final boolean needsRequestBody;

    private HttpMethod(boolean needsRequestBody) {
        this.needsRequestBody = needsRequestBody;
    }

    public boolean needsRequestBody() {
        return needsRequestBody;
    }
}
