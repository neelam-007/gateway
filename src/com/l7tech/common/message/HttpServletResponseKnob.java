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
public class HttpServletResponseKnob extends AbstractHttpResponseKnob {
    private final HttpServletResponse response;
    private final List cookiesToSend = new ArrayList();

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
