/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

/** @author alex */
public enum HttpMethod {
    GET(false, true),
    POST(true, false),
    PUT(true, false),
    DELETE(false, true),
    HEAD(false, true),
    OPTIONS(false, false), // According to RFC 2616 an OPTIONS request may include a body, but the commons OptionsMethod does not extend EntityEnclosingMethod so we'll ingore that possibility for now.
    /**
     * Other methods are possible, but enums aren't extensible.
     */
    OTHER(false, false);

    private final boolean needsRequestBody;
    private final boolean followRedirects;

    private HttpMethod(boolean needsRequestBody, boolean followRedirects) {
        this.needsRequestBody = needsRequestBody;
        this.followRedirects = followRedirects;
    }

    public boolean needsRequestBody() {
        return needsRequestBody;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }
}
