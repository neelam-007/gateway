/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * Implementation of {@link HttpRequestKnob} that knows how to obtain the HTTP request transport metadata
 * from a servlet request.
 */
public class HttpServletRequestKnob implements HttpRequestKnob {
    private final HttpServletRequest request;

    public HttpServletRequestKnob(HttpServletRequest request) {
        if (request == null) throw new NullPointerException();
        this.request = request;
    }

    public static org.apache.commons.httpclient.Cookie servletCookieToCommons(javax.servlet.http.Cookie cookie) {
        org.apache.commons.httpclient.Cookie c = new org.apache.commons.httpclient.Cookie();
        c.setName(cookie.getName());
        c.setValue(cookie.getValue());
        c.setPath(cookie.getPath());
        c.setDomain(cookie.getDomain());
        c.setVersion(cookie.getVersion());
        c.setComment(cookie.getComment());
        if (cookie.getMaxAge() >= 0)
            c.setExpiryDate(new Date(System.currentTimeMillis() + (cookie.getMaxAge() * 1000L)));
        return c;
    }

    public org.apache.commons.httpclient.Cookie[] getCookies() {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        List out = new ArrayList(cookies.length);
        for (int i = 0; i < cookies.length; i++) {
            javax.servlet.http.Cookie cookie = cookies[i];
            out.add(servletCookieToCommons(cookie));
        }
        return (org.apache.commons.httpclient.Cookie[])out.toArray(new org.apache.commons.httpclient.Cookie[0]);
    }

    public String getMethod() {
        return request.getMethod();
    }

    public String getRequestUri() {
        return request.getRequestURI();
    }

    public String getRequestUrl() {
        return request.getRequestURL().toString(); // NPE here if servlet is bogus
    }

    public long getDateHeader(String name) throws ParseException {
        return request.getDateHeader(name);
    }

    public String getSingleValuedHeader(String name) throws IOException {
        Enumeration en = request.getHeaders(name);
        while (en.hasMoreElements()) {
            String value = (String)en.nextElement();
            if (en.hasMoreElements())
                throw new IOException("More than one value found for HTTP request header " + name);
            return value;
        }
        return null;
    }

    public String[] getHeaderNames() {
        Enumeration names = request.getHeaderNames();
        List out = new ArrayList();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            out.add(name);
        }
        return (String[])out.toArray(new String[0]);
    }

    public String[] getHeaderValues(String name) {
        Enumeration values = request.getHeaders(name);
        List out = new ArrayList();
        while (values.hasMoreElements()) {
            String value = (String)values.nextElement();
            out.add(value);
        }
        return (String[])out.toArray(new String[0]);
    }
}
