/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import org.apache.commons.httpclient.Cookie;

import java.io.IOException;
import java.text.ParseException;

/**
 * Information about a request that arrived over HTTP.
 *
 * Note that in keeping with the Java Servlet API, requests allow headers to be read but not set.
 */
public interface HttpRequestKnob extends TcpKnob {
    /**
     * @return the array of {@link Cookie}s that were found in this request. Never null, but may be empty.
     */
    Cookie[] getCookies();

    /**
     * @return the method (e.g. GET, PUT, POST, DELETE) used in this request. Never null or empty.
     */
    String getMethod();

    /**
     * @return the URI part of the URL for this request (e.g. /ssg/soap). Never null or empty.
     */
    String getRequestUri();

    /**
     * @return the complete URL of this request (e.g. https://ssg.example.com/ssg/soap). Never null or empty.
     */
    String getRequestUrl();

    /**
     * @param name the name of the header whose value should be retrieved. Must not be null.
     * @return the value of the specified header, expressed as a long, -1 if the requested header was not present
     * @throws ParseException if the specified header cannot be converted to a date
     */
    long getDateHeader(String name) throws ParseException;

    /**
     * @param name the name of the header whose value should be retrieved. Must not be null.
     * @return the first value of the specified header, or null if the requested header was not present.
     * @throws IOException if the header is multivalued
     */
    String getHeaderSingleValue(String name) throws IOException;

    /**
     * @return an array of the names of all the headers in the request, or an empty array if there are none. Never null.
     */
    String[] getHeaderNames();

    /**
     * @param name the name of the header whose values should be retrieved. Must not be null.
     * @return an array containing all the values of the specified header, which will be empty if the specified header is not
     *         present in the request. Never null.
     */
    String[] getHeaderValues(String name);
}
