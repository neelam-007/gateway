/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link HttpResponseKnob} that knows how to place the HTTP response transport metadata
 * into a servlet response.
 */
public class HttpServletResponseKnob implements HttpResponseKnob {
    private final HttpServletResponse response;
    private final List headersToSend = new ArrayList();
    private final List cookiesToSend = new ArrayList();
    private int statusToSet;

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
        cookiesToSend.add(c);
    }

    private static final class Pair {
        Pair(String name, Object value) {
            this.name = name;
            this.value = value;
        }
        String name;
        Object value;
    }

    public void setDateHeader(String name, long date) {
        headersToSend.add(new Pair(name, new Long(date)));
    }

    public void addDateHeader(String name, long date) {
        headersToSend.add(new Pair(name, new Long(date)));
    }

    public void setHeader(String name, String value) {
        // Clear out any previous value
        for (Iterator i = headersToSend.iterator(); i.hasNext();) {
            Pair pair = (Pair)i.next();
            if (name.equals(pair.name)) i.remove();
        }
        headersToSend.add(new Pair(name, value));
    }

    public void addHeader(String name, String value) {
        headersToSend.add(new Pair(name, value));
    }

    public boolean containsHeader(String name) {
        for (Iterator i = headersToSend.iterator(); i.hasNext();) {
            Pair pair = (Pair)i.next();
            if (name.equals(pair.name)) return true;
        }

        return false;
    }

    public void setStatus(int code) {
        statusToSet = code;
    }

    public int getStatus() {
        return statusToSet;
    }

    /**
     * Begins the process of sending the response to the client by setting a status code and sending headers using the HttpServletResponse.
     */
    public void beginResponse() {
        response.setStatus(statusToSet);
        for (Iterator i = headersToSend.iterator(); i.hasNext();) {
            Pair pair = (Pair)i.next();
            final Object value = pair.value;
            if (value instanceof Long) {
                response.addDateHeader(pair.name, ((Long)value).longValue());
            } else {
                response.addHeader(pair.name, (String)value);
            }
        }
    }
}
