/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

/**
 * @author alex
 * @version $Revision$
 */
public class TransportProtocol {
    public static final TransportProtocol UNKNOWN = new TransportProtocol( -1, "unknown" );
    public static final TransportProtocol HTTP  = new TransportProtocol( 0, "http" );
    public static final TransportProtocol HTTPS = new TransportProtocol( 1, "https" );
    public static final TransportProtocol JMS = new TransportProtocol( 2, "jms" );

    private TransportProtocol( int num, String name ) {
        _num = num;
        _name = name;
    }

    public String toString() {
        StringBuffer result = new StringBuffer( "<class " );
        result.append( getClass().getName() );
        result.append( ": #" );
        result.append( _num );
        result.append( ": " );
        result.append( _name );
        result.append( ">" );
        return result.toString();
    }

    private int _num;
    private String _name;
}
