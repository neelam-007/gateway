/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * @author alex
 * @version $Revision$
 */
public class RoutingStatus {
    public static final RoutingStatus NONE = new RoutingStatus( -1, "None" );
    public static final RoutingStatus ATTEMPTED = new RoutingStatus( 0, "Attempted" );
    public static final RoutingStatus ROUTED = new RoutingStatus( 1, "Routed" );

    private RoutingStatus( int num, String name ) {
        _num = num;
        _name = name;
    }

    public String toString() {
        return "<RoutingStatus num='" + _num + "' name='" + _name + "'/>";
    }

    private int _num;
    private String _name;
}
