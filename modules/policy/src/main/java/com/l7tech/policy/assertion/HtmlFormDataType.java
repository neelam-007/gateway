/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.util.EnumTranslator;

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
public class HtmlFormDataType implements Serializable, Cloneable {
    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal ++;

    /** String representation used in XML serialization. Must not change for backward compatibility. */
    private final String _wspName;

    /** For UI display. Can be internationalized. */
    private final String _displayName;

    /** Map for looking up instance by wspName.
        The keys are of type String. The values are of type {@link HtmlFormDataType}. */
    private static final Map<String, HtmlFormDataType> _byWspName = new HashMap<String, HtmlFormDataType>();

    /** Map for looking up instance by displayName.
        The keys are of type String. The values are of type {@link HtmlFormDataType}. */
    private static final Map<String, HtmlFormDataType> _byDisplayName = new HashMap<String, HtmlFormDataType>();

    // Warning: Never change wspName; in order to maintain backward compatibility.
    public static final HtmlFormDataType ANY = new HtmlFormDataType("any", "<any>");
    public static final HtmlFormDataType NUMBER = new HtmlFormDataType("number", "number");
    public static final HtmlFormDataType FILE = new HtmlFormDataType("file", "file");
    public static final HtmlFormDataType STRING = new HtmlFormDataType("string", "string");

    private static final HtmlFormDataType[] _values = new HtmlFormDataType[]{ANY, NUMBER, FILE, STRING};

    public static HtmlFormDataType[] values() {
        return _values.clone();
    }

    public static HtmlFormDataType fromWspName(final String wspName) {
        return _byWspName.get(wspName);
    }

    public static HtmlFormDataType fromDisplayName(final String displayName) {
        return _byDisplayName.get(displayName);
    }

    private HtmlFormDataType(final String wspName, final String displayName) {
        _wspName = wspName;
        _displayName = displayName;
        _byWspName.put(wspName, this);
        _byDisplayName.put(displayName, this);
    }

    public String getWspName() {
        return _wspName;
    }

    public String getDisplayName() {
        return _displayName;
    }

    /** @return a String representation suitable for UI display */
    public String toString() {
        return _displayName;
    }

    protected Object readResolve() throws ObjectStreamException {
        return _values[_ordinal];
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            @Override
            public String objectToString(Object target) {
                HtmlFormDataType dataType = (HtmlFormDataType)target;
                return dataType.getWspName();
            }

            @Override
            public Object stringToObject(String wspName) throws IllegalArgumentException {
                HtmlFormDataType dataType = HtmlFormDataType.fromWspName(wspName);
                if (dataType == null) throw new IllegalArgumentException("Unknown HtmlFormDataType: '" + wspName + "'");
                return dataType;
            }
        };
    }
}
