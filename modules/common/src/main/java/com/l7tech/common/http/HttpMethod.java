/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

import org.apache.commons.lang.StringUtils;

import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(HttpMethod.class.getName());

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

    /**
     * This method return the HttpMethod object mapped to the string.
     *
     * @param methodAsString string representation of the HttpMethod
     * @return HttpMethod object of the string or NULL if the string cannot be mapped to any standard HTTP Method
     */
    public static HttpMethod valueOfHttpMethod(final String methodAsString) {
        if(StringUtils.isNotBlank(methodAsString)) {
            try {
                return HttpMethod.valueOf(methodAsString);
            } catch (IllegalArgumentException e) {
                // Ignore. It should return NULL in this case.
                logger.fine("Could not map '" + methodAsString + "' to any standard HTTP method.");
            }
        }
        return null;
    }
}
