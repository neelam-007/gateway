/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ftprouting;

import com.l7tech.util.EnumTranslator;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rmak
 * @since SecureSpan 4.0
 */
public final class FtpCredentialsSource implements Serializable {
    /** Map for looking up instance by wspName. */
    private static final Map<String, FtpCredentialsSource> _wspNameMap = new HashMap<String, FtpCredentialsSource>();

    public static final FtpCredentialsSource PASS_THRU = new FtpCredentialsSource("passThru", "pass-through");
    public static final FtpCredentialsSource SPECIFIED = new FtpCredentialsSource("specified", "specified");

    private static final FtpCredentialsSource[] _values = new FtpCredentialsSource[] { PASS_THRU, SPECIFIED };

    /** String representation used in XML serialization.
        Must be unique. Must not change for backward compatibility. */
    private final String _wspName;

    /** For printing in logs and audits. */
    private final String _printName;

    private FtpCredentialsSource(String wspName, String printName) {
        _wspName = wspName;
        _printName = printName;
        if (_wspNameMap.put(wspName, this) != null) throw new IllegalArgumentException("Duplicate wspName: " + wspName);
    }

    public String getWspName() {
        return _wspName;
    }

    public String getDisplayName() {
        return _printName;
    }

    public String toString() {
        return _printName;
    }

    public static FtpCredentialsSource[] values() {
        return (FtpCredentialsSource[]) _values.clone();
    }

    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public Object stringToObject(String s) throws IllegalArgumentException {
                return _wspNameMap.get(s);
            }

            public String objectToString(Object o) throws ClassCastException {
                return ((FtpCredentialsSource)o).getWspName();
            }
        };
    }

    protected Object readResolve() throws ObjectStreamException {
        return _wspNameMap.get(_wspName);
    }
}
