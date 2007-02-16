/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

/**
 * @author alex
 */
public abstract class MinMaxPredicate extends Predicate {
    protected int min;
    protected int max;

    public MinMaxPredicate() { }

    public MinMaxPredicate(int min, int max, boolean negated) {
        this.min = min;
        this.max = max;
        this.negated = negated;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public void setMax(int max) {
        this.max = max;
    }
}
