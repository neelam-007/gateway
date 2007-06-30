/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.Predicate;

/**
 * Concrete implementations can evaluate a predicate against a single value at a time.
 */
public abstract class SingleValuedEvaluator<PT extends Predicate> extends Evaluator<PT> {
    protected SingleValuedEvaluator(PT predicate) {
        super(predicate);
    }

    /**
     * @param leftValue the value to evaluate against the {@link com.l7tech.external.assertions.comparison.Predicate}.
     * @return the truthiness of the predicate <em>irrespective of {@link com.l7tech.external.assertions.comparison.Predicate#isNegated()}</em>.  Any ${variable}
     *         references previously found in the argument should already have been resolved.
     */
    public abstract boolean evaluate(Object leftValue);
}
