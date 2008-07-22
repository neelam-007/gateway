/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import org.w3c.dom.Element;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

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
    public static final DataType BLOB = new DataType("blob", "BLOB", new Class[] { InputStream.class });
    public static final DataType CLOB = new DataType("clob", "CLOB", new Class[] { Reader.class });
    public static final DataType UNKNOWN = new DataType("other", "Unknown/Other", new Class[] { Object.class });

    public static final DataType[] VALUES = new DataType[] { STRING, CERTIFICATE, INTEGER, DECIMAL, FLOAT, ELEMENT, BOOLEAN, BINARY, DATE_TIME, MESSAGE, BLOB, CLOB, UNKNOWN };

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

    /**
     * Deprecated, but do not remove.
     *
     * @return null
     * @deprecated
     */
    public static Object getEnumTranslator() {
        InvocationHandler handler = new InvocationHandler(){
            public Object invoke( final Object proxy, final Method method, final Object[] args ) throws Throwable {
                if ( "stringToObject".equals( method.getName() ) && args.length==1 ) {
                    return nameMap.get( args[0] );
                } else if ( "objectToString".equals( method.getName() ) && args.length==1 ) {
                    return ((DataType) args[0]).getShortName();
                } else {
                    throw new UnsupportedOperationException( method.getName() );
                }
            }
        };

        try {
            return Proxy.newProxyInstance (
                DataType.class.getClassLoader(),
                new Class[] { Class.forName( "com.l7tech.util.EnumTranslator" )},
                handler );
        } catch ( ClassNotFoundException cnfe ) {
            return null; // class will only be present on SSB/SSG
        }
    }

    protected Object readResolve() throws ObjectStreamException {
        return nameMap.get(shortName);
    }
}
