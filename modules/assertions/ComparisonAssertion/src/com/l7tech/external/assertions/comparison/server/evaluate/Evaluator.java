/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.Predicate;

/**
 * Each concrete subclass evaluates a particular type of {@link Predicate} against a value.
 * @param <PT> is the {@link Predicate} bean class that this evaluator works for
 * @author alex
 */
public abstract class Evaluator<PT extends Predicate> {
    protected final PT predicate;

    public Evaluator(PT predicate) {
        this.predicate = predicate;
    }
}
