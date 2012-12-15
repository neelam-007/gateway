package com.l7tech.console.policy;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;

/**
 * Property editor for BigDecimal fields.
 */
public class BigDecimalPropertyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(new BigDecimal(text));
    }
}
