package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;

/**
 * Attempts to convert any {@link Object} into a {@link Boolean} by calling {@link Boolean#toString()} on it.
 *
 * Effectively converts the string &quot;true&quot; into {@link Boolean#TRUE} and all other values into
 * {@link Boolean#FALSE}.
 *  
 * @author alex
*/
public class BooleanConverter extends ValueConverterSupport<Boolean> {
    
    public BooleanConverter() {
        super( DataType.BOOLEAN );
    }

    @Override
    public Boolean convert( final Object val ) throws ConversionException {
        return Boolean.valueOf(val.toString());
    }
}
