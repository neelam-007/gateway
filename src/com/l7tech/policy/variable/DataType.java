/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.common.util.EnumTranslator;
import org.w3c.dom.Element;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
*/
public final class DataType implements Serializable {
    private static final Map nameMap = new HashMap();

    public static final DataType STRING = new DataType("string", "String", new Class[] { CharSequence.class, char[].class, Character.class, Character.TYPE });
    public static final DataType CERTIFICATE = new DataType("cert", "X.509 Certificate", new Class[] { X509Certificate.class });
    public static final DataType INTEGER = new DataType("int", "Integer",
            new Class[] { Long.TYPE, Long.class,
            Integer.TYPE, Integer.class,
            Byte.TYPE, Byte.class,
            Character.TYPE, Character.class,
            Short.TYPE, Short.class,
            BigInteger.class }
    );
    public static final DataType DECIMAL = new DataType("decimal", "Decimal Number", new Class[] { BigDecimal.class });
    public static final DataType FLOAT = new DataType("float", "Floating Point Number", new Class[] { BigDecimal.class, Float.TYPE, Float.class, Double.TYPE, Double.class});
    public static final DataType ELEMENT = new DataType("xml", "XML Element", new Class[] { Element.class });
    public static final DataType BOOLEAN = new DataType("boolean", "Boolean", new Class[] { Boolean.class, Boolean.TYPE });
    public static final DataType BINARY = new DataType("binary", "Binary", new Class[] { byte[].class, String.class });
    public static final DataType DATE_TIME = new DataType("dateTime", "Date/Time", new Class[] { Date.class, Calendar.class, Long.TYPE, Long.class });
    public static final DataType MESSAGE = new DataType("message", "Message", new Class[] { Object.class /* Message.class would have bring in lots of dependencies to Layer 7 API. */ });
    public static final DataType UNKNOWN = new DataType("other", "Unknown/Other", new Class[] { Object.class });

    public static final DataType[] VALUES = new DataType[] { STRING, CERTIFICATE, INTEGER, DECIMAL, FLOAT, ELEMENT, BOOLEAN, BINARY, DATE_TIME, MESSAGE, UNKNOWN };

    private final String shortName;
    private final String name;
    private final Class[] valueClasses;

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    public Class[] getValueClasses() {
        return valueClasses;
    }

    private DataType(String shortName, String name, Class[] classes) {
        this.shortName = shortName;
        this.name = name;
        this.valueClasses = classes;
        if (nameMap.put(shortName, this) != null) throw new IllegalArgumentException("Duplicate shortName: " + shortName);
    }

    public String toString() {
        return name;
    }

    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public Object stringToObject(String s) throws IllegalArgumentException {
                return nameMap.get(s);
            }

            public String objectToString(Object o) throws ClassCastException {
                return ((DataType)o).getShortName();
            }
        };
    }

    protected Object readResolve() throws ObjectStreamException {
        return nameMap.get(shortName);
    }
}
