/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import java.net.URL;
import java.util.Set;

/**
 * @author alex
 */
public abstract class WebService extends Service {
    public static final Method METHOD_POST = new Method("POST");
    public static final Method METHOD_GET = new Method("GET");

    public static class Method {
        private Method( String method ) {
            _method = method;
        }
        String _method;
    }

    public WebService( String name, Set operations, Method method, String hostname, int port, String uri ) {
        super( name, operations );
        _method = method;
        _hostname = hostname;
        _uri = uri;
        _port = port;
    }

    public WebService( String name, Set operations, Method method, URL url ) {
        super( name, operations );
        String protocol = url.getProtocol();
        if ( "http".equalsIgnoreCase( protocol ) || "https".equalsIgnoreCase( protocol ) ) {
            _method = method;
            _hostname = url.getHost();
            _port = url.getPort();
            _uri = url.getPath();
        } else throw new IllegalArgumentException( "Only http-style URLs are supported!" );
    }

    /** Default constructor. Only for Hibernate, don't call! */
    public WebService() {
        super();
    }

    public Method getMethod() {
        return _method;
    }

    public void setMethod( Method method) {
        _method = method;
    }

    public String getHostname() {
        return _hostname;
    }

    public void setHostname(String hostname) {
        _hostname = hostname;
    }

    public String getUri() {
        return _uri;
    }

    public void setUri(String uri) {
        _uri = uri;
    }


    public int getPort() {
        return _port;
    }

    public void setPort(int port) {
        _port = port;
    }

    protected Method _method;
    protected String _hostname;
    protected int _port = 80;
    protected String _uri;
}
