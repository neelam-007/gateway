/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

/**
 * @author alex
 */
public class NumericRangePredicate<N extends Number> extends Predicate {
    private N minValue;
    private N maxValue;

    public NumericRangePredicate() {
    }

    public NumericRangePredicate(N min, N max) {
        this.minValue = min;
        this.maxValue = max;
    }

    public N getMinValue() {
        return minValue;
    }

    public void setMinValue(N minValue) {
        this.minValue = minValue;
    }

    public N getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(N maxValue) {
        this.maxValue = maxValue;
    }

    public String getSimpleName() {
        return "numericRange";
    }
}
