/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpTransportMetadata extends TransportMetadata {
    public HttpTransportMetadata( HttpServletRequest request, HttpServletResponse response ) {
        _request = request;
        _response = response;
    }

    public HttpServletRequest getRequest() {
        return _request;
    }

    public HttpServletResponse getResponse() {
        return _response;
    }

    protected Object doGetParameter(String name) {
        return _request.getAttribute(name);
    }

    protected final HttpServletRequest _request;
    protected final HttpServletResponse _response;
}
