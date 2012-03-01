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
public class HttpRequestKnobAdapter extends TcpKnobAdapter implements HttpRequestKnob {
    private final String soapAction;

    public HttpRequestKnobAdapter() {
        this.soapAction = null;
    }

    public HttpRequestKnobAdapter(String soapAction) {
        this.soapAction = soapAction;
    }

    @Override
    public String getHeaderSingleValue(String name) throws IOException {
        return getHeaderFirstValue(name);
    }

    @Override
    public String getSoapAction() {
        return soapAction;
    }

    @Override
    public HttpCookie[] getCookies() {return new HttpCookie[0];}

    @Override
    public HttpMethod getMethod() {return HttpMethod.POST;}

    @Override
    public String getMethodAsString() { return getMethod().name(); }

    @Override
    public String getRequestUri() {return "";}

    @Override
    public String getRequestUrl() {return null;}

    @Override
    public URL getRequestURL() {return null;}

    @Override
    public long getDateHeader(String name) throws ParseException {return 0;}

    @Override
    public int getIntHeader(String name){return -1;}

    @Override
    public String getHeaderFirstValue(String name) {
        if (name.equals(SoapUtil.SOAPACTION)) {
            return soapAction;
        }
        return null;
    }

    @Override
    public String[] getHeaderNames() {return new String[0];}

    @Override
    public String[] getHeaderValues(String name) {return new String[0];}

    @Override
    public X509Certificate[] getClientCertificate() throws IOException {return new X509Certificate[0];}

    @Override
    public boolean isSecure() {return false;}

    @Override
    public String getParameter(String name) {return null;}

    @Override
    public Map getParameterMap() {return null;}

    @Override
    public String[] getParameterValues(String s) {return new String[0];}

    @Override
    public Enumeration getParameterNames() {return null;}

    @Override
    public String getQueryString() {return null;}

    @Override
    public Object getConnectionIdentifier() {return new Object();}
}
