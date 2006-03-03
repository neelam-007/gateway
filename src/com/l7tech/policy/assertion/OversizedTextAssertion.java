/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

/**
 * Assertion that can limit length of attribute and text nodes.
 */
public class OversizedTextAssertion extends Assertion {
    public static final long DEFAULT_ATTR_LIMIT = 2048;
    public static final long DEFAULT_TEXT_LIMIT = 16384;
    public static final int DEFAULT_NESTING_LIMIT = 32;
    public static final int MIN_NESTING_LIMIT = 2;         // Constrain to prevent useless check
    public static final int MAX_NESTING_LIMIT = 10000;     // Constrain to prevent enormous value

    private static final String XPATH_TEXT_START = "count((//*/text())[string-length() > ";
    private static final String XPATH_TEXT_END = "]) < 1";

    private static final String XPATH_ATTR_START = "count(//*/@*[string-length() > ";
    private static final String XPATH_ATTR_END = "]) < 1";

    private static final String XPATH_NESTING_STEP = "/*";

    private boolean limitTextChars = true;
    private long maxTextChars = DEFAULT_TEXT_LIMIT;
    private boolean limitAttrChars = true;
    private long maxAttrChars = DEFAULT_ATTR_LIMIT;
    private boolean limitNestingDepth = true;
    private int maxNestingDepth = DEFAULT_NESTING_LIMIT;

    public boolean isLimitTextChars() {
        return limitTextChars;
    }

    public void setLimitTextChars(boolean limitTextChars) {
        this.limitTextChars = limitTextChars;
    }

    public long getMaxTextChars() {
        return maxTextChars;
    }

    public void setMaxTextChars(long maxTextChars) {
        this.maxTextChars = maxTextChars;
    }

    public boolean isLimitAttrChars() {
        return limitAttrChars;
    }

    public void setLimitAttrChars(boolean limitAttrChars) {
        this.limitAttrChars = limitAttrChars;
    }

    public long getMaxAttrChars() {
        return maxAttrChars;
    }

    public void setMaxAttrChars(long maxAttrChars) {
        if (maxAttrChars < 0) maxAttrChars = 0;
        this.maxAttrChars = maxAttrChars;
    }

    public boolean isLimitNestingDepth() {
        return limitNestingDepth;
    }

    public void setLimitNestingDepth(boolean limitNestingDepth) {
        this.limitNestingDepth = limitNestingDepth;
    }

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int maxNestingDepth) {
        if (maxNestingDepth < 0) maxNestingDepth = 0;
        this.maxNestingDepth = maxNestingDepth;
    }

    /**
     * @return an XPath 1.0 expression that evaluates to true if no text node exceeds the configured limit, or null if
     *         limitTextChars is false.
     */
    public String makeTextXpath() {
        return isLimitTextChars() ? XPATH_TEXT_START + maxTextChars + XPATH_TEXT_END : null;
    }

    /**
     * @return an XPath 1.0 expression that evaluates to true if no attribute value exceeds the configured limit, or null
     *         if limitAttrChars is false.
     */
    public String makeAttrXpath() {
        return isLimitAttrChars() ? XPATH_ATTR_START + maxAttrChars + XPATH_ATTR_END : null;
    }

    /**
     * @return a parallelizable Tarari normal form XPath that matches the first node whose nesting depth exceeds
     *         the configured limit, or null if limitNestingDepth is false.
     */
    public String makeNestingXpath() {
        if (!isLimitNestingDepth()) return null;
        int depth = getMaxNestingDepth();

        // last-ditch sanity checks
        if (depth < MIN_NESTING_LIMIT) depth = MIN_NESTING_LIMIT;
        if (depth > MAX_NESTING_LIMIT) depth = MAX_NESTING_LIMIT;

        // Allow depth, but disallow the depth+1'th nested element.
        depth++;

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < depth; ++i) {
            sb.append(XPATH_NESTING_STEP);
        }

        return sb.toString();
    }
}
