/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.util.Pair;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link HttpResponseKnob} that knows how to place the HTTP response transport metadata
 * into a servlet response.
 */
public class HttpServletResponseKnob extends AbstractHttpResponseKnob {
    private final HttpServletResponse response;
    private final List<Cookie> cookiesToSend = new ArrayList<Cookie>();

    public HttpServletResponseKnob(HttpServletResponse response) {
        if (response == null) throw new NullPointerException();
        this.response = response;
    }

    public void addCookie(HttpCookie cookie) {
        cookiesToSend.add(CookieUtils.toServletCookie(cookie));
    }

    /**
     * Begins the process of sending the response to the client by setting a status code and sending headers using the HttpServletResponse.
     */
    public void beginResponse() {
        response.setStatus(statusToSet);
        for (Pair<String, Object> pair : headersToSend) {
            final Object value = pair.right;
            if (value instanceof Long) {
                response.addDateHeader(pair.left, (Long)value);
            } else {
                response.addHeader(pair.left, (String)value);
            }
        }

        for (Cookie cookie : cookiesToSend) {
            response.addCookie(cookie);
        }
    }

    public boolean hasChallenge() {
        return !challengesToSend.isEmpty();
    }

    /**
     * Add the challenge headers to the response.  The challenges will be sorted in reverse alphabetical order
     * so that Digest will be preferred over Basic, if both are present.
     */
    public void beginChallenge() {
        Collections.sort(challengesToSend, String.CASE_INSENSITIVE_ORDER);
        Collections.reverse(challengesToSend);
        for (String challenge : challengesToSend) {
            response.addHeader("WWW-Authenticate", challenge);
        }
    }

    /**
     * Think twice before using this. The HttpServletResponse should be restricted for the usage of the http transport.
     * Other uses may interfeer with the stealth mode implementation. See ResponseKillerValve for more information.
     * todo, remove this completly?
     * @deprecated dont touch this unless you really need it.
     * @return the raw HttpServletResponse
     * */
    public HttpServletResponse getHttpServletResponse() {
        return response;
    }
}
