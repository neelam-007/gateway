/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.logic;

import com.l7tech.common.util.ComparisonOperator;

/**
 * @author alex
*/
public class BinaryPredicate extends Predicate {
    private String rightValue;
    private ComparisonOperator operator = ComparisonOperator.EQ;
    private boolean caseSensitive;

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
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("!");
        sb.append(operator.getShortName()).append("(");
        sb.append("'").append(rightValue).append("'");
        if (caseSensitive) {
            sb.append(", case=true");
        }
        sb.append(")");
        return sb.toString();
    }

    public String getSimpleName() {
        return "binary";
    }
}
