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

    public TransportProtocol getProtocol() {
        String scheme = _request.getScheme();
        if ( "http".equals( scheme ) )
            return TransportProtocol.HTTP;
        else if ( "https".equals( scheme ) && _request.isSecure() )
            return TransportProtocol.HTTPS;
        else
            return TransportProtocol.UNKNOWN;
    }

    protected Object doGetParameter(String name) {
        if ( name == null ) return null;
        Object value = null;
        value = _request.getAttribute( name );

        if ( value == null ) {
            int ppos;
            String subname;

            if ( Request.PARAM_HTTP_REQUEST_URI.equals( name ) ) {
                return _request.getRequestURI();
            } else if ( Request.PARAM_HTTP_METHOD.equals( name ) ) {
                return _request.getMethod();
            } else if ( Request.PARAM_SERVER_NAME.equals( name ) )
                return _request.getServerName();
            else if ( Request.PARAM_SERVER_PORT.equals( name ) )
                return new Integer( _request.getServerPort() );
            else if ( name.startsWith( Request.PREFIX_HTTP_HEADER ) ) {
                ppos = name.indexOf( ".", Request.PREFIX_HTTP_HEADER.length() - 1 );
                subname = name.substring( ppos + 1 );
                value = _request.getHeader( subname );
            }
        }

        return value;
    }

    protected final HttpServletRequest _request;
    protected final HttpServletResponse _response;
}
