/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import javax.servlet.http.HttpServletResponse;

/**
 * Implementation of {@link HttpResponseKnob} that knows how to place the HTTP response transport metadata
 * into a servlet response.
 */
public class HttpServletResponseKnob implements HttpResponseKnob {
    private final HttpServletResponse response;

    public HttpServletResponseKnob(HttpServletResponse response) {
        if (response == null) throw new NullPointerException();
        this.response = response;
    }

    public void addCookie(org.apache.commons.httpclient.Cookie cookie) {
        javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(cookie.getName(), cookie.getValue());
        c.setDomain(cookie.getDomain());
        c.setPath(cookie.getPath());
        c.setVersion(cookie.getVersion());
        c.setComment(cookie.getComment());
        if (cookie.getExpiryDate() != null)
            c.setMaxAge((int)((cookie.getExpiryDate().getTime() - System.currentTimeMillis()) / 1000));
        response.addCookie(c);
    }

    public void setDateHeader(String name, long date) {
        response.setDateHeader(name, date);
    }

    public void addDateHeader(String name, long date) {
        response.addDateHeader(name, date);
    }

    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    public void setStatus(int code) {
        response.setStatus(code);
    }
}
