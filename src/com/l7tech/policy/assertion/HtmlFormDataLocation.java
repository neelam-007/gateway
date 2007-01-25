/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.common.util.EnumTranslator;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum for locations in the request where an HTML Form data may be submitted.
 *
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class HtmlFormDataLocation implements Serializable {
    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal ++;

    private final String _name;

    /** Map for looking up instance by name.
        The keys are of type String. The values are of type {@link HtmlFormDataLocation}. */
    private static final Map _byName = new HashMap();

    public static final HtmlFormDataLocation ANYWHERE = new HtmlFormDataLocation("anywhere");
    public static final HtmlFormDataLocation URI = new HtmlFormDataLocation("request URI");
    public static final HtmlFormDataLocation BODY = new HtmlFormDataLocation("request body");

    private static final HtmlFormDataLocation[] _values = new HtmlFormDataLocation[]{ANYWHERE, URI, BODY};

    public static HtmlFormDataLocation[] values() {
        return (HtmlFormDataLocation[])_values.clone();
    }

    public static HtmlFormDataLocation valueOf(final String name) {
        return (HtmlFormDataLocation)_byName.get(name);
    }

    private HtmlFormDataLocation(final String name) {
        _name = name;
        //noinspection unchecked
        _byName.put(name, this);
    }

    /** @return a String representation suitable for UI display */
    public String toString() {
        return _name;
    }

    protected Object readResolve() throws ObjectStreamException {
        return _values[_ordinal];
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public String objectToString(Object target) {
                HtmlFormDataLocation location = (HtmlFormDataLocation)target;
                return location.toString();
            }

            public Object stringToObject(String name) throws IllegalArgumentException {
                HtmlFormDataLocation location = HtmlFormDataLocation.valueOf(name);
                if (location == null) throw new IllegalArgumentException("Unknown HtmlFormDataLocation name: '" + name + "'");
                return location;
            }
        };
    }
}
