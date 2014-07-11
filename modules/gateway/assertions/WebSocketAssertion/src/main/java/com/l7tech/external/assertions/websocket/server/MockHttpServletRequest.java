package com.l7tech.external.assertions.websocket.server;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * This class was created to support policy processing for messages passed via TCP (through websockets).
 */
public class MockHttpServletRequest implements HttpServletRequest {

    private Hashtable<String, Vector<String>> headers = new Hashtable<String, Vector<String>>();
    private Hashtable<String, Object> attributes = new Hashtable<String, Object>();
    private ConcurrentHashMap<String, String[]> parameters = new ConcurrentHashMap<String, String[]>();
    Vector<Locale> locales = new Vector<Locale>();

    private String authType;
    private Cookie[] cookies;
    private String method;
    private String pathInfo;
    private String pathTranslated;
    private String contextPath;
    private String queryString;
    private String remoteUser;
    private String requestedSessionId;
    private Principal principal;
    private String requestURI;
    private StringBuffer requestURL;
    private String servletPath;
    private HttpSession httpSession;
    private boolean requestedSessionIdValid;
    private boolean requestedSessionIdFromCookie;
    private boolean requestedSessionIdFromUrl;
    private boolean requestedSessionIdFromURL;
    private String characterEncoding;
    private int remotePort;
    private String localName;
    private String localAddr;
    private int localPort;
    private int contentLength;
    private String contentType;
    private String remoteHost;
    private String remoteAddr;
    private boolean secure;
    private String serverName;
    private int serverPort;
    private String protocol;
    private String scheme;
    private Locale preferredLocale;

    public MockHttpServletRequest() {

    }

    public MockHttpServletRequest(HttpServletRequest request) {
        populateHeaders(request);
        populateAttributes(request);
        populateParameters(request);
        populateLocales(request);
        this.authType = request.getAuthType();
        this.cookies = request.getCookies().clone();
        this.method = request.getMethod();
        this.pathInfo = request.getPathInfo();
        this.pathTranslated = request.getPathTranslated();
        this.contextPath = request.getContextPath();
        this.queryString = request.getQueryString();
        this.remoteUser = request.getRemoteUser();
        this.requestedSessionId = request.getRequestedSessionId();
        this.principal = request.getUserPrincipal();
        this.requestURI = request.getRequestURI();
        this.requestURL = request.getRequestURL();
        this.servletPath = request.getServletPath();
        this.httpSession = null; //Session won't be maintained.
        this.requestedSessionIdValid = request.isRequestedSessionIdValid();
        this.requestedSessionIdFromURL = request.isRequestedSessionIdFromURL();
        this.requestedSessionIdFromUrl = request.isRequestedSessionIdFromUrl();
        this.requestedSessionIdFromCookie = request.isRequestedSessionIdFromCookie();
        this.characterEncoding = request.getCharacterEncoding();
        this.remotePort = request.getRemotePort();
        this.localName = request.getLocalName();
        this.localAddr = request.getLocalAddr();
        this.localPort = request.getLocalPort();
        this.contentLength = request.getContentLength();
        this.contentType = request.getContentType();
        this.remoteHost = request.getRemoteHost();
        this.remoteAddr = request.getRemoteAddr();
        this.secure = request.isSecure();
        this.serverName = request.getServerName();
        this.serverPort = request.getServerPort();
        this.protocol = request.getProtocol();
        this.scheme = request.getScheme();
        this.preferredLocale = request.getLocale();


    }
    @Override
    public String getAuthType() {
        return authType;
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public long getDateHeader(String s) {
        Vector<String> dateHeader = headers.get(s.toLowerCase());
        if (dateHeader == null) {
            return -1;
        }
        String date = dateHeader.firstElement();
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        Date d;
        try {
            d = format.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException();
        }
        return d.getTime();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getHeader(String s) {
        Vector<String> headerEnum = headers.get(s.toLowerCase());
        if ( headerEnum != null) {
            return headerEnum.firstElement();
        }
        return null;
    }

    public void setHeader(String key, String value) {
        if ( headers.containsKey(key.toLowerCase())) {
            headers.get(key.toLowerCase()).add(value);
        } else {
            Vector v = new Vector<String>();
            v.add(value);
            headers.put(key.toLowerCase(), v );
        }
    }

    @Override
    public Enumeration getHeaders(String s) {
        Vector v = headers.get(s.toLowerCase());
        return v==null ? new Vector().elements() : v.elements();
    }

    @Override
    public Enumeration getHeaderNames() {
        return headers.keys();
    }

    @Override
    public int getIntHeader(String s) {
        Vector<String> intHeader = headers.get(s.toLowerCase());
        if ( intHeader == null) {
            return -1;
        }
        return Integer.parseInt(intHeader.firstElement());
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return pathTranslated;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRemoteUser() {
        return remoteUser;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        return requestURL;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public HttpSession getSession(boolean b) {
        return httpSession;
    }

    @Override
    public HttpSession getSession() {
        return httpSession;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return requestedSessionIdValid;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return requestedSessionIdFromCookie;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return requestedSessionIdFromURL;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return requestedSessionIdFromUrl;
    }

    @Override
    public Object getAttribute(String s) {
        return attributes.get(s);
    }

    @Override
    public Enumeration getAttributeNames() {
        return attributes.keys();
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        this.characterEncoding = s;
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getParameter(String s) {
        String [] params = parameters.get(s);
        if (params != null && params.length > 0) {
            return params[0];
        }
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        return parameters.keys();
    }

    @Override
    public String[] getParameterValues(String s) {
        return parameters.get(s);
    }

    @Override
    public Map getParameterMap() {
        return parameters;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributes.put(s,o);
    }

    @Override
    public void removeAttribute(String s) {
        attributes.remove(s);
    }

    @Override
    public Locale getLocale() {
        return preferredLocale;
    }

    @Override
    public Enumeration getLocales() {
        return locales.elements();
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public String getLocalAddr() {
        return localAddr;
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    private void populateHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            Enumeration e = request.getHeaders(header);
            Vector v = new Vector();
            while ( e.hasMoreElements()) {
                v.add(e.nextElement());
            }
            headers.put(header.toLowerCase(), v);
        }
    }

    private void populateAttributes(HttpServletRequest request) {
        Enumeration headerNames = request.getAttributeNames();
        while (headerNames.hasMoreElements()) {
            String header = (String)headerNames.nextElement();
            attributes.put(header, request.getAttribute(header));
        }
    }

    private void populateParameters(HttpServletRequest request) {
        Enumeration headerNames = request.getParameterNames();
        while (headerNames.hasMoreElements()) {
            String header = (String)headerNames.nextElement();
            parameters.put(header, request.getParameterValues(header));
        }
    }

    private void populateLocales(HttpServletRequest request) {
        Enumeration<Locale> localeEnum = request.getLocales();
        while (localeEnum.hasMoreElements()) {
            locales.add(localeEnum.nextElement());
        }
    }



    @Override
    public boolean authenticate( HttpServletResponse response ) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login( String username, String password ) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Part getPart( String name ) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync( ServletRequest servletRequest, ServletResponse servletResponse ) throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncStarted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException();
    }
}
