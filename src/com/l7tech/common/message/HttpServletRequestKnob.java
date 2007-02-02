/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.message;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.ParameterizedString;
import com.l7tech.common.util.IteratorEnumeration;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.mime.ContentTypeHeader;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Implementation of {@link HttpRequestKnob} that knows how to obtain the HTTP request transport metadata
 * from a servlet request.
 */
public class HttpServletRequestKnob implements HttpRequestKnob {
    /** Map&lt;String, String[]&gt; of parameters found in the URL query string. */
    private Map queryParams;
    /** Map&lt;String, String[]&gt; of parameters found in the request message body. */
    private Map requestBodyParams;
    /** Map&lt;String, String[]&gt; of all request parameters; i.e., union of {@link #queryParams} and {@link #requestBodyParams}. */
    private Map allParams;

    private final HttpServletRequest request;
    private final URL url;
    private static final String SERVLET_REQUEST_ATTR_X509CERTIFICATE = "javax.servlet.request.X509Certificate";
    private static final String SERVLET_REQUEST_ATTR_CONNECTION_ID = "com.l7tech.server.connectionIdentifierObject";
    private static final int MAX_FORM_POST = 512 * 1024;

    public HttpServletRequestKnob(HttpServletRequest request) {
        this.request = request;
        try {
            this.url = new URL(request.getRequestURL().toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException("HttpServletRequest had invalid URL", e);
        }
    }

    public HttpCookie[] getCookies() {
        Cookie[] cookies = request.getCookies();
        List out = new ArrayList();
        if(cookies!=null) {
            for (Iterator i = out.iterator(); i.hasNext();) {
                Cookie cookie = (Cookie) i.next();
                out.add(CookieUtils.fromServletCookie(cookie, false));
            }
        }
        return (HttpCookie[]) out.toArray(new HttpCookie[out.size()]);
    }

    public String getMethod() {
        return request.getMethod();
    }

    public String getRequestUri() {
        return request.getRequestURI();
    }

    public String getParameter(String name) throws IOException {
        if (queryParams == null || requestBodyParams == null) {
            collectParameters();
        }
        String[] values = (String[]) allParams.get(name);
        if (values != null && values.length >= 1) {
            return values[0];
        } else {
            return null;
        }
    }

    private void collectParameters() throws IOException {
        String q = getQueryString();
        if (q == null || q.length() == 0) {
            queryParams = Collections.emptyMap();
        } else {
            queryParams = Collections.unmodifiableMap(ParameterizedString.parseQueryString(q, true)); // TODO configurable strictness?
        }

        // Check for PUT or POST; otherwise there can't be body params
        int len = request.getContentLength();
        if (len > MAX_FORM_POST) throw new IOException(MessageFormat.format("Request too long (Content-Type = {0} bytes)", new Object[] { Integer.valueOf(len) }));
        if (len == -1 || !("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod()))) {
            nobody();
            return;
        }

        ContentTypeHeader ctype = ContentTypeHeader.parseValue(request.getHeader("Content-Type"));
        if (!ctype.matches(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED)) {
            // This stanza is copied because we don't want to parse the Content-Type unnecessarily
            nobody();
            return;
        }

        String enc = ctype.getEncoding();
        byte[] buf = HexUtils.slurpStream(request.getInputStream());
        String blob = new String(buf, enc);
        requestBodyParams = new HashMap();
        ParameterizedString.parseParameterString(requestBodyParams, blob, true);

        if (queryParams.isEmpty() && requestBodyParams.isEmpty()) {
            // Nothing left to do
            allParams = Collections.emptyMap();
            return;
        }

        // Combines queryParams and requestBodyParams into allParams.
        Map allParams = new HashMap(queryParams);
        for (Iterator i = requestBodyParams.keySet().iterator(); i.hasNext(); ) {
            String name = (String) i.next();
            String[] bodyValues = (String[]) requestBodyParams.get(name);
            String[] queryValues = (String[]) queryParams.get(name);
            if (queryValues == null) {
                allParams.put(name, bodyValues);
            } else {
                String[] allValues = new String[queryValues.length + bodyValues.length];
                System.arraycopy(queryValues, 0, allValues, 0, queryValues.length);
                System.arraycopy(bodyValues, 0, allValues, queryValues.length, bodyValues.length);
                allParams.put(name, allValues);
            }
        }
        this.allParams = Collections.unmodifiableMap(allParams);
    }

    /**
     * Signal not to try to parse parameters again -- request is not an acceptable form post
     */
    private void nobody() {
        requestBodyParams = Collections.emptyMap();
        allParams = queryParams;
    }

    public String getQueryString() {
        return request.getQueryString();
    }

    /**
     * @return the Map&lt;String, String[]&gt; of this request's parameters
     */
    public Map getParameterMap() throws IOException {
        if (allParams == null) collectParameters();
        return allParams;
    }

    public String[] getParameterValues(String s) throws IOException {
        if (allParams == null) collectParameters();
        return (String[]) allParams.get(s);
    }

    public Enumeration getParameterNames() throws IOException {
        if (allParams == null) collectParameters();
        return new IteratorEnumeration(allParams.keySet().iterator());
    }

    /**
     * @return the Map&lt;String, String[]&gt; of parameters found in the URL query string.
     * @since SecureSpan 3.7
     */
    public Map getQueryParameterMap() throws IOException {
        if (queryParams == null) collectParameters();
        return queryParams;
    }

    /**
     * @return the Map&lt;String, String[]&gt; of parameters found in the request message body.
     * @since SecureSpan 3.7
     */
    public Map getRequestBodyParameterMap() throws IOException {
        if (requestBodyParams == null) collectParameters();
        return requestBodyParams;
    }

    public String getRequestUrl() {
        return request.getRequestURL().toString(); // NPE here if servlet is bogus
    }

    public URL getRequestURL() {
        return url;
    }

    public long getDateHeader(String name) throws ParseException {
        return request.getDateHeader(name);
    }

    public int getIntHeader(String name) {
        try {
            return request.getIntHeader(name);
        }
        catch(NumberFormatException nfe) {
            return -1;
        }
    }

    public String getHeaderSingleValue(String name) throws IOException {
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

    public X509Certificate[] getClientCertificate() throws IOException {
        Object param = request.getAttribute(SERVLET_REQUEST_ATTR_X509CERTIFICATE);
        if (param == null)
            return null;
        if (param instanceof X509Certificate)
            return new X509Certificate[] { (X509Certificate)param };
        if (param instanceof X509Certificate[])
            return (X509Certificate[])param;
        throw new IOException("Request X509Certificate was unsupported type " + param.getClass());
    }

    public Object getConnectionIdentifier() {
        return request.getAttribute(SERVLET_REQUEST_ATTR_CONNECTION_ID);
    }

    public boolean isSecure() {
        return request.isSecure();
    }

    public String getRemoteAddress() {
        return request.getRemoteAddr();
    }

    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    public int getLocalPort() {
        return request.getServerPort();
    }

    /** @return the raw HttpServletRequest instance. */
    public HttpServletRequest getHttpServletRequest() {
        return request;
    }
}
