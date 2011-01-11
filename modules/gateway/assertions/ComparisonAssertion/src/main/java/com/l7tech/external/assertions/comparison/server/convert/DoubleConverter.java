package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;

/**
 * @author alex
 */
public class DoubleConverter extends ValueConverterSupport<Double> {

    public DoubleConverter() {
        super( DataType.FLOAT );
    }

    @Override
    public Double convert( final Object val ) throws ConversionException {
        try {
            return Double.valueOf(val.toString());
        } catch (NumberFormatException nfe) {
            throw new ConversionException("Invalid floating-point value", nfe);
        }
    }
}
