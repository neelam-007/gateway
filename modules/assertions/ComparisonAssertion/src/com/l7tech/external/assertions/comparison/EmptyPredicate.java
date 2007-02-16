/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

/**
 * @author alex
 */
public class EmptyPredicate extends Predicate {
    private boolean trimWhitespace;

    public EmptyPredicate(boolean trimWhitespace, boolean negated) {
        this.negated = negated;
        this.trimWhitespace = trimWhitespace;
    }

    public EmptyPredicate() {
    }

    public boolean isTrimWhitespace() {
        return trimWhitespace;
    }

    public void setTrimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("!");
        sb.append("EMPTY(");
        if (trimWhitespace) sb.append("trim=true");
        sb.append(")");
        return sb.toString();
    }

    public String getSimpleName() {
        return "empty";
    }
}
