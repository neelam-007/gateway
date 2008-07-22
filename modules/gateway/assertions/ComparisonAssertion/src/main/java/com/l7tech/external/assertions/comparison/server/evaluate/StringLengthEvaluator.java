/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.MinMaxPredicate;
import com.l7tech.external.assertions.comparison.StringLengthPredicate;

/**
 * Evaluator for {@link StringLengthPredicate}s.
 * @author alex
 */
public class StringLengthEvaluator extends SingleValuedEvaluator<MinMaxPredicate> {
    public StringLengthEvaluator(StringLengthPredicate predicate) {
        super(predicate);
    }

    public boolean evaluate(Object leftValue) {
        String leftValue1 = leftValue.toString();
        int num = leftValue1 == null ? 0 : leftValue1.length();
        int min = predicate.getMin();
        int max = predicate.getMax();
        return num >= min && (max < 0 || num <= max);
    }
}
