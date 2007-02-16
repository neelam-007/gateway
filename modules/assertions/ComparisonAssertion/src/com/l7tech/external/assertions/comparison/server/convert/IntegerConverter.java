/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Attempts to convert any {@link Object} into a {@link java.math.BigInteger} by calling
 * {@link java.math.BigInteger#BigInteger(String)} on it, and failing that, {@link BigDecimal#BigDecimal(String)}
 * followed by {@link java.math.BigDecimal#toBigInteger()}.
 * @author alex
*/
public class IntegerConverter implements ValueConverter<BigInteger> {
    public BigInteger convert(Object val) throws ConversionException {
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
