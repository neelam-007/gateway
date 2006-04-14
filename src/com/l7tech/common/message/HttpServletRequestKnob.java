/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.message;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.util.IteratorEnumeration;
import com.l7tech.server.transport.http.ConnectionId;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

/**
 * Implementation of {@link HttpRequestKnob} that knows how to obtain the HTTP request transport metadata
 * from a servlet request.
 */
public class HttpServletRequestKnob implements HttpRequestKnob {
    private final HttpServletRequest request;
    private final Map paramMap; // Necessary because you can't get parameters after reading the request's InputStream
    private final URL url;
    private static final String SERVLET_REQUEST_ATTR_X509CERTIFICATE = "javax.servlet.request.X509Certificate";

    public HttpServletRequestKnob(HttpServletRequest request) {
        if (request == null) throw new NullPointerException();
        Enumeration names = request.getParameterNames();
        Map params = new HashMap();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            params.put(name, request.getParameterValues(name));
        }
        this.paramMap = Collections.unmodifiableMap(params);
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
            for (int i=0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
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

    public String getParameter(String name) {
        String[] values = (String[]) paramMap.get(name);
        if (values == null) return null;
        return values[0];
    }

    public String getQueryString() {
        return request.getQueryString();
    }

    /**
     * @return the Map<String, String[]> of this request's parameters
     */
    public Map getParameterMap() {
        return paramMap;
    }

    public String[] getParameterValues(String s) {
        return (String[]) paramMap.get(s);
    }

    public Enumeration getParameterNames() {
        return new IteratorEnumeration(paramMap.keySet().iterator());
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
        return ConnectionId.getConnectionId();
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
