package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;
import com.l7tech.util.Charsets;

/**
 * Converts the specified value into a String (by calling {@link String#toString()} on it as a last resort).
 * @author alex
*/
public class StringConverter extends ValueConverterSupport<String> {

    public StringConverter() {
        super( DataType.STRING );
    }

    @Override
    public String convert( final Object val ) throws ConversionException {
        if (val instanceof Character) {
            Character ch = (Character) val;
            return Character.toString(ch);
        } else if (val instanceof char[]) {
            return new String((char[])val);
        } else if (val instanceof byte[]) {
            return new String((byte[])val, Charsets.UTF8);
        }
        return val.toString();
    }
}
