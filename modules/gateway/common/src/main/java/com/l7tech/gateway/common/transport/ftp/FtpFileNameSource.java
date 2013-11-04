/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.transport.ftp;

import com.l7tech.util.EnumTranslator;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rmak
 * @since SecureSpan 4.0
 */
public final class FtpFileNameSource implements Serializable {
    /** Map for looking up instance by wspName. */
    private static final Map<String, FtpFileNameSource> _wspNameMap = new HashMap<>();

    public static final FtpFileNameSource AUTO = new FtpFileNameSource("auto", "auto-generated");
    //TODO jwilliams: remove PATTERN source - only need ARGUMENT? N.B. used by SFTP too
    public static final FtpFileNameSource PATTERN = new FtpFileNameSource("pattern", "user-specified pattern");
    public static final FtpFileNameSource ARGUMENT = new FtpFileNameSource("argument", "user-specified pattern");

    private static final FtpFileNameSource[] _values = new FtpFileNameSource[] {AUTO, PATTERN, ARGUMENT};

    /** String representation used in XML serialization.
        Must be unique. Must not change for backward compatibility. */
    private final String _wspName;

    /** For printing in logs and audits. */
    private final String _printName;

    private FtpFileNameSource(String wspName, String printName) {
        _wspName = wspName;
        _printName = printName;
        if (_wspNameMap.put(wspName, this) != null) throw new IllegalArgumentException("Duplicate wspName: " + wspName);
    }

    public String getWspName() {
        return _wspName;
    }

    public String getPrintName() {
        return _printName;
    }

    public String toString() {
        return _printName;
    }

    public static FtpFileNameSource[] values() {
        return _values.clone();
    }

    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public Object stringToObject(String s) throws IllegalArgumentException {
                return _wspNameMap.get(s);
            }

            public String objectToString(Object o) throws ClassCastException {
                return ((FtpFileNameSource)o).getWspName();
            }
        };
    }

    protected Object readResolve() throws ObjectStreamException {
        return _wspNameMap.get(_wspName);
    }
}
