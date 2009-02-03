/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.util.Pair;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Generic implementation of an immutable HTTP header.
 */
public final class GenericHttpHeader extends Pair<String, String> implements HttpHeader {
    private static final ThreadLocal httpHeaderDateFormat = new ThreadLocal() {
        protected Object initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sdf;
        }
    };

    /**
     * Create a GenericHttpHeader from a Map.Entry.
     *
     * @param entry a Map.Entry containing two Strings.  Required.
     */
    public GenericHttpHeader(Map.Entry<String, String> entry) {
        super(entry.getKey(), entry.getValue());
    }

    /**
     * Create a GenericHttpHeader with the specified name and value.
     *
     * @param name  the header name.  Must not be null or empty.
     * @param value  the header value.  Must not be null.
     */
    public GenericHttpHeader(String name, String value) {
        super(name, value);
        if (name == null || name.length() < 1) throw new IllegalArgumentException("Header name must be non-empty");
        if (value == null) throw new NullPointerException("Header value must not be null");
    }

    public String getName() {
        return left;
    }

    public String getFullValue() {
        return right;
    }

    public static HttpHeader makeDateHeader(String name, Date date) {
        return new GenericHttpHeader(name, ((SimpleDateFormat)httpHeaderDateFormat.get()).format(date));
    }

    public String toString() {
        return super.toString()+"["+ left + "=" + right + "]";
    }
}
