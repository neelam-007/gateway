/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.common.logic.Predicate;

/**
 * Concrete implementations can evaluate a predicate against an array of values. 
 * @author alex
 */
public abstract class MultiValuedEvaluator<PT extends Predicate> extends Evaluator<PT> {
    public MultiValuedEvaluator(PT predicate) {
        super(predicate);
    }
    
    /**
     * @param vals the values to evaluate against the {@link Predicate}.
     * @return the truthiness of the predicate <em>irrespective of {@link Predicate#negated}</em>.  Any ${variable}
     *         references previously found in the argument should already have been resolved.
     */
    public abstract boolean evaluate(Object[] vals);
}
