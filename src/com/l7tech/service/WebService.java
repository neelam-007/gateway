/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;

/**
 * @author alex
 */
public abstract class WebService extends Service {
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

    public String getMethod() {
        return _method;
    }

    public void setMethod(String method) {
        _method = method;
    }

    public int getPort() {
        return _port;
    }

    public void setPort(int port) {
        _port = port;
    }

    protected String _hostname;
    protected String _uri;
    protected String _method;
    protected int _port = 80;
}
