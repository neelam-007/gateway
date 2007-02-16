/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 * @author alex
 */
public class StringLengthPredicate extends MinMaxPredicate {
    public StringLengthPredicate() {
    }

    public StringLengthPredicate(int min, int max, boolean negated) {
        this.negated = negated;
        this.min = min;
        this.max = max;
    }

    public String toString() {
        ResourceBundle res = ComparisonAssertion.resources;
        String verb = negated ? res.getString("verb.hasNot") : res.getString("verb.has");
        if (min == 1 && max == 1) {
            return MessageFormat.format(res.getString("stringLengthPredicate.desc.1"), verb, min);
        } else if (min == max) {
            return MessageFormat.format(res.getString("stringLengthPredicate.desc.n"), verb, min);
        } else if (max < 0) {
            return MessageFormat.format(res.getString(min == 1 ? "stringLengthPredicate.desc.atLeast1" : "stringLengthPredicate.desc.atLeastN"), verb, min);
        } else {
            return MessageFormat.format(res.getString("stringLengthPredicate.desc"), verb, min, max);
        }
    }

    public String getSimpleName() {
        return "stringLength";
    }
}
