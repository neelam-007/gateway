/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

/**
 * Assertion that can limit length of attribute and text nodes.
 */
public class OversizedTextAssertion extends Assertion {
    private static final long DEFAULT_ATTR_LIMIT = 2048;
    private static final long DEFAULT_TEXT_LIMIT = 16384;

    private static final String XPATH_TEXT_START = "(//*/text())[string-length() > ";
    private static final String XPATH_TEXT_END = "]";

    private static final String XPATH_ATTR_START = "(//@*)[string-length() > ";
    private static final String XPATH_ATTR_END = "]";

    private long maxTextChars = DEFAULT_TEXT_LIMIT;
    private long maxAttrChars = DEFAULT_ATTR_LIMIT;

    public long getMaxTextChars() {
        return maxTextChars;
    }

    public void setMaxTextChars(long maxTextChars) {
        this.maxTextChars = maxTextChars;
    }

    public long getMaxAttrChars() {
        return maxAttrChars;
    }

    public void setMaxAttrChars(long maxAttrChars) {
        this.maxAttrChars = maxAttrChars;
    }

    /** @return an xpath that enforces the configured limit on all text nodes. */
    public String makeTextXpath() {
        return XPATH_TEXT_START + maxTextChars + XPATH_TEXT_END;
    }

    /** @return an xpath that enforces the configured limit on all attribute values. */
    public String makeAttrXpath() {
        return XPATH_ATTR_START + maxAttrChars + XPATH_ATTR_END;
    }
}
