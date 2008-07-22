/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;

import java.util.Map;
import java.util.HashMap;

/**
 * Implementors are utility classes that attempt to convert any Object into a (possibly identical) value of type RT.
 * @param <RT> is the Result Type
 * @author alex
*/
public interface ValueConverter<RT> {
    RT convert(Object val) throws ConversionException;

    public static class Factory {
        private static final UnknownConverter UC = new UnknownConverter();

        private static final Map<DataType, ValueConverter> typeMap = new HashMap<DataType, ValueConverter>();

        static {
            typeMap.put(DataType.INTEGER, new IntegerConverter());
            typeMap.put(DataType.BOOLEAN, new BooleanConverter());
            typeMap.put(DataType.DECIMAL, new DecimalConverter());
            typeMap.put(DataType.FLOAT, new DoubleConverter());
            typeMap.put(DataType.CERTIFICATE, new CertConverter());
            typeMap.put(DataType.ELEMENT, new XmlConverter());
            typeMap.put(DataType.STRING, new StringConverter());
            typeMap.put(DataType.UNKNOWN, UC);
        }

        public static ValueConverter getConverter(DataType type) {
            ValueConverter conv = typeMap.get(type);
            if (conv == null) throw new IllegalArgumentException("No converter registered for type " + type.getName());
            return conv;
        }

        private static class UnknownConverter implements ValueConverter {
            public Object convert(Object val) throws ConversionException {
                throw new ConversionException("Can't convert a value of unknown type");
            }
        }
    }
}
