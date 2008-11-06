/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementors are utility classes that attempt to convert any Object into a (possibly identical) value of type RT.
 * @param <RT> is the Result Type
 * @author alex
*/
public interface ValueConverter<RT> {
    /**
     * Converts a comparison value into the converter's target type.
     * 
     * @param val the value that needs to be converted.  Must not be null.
     * @return a converted version of the object. Never null.
     * @throws ConversionException if the value cannot be converted.
     */
    RT convert(Object val) throws ConversionException;

    public static class Factory {
        private static final Map<DataType, ValueConverter> typeMap = Collections.unmodifiableMap(new HashMap<DataType, ValueConverter>() {{
            put(DataType.INTEGER, new IntegerConverter());
            put(DataType.BOOLEAN, new BooleanConverter());
            put(DataType.DECIMAL, new DecimalConverter());
            put(DataType.FLOAT, new DoubleConverter());
            put(DataType.CERTIFICATE, new CertConverter());
            put(DataType.ELEMENT, new XmlConverter());
            put(DataType.STRING, new StringConverter());
            put(DataType.UNKNOWN, new ValueConverter() {
                public Object convert(Object val) throws ConversionException {
                    throw new ConversionException("Can't convert a value of unknown type");
                }
            }
            );
        }});

        public static ValueConverter getConverter(DataType type) {
            ValueConverter conv = typeMap.get(type);
            if (conv == null) throw new IllegalArgumentException("No converter registered for type " + type.getName());
            return conv;
        }

    }
}
