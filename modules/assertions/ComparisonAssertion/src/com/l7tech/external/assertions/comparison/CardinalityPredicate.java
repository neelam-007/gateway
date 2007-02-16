/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Tests that the number of values in the expression falls between {@link #min} and {@link #max}.
 *
 * Factory methods are provided for {@link #empty} (i.e. num == 0), {@link #notEmpty} (i.e. num > 0) and
 * {@link #exactlyOne} (i.e. num == 1).
 *
 * Set {@link #max} to a negative number to specify no maximum.
 */
public class CardinalityPredicate extends MinMaxPredicate {
    public CardinalityPredicate() {
    }

    public CardinalityPredicate(int min, int max, boolean negated) {
        super(min, max, negated);
        this.negated = negated;
        this.min = min;
        this.max = max;
    }

    public static CardinalityPredicate empty() {
        return new CardinalityPredicate(0, 0, false);
    }

    public static CardinalityPredicate notEmpty() {
        return new CardinalityPredicate(1, Integer.MAX_VALUE, false);
    }

    public static CardinalityPredicate exactlyOne() {
        return new CardinalityPredicate(1, 1, false);
    }

    public String toString() {
        ResourceBundle res = ComparisonAssertion.resources;
        String verb = negated ? res.getString("verb.hasNot") : res.getString("verb.has");
        if (min == 1 && max == 1) {
            return MessageFormat.format(res.getString("cardinalityPredicate.desc.1"), verb, min);
        } else if (min == max) {
            return MessageFormat.format(res.getString("cardinalityPredicate.desc.n"), verb, min);
        } else if (max < 0) {
            return MessageFormat.format(res.getString(min == 1 ? "cardinalityPredicate.desc.atLeast1" : "cardinalityPredicate.desc.atLeastN"), verb, min);
        } else {
            return MessageFormat.format(res.getString("cardinalityPredicate.desc"), verb, min, max);
        }
    }

    public String getSimpleName() {
        return "cardinality";
    }
}
