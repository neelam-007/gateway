/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;

/**
 * @author alex
 * @version $Revision$
 */
public class NameValueResolutionParameter extends ServiceResolutionParameter {
    public boolean matches(Request request) {
        Object ovalue = request.getParameter( _name );
        if ( ovalue instanceof String ) {
            String svalue = (String)ovalue;
            return ( svalue.equals( _value ) );
        }

        return false;
    }

    public String getName() {
        return _name;
    }

    public void setName( String name ) {
        _name = name;
    }

    public String getValue() {
        return _value;
    }

    public void setValue( String value ) {
        _value = value;
    }

    protected String _name;
    protected String _value;
}
