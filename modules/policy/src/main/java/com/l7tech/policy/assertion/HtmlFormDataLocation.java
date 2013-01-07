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
 * Enum for locations in the request where an HTML Form data may be submitted.
 *
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class HtmlFormDataLocation implements Serializable, Cloneable {
    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal ++;

    /** String representation used in XML serialization. Must not change for backward compatibility. */
    private final String _wspName;

    /** For UI display. Can be internationalized. */
    private final String _displayName;

    /** Map for looking up instance by wspName.
        The keys are of type String. The values are of type {@link HtmlFormDataLocation}. */
    private static final Map<String, HtmlFormDataLocation> _byWspName = new HashMap<String, HtmlFormDataLocation>();

    /** Map for looking up instance by displayName.
        The keys are of type String. The values are of type {@link HtmlFormDataLocation}. */
    private static final Map<String, HtmlFormDataLocation> _byDisplayName = new HashMap<String, HtmlFormDataLocation>();

    // Warning: Never change wspName; in order to maintain backward compatibility.
    public static final HtmlFormDataLocation ANYWHERE = new HtmlFormDataLocation("anywhere", "anywhere");
    public static final HtmlFormDataLocation URL = new HtmlFormDataLocation("requestUrl", "request URL");
    public static final HtmlFormDataLocation BODY = new HtmlFormDataLocation("requestBody", "request body");

    private static final HtmlFormDataLocation[] _values = new HtmlFormDataLocation[]{ANYWHERE, URL, BODY};

    public static HtmlFormDataLocation[] values() {
        return _values.clone();
    }

    public static HtmlFormDataLocation fromWspName(final String wspName) {
        return _byWspName.get(wspName);
    }

    public static HtmlFormDataLocation fromDisplayName(final String displayName) {
        return _byDisplayName.get(displayName);
    }

    private HtmlFormDataLocation(final String wspName, final String displayName) {
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
                HtmlFormDataLocation location = (HtmlFormDataLocation)target;
                return location.getWspName();
            }

            @Override
            public Object stringToObject(String wspName) throws IllegalArgumentException {
                HtmlFormDataLocation location = HtmlFormDataLocation.fromWspName(wspName);
                if (location == null) throw new IllegalArgumentException("Unknown HtmlFormDataLocation: '" + wspName + "'");
                return location;
            }
        };
    }
}
