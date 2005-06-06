/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Represents a cookie.
 */
public class HttpCookie {
    public static final Pattern WHITESPACE = Pattern.compile(";\\s*");
    public static final Pattern EQUALS = Pattern.compile("=");

    //store the full initial value of the cookie so that it can be regenerated later with ease
    final String fullValue;
    final String cookieValue;
    final String cookieName;
    String expires = null;
    String path = null;
    String domain = null;
    boolean secure = false;

    /**
     * Create an HttpCookie out of the specified raw header value value.
     *
     * @param headerFullValue the value of a Set-Cookie header, ie:
     *    "PREF=ID=e51:TM=686:LM=86:S=BL-w0; domain=.google.com; path=/; expires=Sun, 17-Jan-2038 19:14:07 GMT; secure".
     */
    public HttpCookie(String headerFullValue) throws IOException {
        // Parse cookie
        if (headerFullValue == null || "".equals(headerFullValue)) {
            throw new IllegalArgumentException("Cookie value is empty");
        }
        fullValue = headerFullValue;

        //fields will contain the following
        //      name=value
        //      domain
        //      path
        //      expires
        //      secure
        String[] fields = WHITESPACE.split(fullValue);
        if (fields == null || fields.length ==0) {
            throw new IllegalArgumentException("Cookie value is an invalid format: '" + headerFullValue + "'");
        }

        //need to split the name=value pair in fields[0]
        String[] nameValue = EQUALS.split(fields[0]);

        cookieName = nameValue[0];
        cookieValue = nameValue[1];


        // now parse each field from the rest of the cookie, if present
        for (int j=1; j<fields.length; j++) {

            if ("secure".equalsIgnoreCase(fields[j])) {
                secure = true;
            } else if (fields[j].indexOf('=') > 0) {
                String[] f = EQUALS.split(fields[j], 2);
                if ("expires".equalsIgnoreCase(f[0])) {
                    expires = f[1];
                } else if ("domain".equalsIgnoreCase(f[0])) {
                    domain = f[1];
                } else if ("path".equalsIgnoreCase(f[0])) {
                    path = f[1];
                }
            }
        }
    }

    public String getCookieName() {
        return cookieName;
    }

    /** @return the value of this cookie, ie "PREF=ID=e51:TM=686:LM=86:S=BL-w0" */
    public String getCookieValue() {
        return cookieValue;
    }

    /** @return true iff. this cookie should be sent only if transport-layer encryption is being used. */
    public boolean isSecure() {
        return secure;
    }

    /** @return the underlying cookie data as a Cookie Spec conformant string in the format:
     *      name=value; domain=thedomain.com; path=/; expires=Sun, 17-Jan-2038 19:14:07 GMT;secure
     *  this is identical to the string that was used to construct this object.
     */
    public String toExternalForm() {
        return fullValue;
    }
}
