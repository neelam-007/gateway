/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.logic;

/**
 * @author alex
 */
public class StringLengthPredicate extends Predicate {
    private int minLength;
    private int maxLength;

    public StringLengthPredicate() {
    }

    public StringLengthPredicate(int minLength, int maxLength, boolean negated) {
        this.negated = negated;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("!");
        sb.append("LENGTH(");
        sb.append(minLength).append(",");
        sb.append(maxLength).append(")");
        return sb.toString();
    }

    public String getSimpleName() {
        return "stringLength";
    }
}
