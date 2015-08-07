/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;

import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Map;

/**
 * Information about a request that arrived over HTTP.
 *
 * Note that in keeping with the Java Servlet API, requests allow headers to be read but not set.
 */
public interface HttpRequestKnob extends TlsKnob, TcpKnob, UriKnob, HasSoapAction, HasHeaders {
    /**
     * @return the array of {@link HttpCookie}s that were found in this request. Never null, but may be empty.
     */
    HttpCookie[] getCookies();

    /**
     * @return the method (e.g. GET, PUT, POST, DELETE) used in this request. Never null, but may be
     * {@link HttpMethod#OTHER}, in which case {@link #getMethodAsString()} will return the actual method from the request. 
     */
    HttpMethod getMethod();

    /**
     * @return the HTTP method as a String.  Never null or empty.
     */
    String getMethodAsString();

    /**
     * @return the complete URL of this request (e.g. https://ssg.example.com/ssg/soap). Never null or empty.
     */
    String getRequestUrl();

    URL getRequestURL();

    /**
     * @param name the name of the header whose value should be retrieved. Must not be null.
     * @return the value of the specified header, expressed as a long, -1 if the requested header was not present
     * @throws ParseException if the specified header cannot be converted to a date
     */
    long getDateHeader(String name) throws ParseException;

    /**
     * @param name the name of the header whose value should be retrieved. Must not be null.
     * @return the value of the specified header, expressed as a int, -1 if the request doesn't have a header of this name or it cannot be parsed
     */
    int getIntHeader(String name);

    /**
     * Get the value of a header.  If the header has multiple values, this will return the first value.
     * <p/>
     * This method is faster than {@link #getHeaderSingleValue(String)} and should be preferred when there
     * is no risk of a bug or security hole if subsequent multiple values are ignored.
     *
     * @param name the name of the header whose value should be retrieved.  Required.
     * @return the first value of the specified header, or null if the requested header was not present.
     */
    String getHeaderFirstValue(String name);

    /**
     * Get the value of a header, enforcing that only one value is present.
     * <p/>
     * This method is slower than {@link #getHeaderFirstValue(String)} but should be used when there is
     * risk of a bug or security problem if multiple values for a header are present and only the first value
     * is examined.
     *
     * @param name the name of the header whose value should be retrieved. Must not be null.
     * @return the first value of the specified header, or null if the requested header was not present.
     * @throws IOException if the header is multivalued
     */
    String getHeaderSingleValue(String name) throws IOException;

    String getParameter(String name) throws IOException;

    /**
     * @return a Map&lt;String,String[]&gt;
     */
    Map getParameterMap() throws IOException;

    String[] getParameterValues(String s) throws IOException;

    Enumeration getParameterNames() throws IOException;

    Object getConnectionIdentifier();

    String getQueryString();
}
