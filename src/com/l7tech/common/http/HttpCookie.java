/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import java.util.regex.Pattern;

/**
 * Represents a cookie.
 */
public class HttpCookie {
    public static final Pattern WHITESPACE = Pattern.compile(";\\s*");
    public static final Pattern EQUALS = Pattern.compile("=");

    final String cookieValue;
    //String expires = null;
    //String path = null;
    //String domain = null;
    boolean secure = false;

    /**
     * Create an HttpCookie out of the specified raw header value value.
     *
     * @param headerFullValue the value of a Set-Cookie header, ie:
     *    "PREF=ID=e51:TM=686:LM=86:S=BL-w0; domain=.google.com; path=/; expires=Sun, 17-Jan-2038 19:14:07 GMT; secure".
     */
    public HttpCookie(String headerFullValue) {
        // Parse cookie
        String[] fields = WHITESPACE.split(headerFullValue);

        cookieValue = fields[0];

        // Parse each field
        for (int j=1; j<fields.length; j++) {
            if ("secure".equalsIgnoreCase(fields[j])) {
                secure = true;
            } else if (fields[j].indexOf('=') > 0) {
//                String[] f = EQUALS.split(fields[j], 2);
//                if ("expires".equalsIgnoreCase(f[0])) {
//                    expires = f[1];
//                } else if ("domain".equalsIgnoreCase(f[0])) {
//                    domain = f[1];
//                } else if ("path".equalsIgnoreCase(f[0])) {
//                    path = f[1];
//                }
            }
        }
    }

    public HttpCookie(String cookieValue, boolean secure) {
        this.cookieValue = cookieValue;
        this.secure = secure;
    }

    /** @return the value of this cookie, ie "PREF=ID=e51:TM=686:LM=86:S=BL-w0" */
    public String getCookieValue() {
        return cookieValue;
    }

    /** @return true iff. this cookie should be sent only if transport-layer encryption is being used. */
    public boolean isSecure() {
        return secure;
    }

    public String toExternalForm() {
        return getCookieValue();
    }
}
