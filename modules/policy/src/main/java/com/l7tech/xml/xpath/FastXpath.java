/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import com.l7tech.util.ComparisonOperator;

/**
 * Represents an Xpath expression that either selects a simple nodeset, or compares the count of selected
 * nodes to an integer value.
 */
public final class FastXpath {
    private final String expression;       // globally registered expression
    private final ComparisonOperator countComparison;  // for fastxpaths which compare count(TNS) to an int
    private final Integer countValue;                  // for fastxpaths which compare count(TNS) to an int

    /**
     * Create a FastXpath that just selects a nodeset using the specified expression.
     *
     * @param expression  a valid Tarari normal form XPath expression that selectes a nodeset.
     */
    public FastXpath(String expression) {
        if (expression == null) throw new IllegalArgumentException("Expression must be non-null");
        this.expression = expression;
        this.countComparison = null;
        this.countValue = null;
    }

    /**
     * Create a FastXpath that selects a nodeset and compares the count of the result to an integer.
     * For example, to represent the XPath 1.0 expression "<pre>0=count(/foo/bar)</pre>", use:
     * new FastXpath("/foo/bar", ComparisonOperator.EQ, new Integer(0))
     *
     * @param expression  a valid Tarari normal form XPath expression that selectes a nodeset.
     * @param countComparison  the comparison operator to use on the count of the result nodeset, or null to just use the nodeset as the result
     * @param countValue  the value with which to compare the nodeset count.  Must not be null if countComparison is non-null.
     */
    public FastXpath(String expression, ComparisonOperator countComparison, Integer countValue) {
        if (expression == null) throw new IllegalArgumentException("Expression must be non-null");
        this.expression = expression;
        this.countComparison = countComparison;
        this.countValue = countValue;
        if (countComparison != null && countValue == null) throw new IllegalArgumentException("countValue is required if countComparison is provided");
    }

    /** @return the Tarari normal form expression.  Never null. */
    public String getExpression() {
        return expression;
    }

    /** @return the comparison operator for comparing the count of the result nodeset, or null to just use the nodeset as the result. */
    public ComparisonOperator getCountComparison() {
        return countComparison;
    }

    /** @return the integer to compare the count with.  Never null if getCountComparison() is non-null. */
    public Integer getCountValue() {
        return countValue;
    }
}
