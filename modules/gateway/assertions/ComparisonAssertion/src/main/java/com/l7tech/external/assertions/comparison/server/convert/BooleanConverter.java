/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

/**
 * Attempts to convert any {@link Object} into a {@link Boolean} by calling {@link Boolean#toString()} on it.
 *
 * Effectively converts the string &quot;true&quot; into {@link Boolean#TRUE} and all other values into
 * {@link Boolean#FALSE}.
 *  
 * @author alex
*/
public class BooleanConverter implements ValueConverter<Boolean> {
    public Boolean convert(Object val) throws ConversionException {
        return Boolean.valueOf(val.toString());
    }
}
