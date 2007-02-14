/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.logic;

/**
 * Tests that the number of values in the expression falls between {@link #minValues} and {@link #maxValues}.
 *
 * Factory methods are provided for {@link #empty} (i.e. num == 0), {@link #notEmpty} (i.e. num > 0) and
 * {@link #exactlyOne} (i.e. num == 1).
 */
public class CardinalityPredicate extends Predicate {
    private int minValues;
    private int maxValues;

    public CardinalityPredicate() {
    }

    public CardinalityPredicate(int min, int max, boolean negated) {
        this.negated = negated;
        this.minValues = min;
        this.maxValues = max;
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

    public int getMinValues() {
        return minValues;
    }

    public int getMaxValues() {
        return maxValues;
    }

    public void setMinValues(int minValues) {
        this.minValues = minValues;
    }

    public void setMaxValues(int maxValues) {
        this.maxValues = maxValues;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("!");
        sb.append("VALUES(");
        sb.append(minValues).append(",");
        sb.append(maxValues).append(")");
        return sb.toString();
    }

    public String getSimpleName() {
        return "cardinality";
    }
}
