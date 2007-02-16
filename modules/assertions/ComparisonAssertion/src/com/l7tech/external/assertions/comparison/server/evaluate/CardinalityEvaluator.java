/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.CardinalityPredicate;

/**
 * Evaluator for {@link CardinalityPredicate}s.
 * @author alex
 */
public class CardinalityEvaluator extends MultiValuedEvaluator<CardinalityPredicate> {
    public CardinalityEvaluator(CardinalityPredicate predicate) {
        super(predicate);
    }

    public boolean evaluate(Object[] leftValue) {
        int num = leftValue == null ? 0 : leftValue.length;
        int min = predicate.getMin();
        int max = predicate.getMax();
        return num >= min && (max < 0 || num <= max);
    }
}
