package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;
import com.l7tech.util.Functions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
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

    /**
     * Get the data type for this converter.
     *
     * @return the DataType
     */
    DataType getDataType();

    public static class Factory {
        private static final Map<DataType, ValueConverter> typeMap = Collections.unmodifiableMap(new HashMap<DataType, ValueConverter>() {{
            put(DataType.INTEGER, new IntegerConverter());
            put(DataType.BOOLEAN, new BooleanConverter());
            put(DataType.DECIMAL, new DecimalConverter());
            put(DataType.FLOAT, new DoubleConverter());
            put(DataType.CERTIFICATE, new CertConverter());
            put(DataType.ELEMENT, new XmlConverter());
            put(DataType.STRING, new StringConverter());
            put(DataType.UNKNOWN, new ValueConverterSupport(DataType.UNKNOWN) {
                @Override
                public Object convert(Object val) throws ConversionException {
                    throw new ConversionException("Can't convert a value of unknown type");
                }
            }
            );
        }});

        public static ValueConverter getConverter(DataType type) {
            final ValueConverter converter = typeMap.get(type);
            if (converter == null) throw new IllegalArgumentException("No converter registered for type " + type.getName());
            return converter;
        }

        /**
         * Get the converter if one is available for the given target type.
         *
         * @param target An example of the target type
         * @return the value or null
         */
        public static ValueConverter getConverter( final Comparable target ) {
            final DataType type = getDataType( target );
            return type == null ? helperConverter( target ) : getConverter( type );
        }

        /**
         * Get any additional helper converters for common types.
         */
        private static ValueConverter helperConverter( final Comparable target ) {
            ValueConverter converter = null;

            if ( target instanceof Long ) {
                return new SimpleNumericValueConverter<Long>( new Functions.Unary<Long,String>(){
                    @Override
                    public Long call( final String value ) {
                        return Long.parseLong( value );
                    }
                } );
            } else if ( target instanceof Integer ) {
                return new SimpleNumericValueConverter<Integer>( new Functions.Unary<Integer,String>(){
                    @Override
                    public Integer call( final String value ) {
                        return Integer.parseInt( value );
                    }
                } );
            }

            return converter;
        }

        /**
         * Get the DataType to use for the given object.
         *
         * @param value The value
         * @return The applicable data type (null if no type is applicable)
         */
        private static DataType getDataType( final Comparable value ) {
            final DataType dataType;

            if ( value instanceof String ) {
                dataType = DataType.STRING;
            } else if ( value instanceof BigInteger ) {
                dataType = DataType.INTEGER;
            } else if ( value instanceof BigDecimal ) {
                dataType = DataType.DECIMAL;
            } else if ( value instanceof Boolean ) {
                dataType = DataType.BOOLEAN;
            } else if ( value instanceof Double ) {
                dataType = DataType.FLOAT;
            } else if ( value instanceof Date) {
                dataType = DataType.DATE_TIME;
            } else {
                dataType = null;
            }

            return dataType;
        }

        private static class SimpleNumericValueConverter<RT> extends ValueConverterSupport<RT> {
            private final Functions.Unary<RT,String> converterCallback;

            private SimpleNumericValueConverter( final Functions.Unary<RT,String> converterCallback ) {
                super( DataType.INTEGER );
                this.converterCallback = converterCallback;
            }

            @Override
            public RT convert( final Object value ) throws ConversionException {
                try {
                    return converterCallback.call( value.toString() );
                } catch ( NumberFormatException nfe ) {
                    throw new ConversionException( "Invalid integer value",  nfe );
                }
            }
        }
    }
}
