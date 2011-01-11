package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;

import java.math.BigDecimal;

/**
 * Attempts to convert any {@link Object} into a {@link BigDecimal} (by calling {@link BigDecimal#BigDecimal(String)} on it)
 * @author alex
*/
public class DecimalConverter extends ValueConverterSupport<BigDecimal> {

    public DecimalConverter() {
        super( DataType.DECIMAL );
    }

    @Override
    public BigDecimal convert( final Object val ) throws ConversionException {
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            throw new ConversionException("Invalid decimal value", e);
        }
    }
}
