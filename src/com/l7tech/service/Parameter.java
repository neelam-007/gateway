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
    public static final Parameter NULL_OBJECT = new Parameter( "null", Object.class, null );
    public static final Parameter NULL_STRING = new Parameter( "null", String.class, null );

    public Parameter( String name, Class type, Object value ) {
        _name = name;
        _type = type;
        _value = value;
    }

    /** Default constructor, only for Hibernate. Don't call! */
    public Parameter() { }

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
