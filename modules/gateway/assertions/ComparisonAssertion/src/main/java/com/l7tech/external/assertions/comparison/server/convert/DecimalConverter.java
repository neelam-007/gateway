/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import java.math.BigDecimal;

/**
 * Attempts to convert any {@link Object} into a {@link BigDecimal} (by calling {@link BigDecimal#BigDecimal(String)} on it)
 * @author alex
*/
public class DecimalConverter implements ValueConverter<BigDecimal> {
    public BigDecimal convert(Object val) throws ConversionException {
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            throw new ConversionException("Invalid decimal value", e);
        }
    }
}
