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
        Object value = null;
        value = _request.getAttribute( name );

        if ( value == null ) {
            int ppos;
            String subname;

            if ( name.startsWith( Request.PREFIX_HTTP_HEADER ) ) {
                // FIXME: Check punctuation
                ppos = name.indexOf( ".", Request.PREFIX_HTTP_HEADER.length() + 1 );
                subname = name.substring( ppos + 1 );
                value = _request.getHeader( subname );
            }
        }

        return value;
    }

    protected final HttpServletRequest _request;
    protected final HttpServletResponse _response;
}
