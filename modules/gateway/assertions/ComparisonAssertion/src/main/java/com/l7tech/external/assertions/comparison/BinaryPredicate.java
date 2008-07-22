/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

import com.l7tech.util.ComparisonOperator;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Enumeration;

/**
 * A predicate that compares the left value using a {@link ComparisonOperator} against a {@link #rightValue}.
 * @author alex
 */
public class BinaryPredicate extends Predicate {
    private String rightValue;
    private ComparisonOperator operator = ComparisonOperator.EQ;
    private boolean caseSensitive = true;

    public BinaryPredicate() {
    }

    public BinaryPredicate(ComparisonOperator operator, String rightValue, boolean caseSensitive, boolean negate) {
        this.negated = negate;
        if (operator.isUnary() && rightValue != null) throw new IllegalArgumentException("Unary operators must not have an rvalue");
        this.operator = operator;
        this.rightValue = rightValue;
        this.caseSensitive = caseSensitive;
    }
    
    public String getRightValue() {
        return rightValue;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }

    public void setOperator(ComparisonOperator operator) {
        this.operator = operator;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String toString() {
        ResourceBundle res = ComparisonAssertion.resources;
        String rv = rightValue;
        final String prefix = "binaryPredicate." + operator.getShortName().toLowerCase();
        final String negKey = prefix + ".negatedDesc";
        final String nonNegKey = prefix + ".nonNegatedDesc";
        // Use the override construction if present
        final Enumeration<String> keys = res.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (negated && negKey.equals(key)) {
                return addCaseLabel(MessageFormat.format(res.getString(negKey), rightValue));
            } else if (!negated && nonNegKey.equals(key)) {
                return addCaseLabel(MessageFormat.format(res.getString(nonNegKey), rightValue));
            }
        }

        String fmt = res.getString(prefix + ".desc");
        String whichVerb = res.getString(prefix + ".verb");
        String verb = negated ? res.getString(whichVerb + "Not") : res.getString(whichVerb);
        String s = MessageFormat.format(fmt, verb, rv);
        return addCaseLabel(s);
    }

    private String addCaseLabel(String s) {
        if (caseSensitive) {
            String csfmt = ComparisonAssertion.resources.getString("binaryPredicate.caseSensitive");
            return MessageFormat.format(csfmt, s);
        } else {
            return s;
        }
    }

    public String getSimpleName() {
        return "binary";
    }
}
