/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.logic;

/**
 * @author alex
 */
public class RegexPredicate extends Predicate {
    private String pattern;

    public RegexPredicate() {
    }

    public RegexPredicate(String pattern, boolean negated) {
        this.negated = negated;
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("!");
        sb.append("REGEX('").append(pattern).append("'");
        sb.append(")");
        return sb.toString();
    }

    public String getSimpleName() {
        return "regex";
    }
}
