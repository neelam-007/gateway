/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

/** @author alex */
public enum HttpMethod {
    GET(false, true),
    POST(true, false),
    PUT(true, false),
    PATCH(true, false),
    DELETE(false, true),
    HEAD(false, true),
    OPTIONS(false, false), // According to RFC 2616 an OPTIONS request may include a body
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

    public String getProtocolName() {
        // OTHER is currently the only enum value whose enum name isn't the same as its valid protocol name
        return OTHER == this ? "POST" : name();
    }

    public boolean canForceIncludeRequestBody() {
        // TODO The "TRACE" method is the only one explicitly forbidden by the spec from including a body with the request
        return true; // except for when TRACE method is eventually supported
    }
}
