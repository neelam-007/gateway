/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

/**
 * @author alex
 * @version $Revision$
 */
public class TransportProtocol {
    public static final TransportProtocol HTTP  = new TransportProtocol( 0, "http" );
    public static final TransportProtocol HTTPS = new TransportProtocol( 1, "https" );

    private TransportProtocol( int num, String name ) {
        _num = num;
        _name = name;
    }

    private int _num;
    private String _name;
}
