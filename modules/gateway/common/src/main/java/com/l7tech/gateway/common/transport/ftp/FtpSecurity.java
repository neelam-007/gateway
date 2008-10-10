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
public final class FtpSecurity implements Serializable {
    /** Map for looking up instance by wspName. */
    private static final Map<String, FtpSecurity> _wspNameMap = new HashMap<String, FtpSecurity>();

    public static final FtpSecurity FTP_UNSECURED = new FtpSecurity("ftp", "unsecured FTP");
    public static final FtpSecurity FTPS_EXPLICIT = new FtpSecurity("ftpsExplicit", "FTPS with Explicit SSL"); // AUTH_TLS with fallback to AUTH_SSL
    public static final FtpSecurity FTPS_IMPLICIT = new FtpSecurity("ftpsImplicit", "FTPS with Implicit SSL");

    private static final FtpSecurity[] _values = new FtpSecurity[] { FTP_UNSECURED, FTPS_EXPLICIT, FTPS_IMPLICIT };

    /** String representation used in XML serialization.
        Must be unique. Must not change for backward compatibility. */
    private final String _wspName;

    /** For printing in logs and audits. */
    private final String _printName;

    private FtpSecurity(String wspName, String printName) {
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

    public static FtpSecurity[] values() {
        return (FtpSecurity[]) _values.clone();
    }

    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public Object stringToObject(String s) throws IllegalArgumentException {
                return _wspNameMap.get(s);
            }

            public String objectToString(Object o) throws ClassCastException {
                return ((FtpSecurity)o).getWspName();
            }
        };
    }

    protected Object readResolve() throws ObjectStreamException {
        return _wspNameMap.get(_wspName);
    }
}
