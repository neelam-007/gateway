/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import java.io.UnsupportedEncodingException;

/**
 * Converts the specified value into a String (by calling {@link String#toString()} on it as a last resort).
 * @author alex
*/
public class StringConverter implements ValueConverter<String> {
    public String convert(Object val) throws ConversionException {
        if (val instanceof Character) {
            Character ch = (Character) val;
            return Character.toString(ch.charValue());
        } else if (val instanceof char[]) {
            return new String((char[])val);
        } else if (val instanceof byte[]) {
            try {
                return new String((byte[])val, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new ConversionException("Couldn't decode byte array", e);
            }
        }
        return val.toString();
    }
}
