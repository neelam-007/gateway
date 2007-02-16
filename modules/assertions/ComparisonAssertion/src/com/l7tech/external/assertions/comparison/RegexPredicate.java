/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

import java.text.MessageFormat;

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
        String fmt = ComparisonAssertion.resources.getString(negated ? "regexPredicate.negatedDesc" : "regexPredicate.desc");
        return MessageFormat.format(fmt, pattern);
    }

    public String getSimpleName() {
        return "regex";
    }
}
