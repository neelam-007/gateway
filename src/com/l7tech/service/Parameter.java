/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

/**
 * Immutable.  Can't we borrow this from Axis?
 *
 * @author alex
 * @version $Revision$
 */
public class Parameter {
    public static final Parameter NULL = new Parameter( "null", Object.class, null );

    public Parameter( String name, Class type, Object value ) {
        _name = name;
        _type = type;
        _value = value;
    }

    public String getName() {
        return _name;
    }

    public Class getType() {
        return _type;
    }

    public Object getValue() {
        return _value;
    }

    protected String _name;
    protected Class _type;
    protected Object _value;
}
