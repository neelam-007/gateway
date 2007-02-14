/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.common.logic.StringLengthPredicate;

import java.util.Collection;

/**
 * Evaluator for {@link StringLengthPredicate}s.
 * @author alex
 */
public class StringLengthEvaluator extends SingleValuedEvaluator<StringLengthPredicate> {
    public StringLengthEvaluator(StringLengthPredicate predicate) {
        super(predicate);
    }

    public boolean evaluate(Object leftValue) {
        if (leftValue instanceof Collection) {
            Collection coll = (Collection)leftValue;
            for (Object member : coll) {
                if (!lengthWithinBounds(member)) return false;
            }
            return true;
        } else if (leftValue.getClass().isArray()) {
            Object[] array = (Object[])leftValue;
            for (Object member : array) {
                if (!lengthWithinBounds(member)) return false;
            }
            return true;
        } else {
            return lengthWithinBounds(leftValue);
        }
    }

    private boolean lengthWithinBounds(Object obj) {
        String s = obj.toString();
        return !(s.length() < predicate.getMinLength() || s.length() > predicate.getMaxLength());
    }
}
