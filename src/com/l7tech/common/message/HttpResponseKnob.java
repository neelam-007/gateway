/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.http.HttpCookie;

/**
 * Information about a response to be sent via HTTP.
 */
public interface HttpResponseKnob extends MessageKnob {
    /**
     * Adds the specified {@link Cookie} to the response
     * @param cookie the {@link Cookie} to be added. Must not be null.
     */
    void addCookie(HttpCookie cookie);

    /**
     * Sets the specified header to a date value
     * @param name the name of the header to be set. Must not be null or empty.
     * @param date the date value to set.
     */
    void setDateHeader(String name, long date);

    /**
     * Adds a date value to the specified header
     * @param name the name of the header to add a value to. Must not be null or empty.
     * @param date the date value to add.
     */
    void addDateHeader(String name, long date);

    /**
     * Sets a header to a specified value.  If the header already has multiple values it will be replaced with the specified single value.
     * @param name the name of the header to be set. Must not be null or empty.
     * @param value the value to set
     */
    void setHeader(String name, String value);

    /**
     * Adds a value to the specified header.  Do not use this for WWW-Authenticate; see {@link #addChallenge} instead.
     * @param name the name of the header to add a value to. Must not be null or empty.
     * @param value the value to add. Must not be null.
     */
    void addHeader(String name, String value);

    /**
     * Get the values for an http header to be returned
     * @param name name of the header
     * @return an array of header values, never null
     */
    String[] getHeaderValues(String name);

    /**
     * Add a WWW-Authenticate: challenge value to this response.  The actual challenge will not be sent unless
     * the request turns out to fail.
     *
     * @param value the content of the WWW-Authenticate to send, if an HTTP challenge is performed.
     */
    void addChallenge(String value);

    /**
     * Returns true if the response already contains at least one value for the header with the specified name.
     * @param name The name of the header to test
     * @return true if the response contains at least one value for the specified header, otherwise false.
     */
    boolean containsHeader(String name);

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
