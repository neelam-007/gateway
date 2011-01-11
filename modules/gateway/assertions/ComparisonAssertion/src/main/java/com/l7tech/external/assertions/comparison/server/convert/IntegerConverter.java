package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Attempts to convert any {@link Object} into a {@link java.math.BigInteger} by calling
 * {@link java.math.BigInteger#BigInteger(String)} on it, and failing that, {@link BigDecimal#BigDecimal(String)}
 * followed by {@link java.math.BigDecimal#toBigInteger()}.
 * @author alex
*/
public class IntegerConverter extends ValueConverterSupport<BigInteger> {

    public IntegerConverter() {
        super( DataType.INTEGER );
    }

    @Override
    public BigInteger convert(Object val) throws ConversionException {
        //
        if ( val instanceof Long ||
             val instanceof Integer ||
             val instanceof Short ||
             val instanceof Byte ) {
            return BigInteger.valueOf( ((Number)val).longValue() );   
        } else {
            try {
                // Try simple conversion first
                return new BigInteger(val.toString());
            } catch (NumberFormatException e) {
                try {
                    // Not an integer, try to truncate it
                    return new BigDecimal(val.toString()).toBigInteger();
                } catch (Exception e1) {
                    throw new ConversionException("Invalid integer value", e1);
                }
            }
        }
    }
}
