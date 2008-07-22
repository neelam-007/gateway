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
 * Enum for type of line break (i.e., LF, CR, CR-LF).
 *
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 4.3
 */
public class LineBreak implements Serializable {
    /** String representation used in XML serialization as well as display. Must not change for backward compatibility. */
    private final String _name;

    /** The line break characters. */
    private final String _characters;

    /** Map for looking up instance by name.
        The keys are of type String. The values are of type {@link LineBreak}. */
    private static final Map _nameMap = new HashMap();

    // Warning: Never change wspName; in order to maintain backward compatibility.
    public static final LineBreak LF = new LineBreak("LF", "\n");
    public static final LineBreak CR = new LineBreak("CR", "\r");
    public static final LineBreak CRLF = new LineBreak("CR-LF", "\r\n");

    private static final LineBreak[] _values = new LineBreak[]{LF, CR, CRLF};

    public static LineBreak[] values() {
        return (LineBreak[])_values.clone();
    }

    public static LineBreak fromName(final String wspName) {
        return (LineBreak) _nameMap.get(wspName);
    }

    private LineBreak(final String name, final String characters) {
        _name = name;
        _characters = characters;
        if(_nameMap.put(name, this) != null) throw new IllegalArgumentException("Duplicate name: " + name);
    }

    public String getName() {
        return _name;
    }

    public String getCharacters() {
        return _characters;
    }

    /** @return a String representation suitable for UI display */
    public String toString() {
        return _name;
    }

    protected Object readResolve() throws ObjectStreamException {
        return _nameMap.get(_name);
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public String objectToString(Object o) {
                return ((LineBreak)o).getName();
            }

            public Object stringToObject(String name) throws IllegalArgumentException {
                LineBreak lineBreak = LineBreak.fromName(name);
                if (lineBreak == null) throw new IllegalArgumentException("Unknown LineBreak: '" + name + "'");
                return lineBreak;
            }
        };
    }
}
