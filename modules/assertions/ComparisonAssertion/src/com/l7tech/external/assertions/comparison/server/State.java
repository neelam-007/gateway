/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import com.l7tech.external.assertions.comparison.DataTypePredicate;
import com.l7tech.external.assertions.comparison.Predicate;
import com.l7tech.external.assertions.comparison.server.convert.ConversionException;
import com.l7tech.external.assertions.comparison.server.convert.ValueConverter;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;
import com.l7tech.policy.variable.DataType;

import java.util.Collection;
import java.util.Map;

/**
 * Holds the state kept while evaluating a single request through the {@link ServerComparisonAssertion}.
 * @author alex
*/
abstract class State {
    protected final Map<Predicate, Evaluator> evaluators;
    protected final Auditor auditor;
    protected final Map<String, Object> vars;

    /**
     * The {@link DataType} of any {@link DataTypePredicate} that has been observed so far in the evaluation.
     * Note that this basically requires that any {@link DataTypePredicate} must come first in the
     * {@link ComparisonAssertion#getPredicates()} array. 
     */
    protected DataType type;

    /**
     * True iff all predicates processed thus far have evaluated to true. 
     */
    protected boolean assertionResult = true;

    public boolean getAssertionResult() {
        return assertionResult;
    }

    protected State(Map<Predicate, Evaluator> evaluators, Map<String, Object> vars, Auditor auditor) {
        this.evaluators = evaluators;
        this.vars = vars;
        this.auditor = auditor;
    }

    protected abstract void evaluate(Predicate pred);

    protected static State make(Map<Predicate, Evaluator> evaluators, Object left, Map<String, Object> vars, Auditor auditor) {
        Class leftClass = left.getClass();
        if (leftClass.isArray()) {
            return new MVState(evaluators, (Object[])left, vars, auditor);
        } else if (Collection.class.isAssignableFrom(leftClass)) {
            return new MVState(evaluators, ((Collection)left).toArray(new Object[0]), vars, auditor);
        } else {
            return new SVState(evaluators, left, vars, auditor);
        }
    }

    protected Object convertValue(Object value, DataType type) {
        for (Class clazz : type.getValueClasses()) {
            if (clazz.isAssignableFrom(value.getClass())) {
                // no conversion required
                return value;
            }
        }

        auditor.logAndAudit(AssertionMessages.COMPARISON_CONVERTING, value.getClass().getName(), type.getShortName());
        ValueConverter conv = ValueConverter.Factory.getConverter(type);
        try {
            return conv.convert(value);
        } catch (ConversionException e) {
            auditor.logAndAudit(AssertionMessages.COMPARISON_CANT_CONVERT, value.getClass().getSimpleName(), type.getName());
            return null;
        }
    }

    protected boolean evalBinary(final Object value, final BinaryPredicate bpred, final Map<String, Object> vars) {
        Comparable cleft;
        if (value instanceof Comparable) {
            cleft = (Comparable)value;
        } else {
            auditor.logAndAudit(AssertionMessages.COMPARISON_NOT_COMPARABLE, value.getClass().getSimpleName(), bpred.toString());
            cleft = value.toString();
        }

        Comparable cright;
        if (bpred.getOperator().isUnary()) {
            cright = null;
        } else {
            Object right = ServerComparisonAssertion.getValue(bpred.getRightValue(), vars);
            if (type != null) {
                // Convert this rvalue before comparing if there's a DataTypePredicate present
                right = convertValue(right, type);
            }
            if (right instanceof Comparable) {
                cright = (Comparable) right;
            } else {
                auditor.logAndAudit(AssertionMessages.COMPARISON_NOT_COMPARABLE, value.getClass().getSimpleName(), bpred.toString());
                cright = value.toString();
            }
        }
        return bpred.getOperator().compare(cleft, cright, !bpred.isCaseSensitive());
    }

}
