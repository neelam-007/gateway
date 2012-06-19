package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;
import com.l7tech.util.DateTimeConfigUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
            put(DataType.DATE_TIME, new ValueConverterSupport<Date>(DataType.DATE_TIME) {
                @Override
                public Date convert(Object val) throws ConversionException {
                    try {
                        if (val instanceof Date) {
                            return (Date) val;
                        } else if (val instanceof Calendar) {
                            return new Date(((Calendar) val).getTimeInMillis());
                        } else {
                            // allow Long to be converted to a string, whether the long is a valid timestamp or not will be determined
                            return dateParserRef.get().parseDateFromString(val.toString());
                        }
                    } catch (ParseException e) {
                        throw new ConversionException("Cannot convert to " + DataType.DATE_TIME.getShortName(), e);
                    } catch (DateTimeConfigUtils.UnknownDateFormatException e) {
                        throw new ConversionException("Cannot convert to " + DataType.DATE_TIME.getShortName(), e);
                    } catch (DateTimeConfigUtils.InvalidDateFormatException e) {
                        throw new ConversionException("Cannot convert to " + DataType.DATE_TIME.getShortName(), e);
                    }
                }
            });
            put(DataType.UNKNOWN, new ValueConverterSupport(DataType.UNKNOWN) {
                @Override
                public Object convert(Object val) throws ConversionException {
                    throw new ConversionException("Can't convert a value of unknown type");
                }
            }
            );
        }});

        /**
         * Get the converter for the specified type. This converter will always convert to the default 'primary'
         * internal representation for this DataType.
         *
         * @param type data type
         * @return converter, never null. Every DataType must have a converter.
         */
        @NotNull
        public static ValueConverter getConverter(DataType type) {
            return getConverter(type, null);
        }

        @NotNull
        public static ValueConverter getConverter(DataType type, @Nullable Comparable requiredComparableClass) {
            final ValueConverter converter;
            if (requiredComparableClass == null) {
                converter = typeMap.get(type);
            } else {
                converter = getConverterOrHelperConverter(requiredComparableClass, type);
            }

            if (converter == null)
                throw new IllegalArgumentException("No converter registered for type " + type.getName());
            return converter;
        }

        /**
         * Get the converter if one is available for the given target type.
         *
         * @param target An example of the target type
         * @param forceType If not null, then the converter found must be capable of converting objects of the target type
         * from variables of this data type.
         * @return the value or null
         */
        @Nullable
        public static ValueConverter getConverterOrHelperConverter(final Comparable target, @Nullable DataType forceType) {
            final DataType type = getDataType( target );
            final ValueConverter converter = getHelperConverter(target, forceType);
            return converter != null ? converter : (type != null) ? getConverter(type) : null;
        }

        public static void setDateParser(DateTimeConfigUtils dateParser) {
            Factory.dateParserRef.set(dateParser);
        }

        // - PRIVATE

        private final static AtomicReference<DateTimeConfigUtils> dateParserRef = new AtomicReference<DateTimeConfigUtils>();

        /**
         * Get any additional helper converters for common types.
         *
         * Converters for types which are not listed as a type in DataType
         *
         * @param target the Comparable which is either not a primary valueClass for a DataType but is supported
         * for comparisons or is a valueClass for a DataType, but the DataType has more than one, adding the possible
         * requirement to be able to compare those types if they appear at runtime.
         * @param requiredType Used to distinguish between ambiguous conversion like Long, which is used for both
         * Integer types and Date/Time types. Null when no default behaviour required.
         * @return the converter. Null when no converter for the target Comparable's type exists.
         */
        private static ValueConverter getHelperConverter(final Comparable target, @Nullable final DataType requiredType) {
            ValueConverter converter = null;

            if ( target instanceof Long && (requiredType == null || requiredType == DataType.INTEGER)) {
                return new SimpleNumericValueConverter<Long>( new Functions.Unary<Long,String>(){
                    @Override
                    public Long call( final String value ) {
                        return Long.parseLong(value);
                    }
                } );
            } else if ( target instanceof Integer ) {
                return new SimpleNumericValueConverter<Integer>( new Functions.Unary<Integer,String>(){
                    @Override
                    public Integer call( final String value ) {
                        return Integer.parseInt( value );
                    }
                } );
            } else if (target instanceof Calendar) {
                return new ValueConverterSupport<Calendar>(DataType.DATE_TIME) {
                    @Override
                    public Calendar convert(Object val) throws ConversionException {
                        if (val instanceof Calendar) {
                            return (Calendar) val;
                        } else if (val instanceof Date) {
                            final Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(((Date)val).getTime());
                            return cal;
                        } else if (val instanceof Long || val instanceof String) {
                            try {
                                final Date dateTime = dateParserRef.get().parseDateFromString(val.toString());
                                final Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(dateTime.getTime());
                                return cal;
                            } catch (Exception e) {
                                //allow fall through
                            }
                        }
                        throw new ConversionException("Cannot convert type " + val.getClass().getName() + " into a Calendar");
                    }
                };
            } else if (target instanceof Long && requiredType == DataType.DATE_TIME) {
                return new ValueConverterSupport<Long>(DataType.DATE_TIME) {
                    @Override
                    public Long convert(Object val) throws ConversionException {
                        if (val instanceof Date) {
                            return ((Date) val).getTime();
                        } else if (val instanceof Calendar) {
                            final Calendar cal = (Calendar)val;
                            return cal.getTimeInMillis();
                        } else if (val instanceof String) {
                            try {
                                return dateParserRef.get().parseDateFromString(val.toString()).getTime();
                            } catch (Exception e) {
                                //allow fall through
                            }
                        }
                        throw new ConversionException("Cannot convert type " + val.getClass().getName() + " into a Long");
                    }
                };
            }

            return converter;
        }

        /**
         * Get the DataType to use for the given object.
         *
         * Supports all types explicitly listed in the valueClasses property for a DataType.
         *
         * WARNING: Do not list any class other than the 'primary' type used to store a DataType internally.
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
            } else if (value instanceof Date) {
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
