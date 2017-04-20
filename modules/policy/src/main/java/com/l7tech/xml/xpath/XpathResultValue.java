package com.l7tech.xml.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;

/**
 * Represents a value in a set of xpath results.
 */
public class XpathResultValue {

    private XdmAtomicValue value;

    public void setValue(Object value) {
        this.value = (XdmAtomicValue)value;
    }

    /** @return the value of this result as a String, or null if it is not . */
    public String getString() {
        return value.getStringValue();
    }

    /** @return the value of this result as a boolean, or false if it is not. */
    public boolean getBoolean() {
        try {
            return value.getBooleanValue();
        } catch (SaxonApiException e) {
            return false;
        }
    }

    /** @return the value of this result as a double, or 0 if it is not. */
    public double getNumber() {
        try {
            return value.getDoubleValue();
        } catch (SaxonApiException e) {
            return 0;
        }
    }
}
