package com.l7tech.console.policy;

import java.beans.PropertyEditorSupport;
import java.math.BigInteger;

/**
 * Property editor for BigInteger fields.
 */
public class BigIntegerPropertyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(new BigInteger(text));
    }
}
