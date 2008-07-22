/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.NumericRangePredicate;

import java.math.BigDecimal;

/**
 * @author alex
 */
public class NumericRangeEvaluator extends SingleValuedEvaluator<NumericRangePredicate> {
    public NumericRangeEvaluator(NumericRangePredicate predicate) {
        super(predicate);
        bdmin = new BigDecimal(predicate.getMinValue().toString());
        bdmax = new BigDecimal(predicate.getMinValue().toString());
    }

    private final BigDecimal bdmin;
    private final BigDecimal bdmax;

    public boolean evaluate(Object leftValue) {
        if (leftValue instanceof Comparable && leftValue instanceof Number) {
            Comparable that = (Comparable) leftValue;
            if (that.equals(predicate.getMinValue()) || that.equals(predicate.getMaxValue())) return true;
            if (predicate.getMinValue().getClass().isAssignableFrom(that.getClass())) {
                if (that.compareTo(predicate.getMaxValue()) < 0) return true;
                if (that.compareTo(predicate.getMinValue()) > 0) return true;
            } else {
                BigDecimal bdthat = new BigDecimal(that.toString());
                if (bdthat.equals(bdmin) || bdthat.equals(bdmax)) return true;
                if (bdthat.compareTo(bdmax) < 0) return true;
                if (bdthat.compareTo(bdmin) > 0) return true;
            }
        }
        return false;
    }
}
