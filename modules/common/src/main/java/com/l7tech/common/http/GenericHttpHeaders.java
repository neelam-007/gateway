/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple read-only holder of zero or more ordered {@link HttpHeader}s.
 */
public class GenericHttpHeaders implements HttpHeaders {
    private final HttpHeader[] headers;

    public GenericHttpHeaders(HttpHeader[] headers) {
        this.headers = headers;
    }

    public String getFirstValue(String name) {
        if (name == null) throw new NullPointerException();
        for (HttpHeader header : headers) {
            if (header != null && header.getName() != null && header.getName().equalsIgnoreCase(name))
                return header.getFullValue();
        }
        return null;
    }

    public String getOnlyOneValue(String name) throws GenericHttpException {
        if (name == null) throw new NullPointerException();
        String value = null;
        for (HttpHeader header : headers) {
            if (header != null && header.getName() != null && header.getName().equalsIgnoreCase(name)) {
                if (value != null)
                    throw new GenericHttpException("Multiple values found for the HTTP header: " + name);
                value = header.getFullValue();
            }
        }
        return value;
    }

    public List<String> getValues(String name) {
        if (name == null) throw new NullPointerException();
        List<String> ret = new ArrayList<String>();
        for (HttpHeader header : headers) {
            if (header != null && header.getName() != null && header.getName().equalsIgnoreCase(name))
                ret.add(header.getFullValue());
        }
        return ret;
    }

    public HttpHeader[] toArray() {
        return headers;
    }

    public String toExternalForm() {
        StringBuffer sb = new StringBuffer();
        for (HttpHeader header : headers) {
            sb.append(header.getName()).append(": ").append(header.getFullValue()).append("\n");
        }
        return sb.toString();
    }

    public String toString() {
        return toExternalForm();
    }
}
