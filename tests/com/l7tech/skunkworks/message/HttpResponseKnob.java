/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import org.apache.commons.httpclient.Cookie;

/**
 * Information about a response to be sent via HTTP.
 */
public interface HttpResponseKnob {
    /**
     * Adds the specified {@link Cookie} to the response
     * @param cookie the {@link Cookie} to be added. Must not be null.
     */
    void addCookie(Cookie cookie);

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
     * Adds a value to the specified header.
     * @param name the name of the header to add a value to. Must not be null or empty.
     * @param value the value to add. Must not be null.
     */
    void addHeader(String name, String value);

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
}
