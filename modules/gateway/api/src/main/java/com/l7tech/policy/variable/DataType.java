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
    private static final long serialVersionUID = 8056552008434644172L;
    private static final Map nameMap = new HashMap();

    public static final DataType STRING = new DataType("string", "String", new Class[] { String.class });
    public static final DataType CERTIFICATE = new DataType("cert", "X.509 Certificate", new Class[] { X509Certificate.class });
    public static final DataType INTEGER = new DataType("int", "Integer", new Class[] { BigInteger.class });
    public static final DataType DECIMAL = new DataType("decimal", "Decimal Number", new Class[] { BigDecimal.class });
    public static final DataType FLOAT = new DataType("float", "Floating Point Number", new Class[] { Double.class });
    public static final DataType ELEMENT = new DataType("xml", "XML Element", new Class[] { Element.class });
    public static final DataType BOOLEAN = new DataType("boolean", "Boolean", new Class[] { Boolean.class, Boolean.TYPE });
    public static final DataType BINARY = new DataType("binary", "Binary", new Class[] { byte[].class, String.class });
    public static final DataType DATE_TIME = new DataType("dateTime", "Date/Time", new Class[] { Date.class, Calendar.class, Long.TYPE, Long.class });
    public static final DataType MESSAGE = new DataType("message", "Message", new Class[] { Object.class /* Message.class would have bring in lots of dependencies to Layer 7 API. */ });
    public static final DataType BLOB = new DataType("blob", "BLOB", new Class[] { InputStream.class });
    public static final DataType CLOB = new DataType("clob", "CLOB", new Class[] { Reader.class });
    public static final DataType UNKNOWN = new DataType("other", "Unknown/Other", new Class[] { Object.class });

    public static final DataType[] VALUES = new DataType[] { STRING, CERTIFICATE, INTEGER, DECIMAL, FLOAT, ELEMENT, BOOLEAN, BINARY, DATE_TIME, MESSAGE, BLOB, CLOB, UNKNOWN };
    public static final DataType[] GUI_EDITABLE_VALUES = new DataType[] { STRING, CERTIFICATE, INTEGER, DECIMAL, FLOAT, ELEMENT, BOOLEAN, BINARY, DATE_TIME, MESSAGE };

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

    /**
     * Look up a DataType by shortName.
     * @param shortName the shortName of a DataType.  May be null.
     * @return the DataType with the specified name, or null if not found.
     */
    public static DataType forName(String shortName) {
        Object type = shortName == null ? null : nameMap.get(shortName);
        return type instanceof DataType
            ? (DataType) type
            : DataType.UNKNOWN;
    }

    protected Object readResolve() throws ObjectStreamException {
        return nameMap.get(shortName);
    }
}
