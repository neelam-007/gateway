/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import java.math.BigInteger;

/**
 * Attempts to convert any {@link Object} into a {@link java.math.BigInteger} (by calling
 * {@link java.math.BigInteger#BigInteger(String)} on it)
 * @author alex
*/
public class IntegerConverter implements ValueConverter<BigInteger> {
    public BigInteger convert(Object val) throws ConversionException {
        try {
            return new BigInteger(val.toString());
        } catch (NumberFormatException e) {
            throw new ConversionException("Invalid integer value", e);
        }
    }
}
