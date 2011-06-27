/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;

/**
 * This concerns only the OUTBOUND RESPONSE.
 * For information recorded about the last INCOMING BACK-END RESPONSE, look for an HttpInboundResponseKnob.
 * Information about a response to be sent via HTTP.
 */
public interface HttpResponseKnob extends OutboundHeadersKnob {
    /**
     * Adds the specified {@link HttpCookie} to the response
     * @param cookie the {@link HttpCookie} to be added. Must not be null.
     */
    void addCookie(HttpCookie cookie);

    /**
     * Adds a value to the specified header.
     *
     * <p>Do not use this for WWW-Authenticate; see {@link #addChallenge} instead.</p>
     * 
     * @param name the name of the header to add a value to. Must not be null or empty.
     * @param value the value to add. Must not be null.
     */
    @Override
    void addHeader( String name, String value );

    /**
     * Add a WWW-Authenticate: challenge value to this response.  The actual challenge will not be sent unless
     * the request turns out to fail.
     *
     * @param value the content of the WWW-Authenticate to send, if an HTTP challenge is performed.
     */
    void addChallenge(String value);

    /**
     * Sets the response's HTTP status code. Must be positive.
     * @param code the HTTP status code for the response, e.g. {@link javax.servlet.http.HttpServletResponse#SC_FORBIDDEN}.
     */
    void setStatus(int code);

    /**
     * Gets the response's HTTP status code. A value of zero indicates that the status has not yet been set.
     * @return the HTTP status code for the response, e.g. {@link javax.servlet.http.HttpServletResponse#SC_FORBIDDEN}, or zero if it has not yet been set.
     */
    int getStatus();
}
