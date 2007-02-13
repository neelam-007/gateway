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
 * Enum for data types of HTML Form data.
 *
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class HtmlFormDataType implements Serializable {
    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal ++;

    private final String _name;

    /** Map for looking up instance by name.
        The keys are of type String. The values are of type {@link HtmlFormDataType}. */
    private static final Map _byName = new HashMap();

    public static final HtmlFormDataType ANY = new HtmlFormDataType("<any>");
    public static final HtmlFormDataType NUMBER = new HtmlFormDataType("number");
    public static final HtmlFormDataType FILE = new HtmlFormDataType("file");

    private static final HtmlFormDataType[] _values = new HtmlFormDataType[]{ANY, NUMBER, FILE};

    public static HtmlFormDataType[] values() {
        return (HtmlFormDataType[])_values.clone();
    }

    public static HtmlFormDataType valueOf(final String name) {
        return (HtmlFormDataType)_byName.get(name);
    }

    private HtmlFormDataType(final String name) {
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
                HtmlFormDataType dataType = (HtmlFormDataType)target;
                return dataType.toString();
            }

            public Object stringToObject(String name) throws IllegalArgumentException {
                HtmlFormDataType dataType = HtmlFormDataType.valueOf(name);
                if (dataType == null) throw new IllegalArgumentException("Unknown HtmlFormDataType name: '" + name + "'");
                return dataType;
            }
        };
    }
}
