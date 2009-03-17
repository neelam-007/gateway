/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.xml.soap.SoapUtil;

import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Map;

/** @author alex */
public class HttpRequestKnobAdapter implements HttpRequestKnob {
    private final String soapAction;

    public HttpRequestKnobAdapter() {
        this.soapAction = null;
    }

    public HttpRequestKnobAdapter(String soapAction) {
        this.soapAction = soapAction;
    }

    public String getHeaderSingleValue(String name) throws IOException {
        return getHeaderFirstValue(name);
    }

    public String getSoapAction() {
        return soapAction;
    }

    public HttpCookie[] getCookies() {return new HttpCookie[0];}

    public HttpMethod getMethod() {return HttpMethod.POST;}

    public String getMethodAsString() { return getMethod().name(); }

    public String getRequestUri() {return null;}

    public String getRequestUrl() {return null;}

    public URL getRequestURL() {return null;}

    public long getDateHeader(String name) throws ParseException {return 0;}

    public int getIntHeader(String name){return -1;}

    public String getHeaderFirstValue(String name) {
        if (name.equals(SoapUtil.SOAPACTION)) {
            return soapAction;
        }
        return null;
    }

    public String[] getHeaderNames() {return new String[0];}

    public String[] getHeaderValues(String name) {return new String[0];}

    public X509Certificate[] getClientCertificate() throws IOException {return new X509Certificate[0];}

    public boolean isSecure() {return false;}

    public String getParameter(String name) {return null;}

    public Map getParameterMap() {return null;}

    public String[] getParameterValues(String s) {return new String[0];}

    public Enumeration getParameterNames() {return null;}

    public String getQueryString() {return null;}

    public String getRemoteAddress() {return null;}

    public String getRemoteHost() {return null;}

    public int getLocalPort() {return 0;}

    public Object getConnectionIdentifier() {return new Object();}
}
