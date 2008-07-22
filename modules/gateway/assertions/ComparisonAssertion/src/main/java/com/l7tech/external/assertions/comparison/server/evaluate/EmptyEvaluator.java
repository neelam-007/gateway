/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.EmptyPredicate;

/**
 * Evaluator for {@link com.l7tech.external.assertions.comparison.EmptyPredicate}s.
 * @author alex
 */
public class EmptyEvaluator extends SingleValuedEvaluator<EmptyPredicate> {
    public EmptyEvaluator(EmptyPredicate predicate) {
        super(predicate);
    }

    public boolean evaluate(Object leftValue) {
        if (leftValue == null) return true;
        if ("".equals(leftValue)) return true;
        String s = predicate.isTrimWhitespace() ? leftValue.toString().trim() : leftValue.toString();
        return s.length() == 0;
    }
}
