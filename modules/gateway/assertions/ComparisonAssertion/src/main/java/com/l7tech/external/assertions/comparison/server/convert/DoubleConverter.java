/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

/**
 * @author alex
 */
public class DoubleConverter implements ValueConverter<Double> {
    public Double convert(Object val) throws ConversionException {
        try {
            return Double.valueOf(val.toString());
        } catch (NumberFormatException nfe) {
            throw new ConversionException("Invalid floating-point value", nfe);
        }
    }
}
