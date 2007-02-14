/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server;

import com.l7tech.policy.variable.VariableMap;
import com.l7tech.policy.variable.DataType;
import com.l7tech.common.logic.Predicate;
import com.l7tech.common.logic.BinaryPredicate;
import com.l7tech.external.assertions.comparison.server.convert.ValueConverter;
import com.l7tech.external.assertions.comparison.server.convert.ConversionException;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Holds the state kept while evaluating a single request through the {@link ServerComparisonAssertion}.
 * @author alex
*/
abstract class State {
    private static final Logger logger = Logger.getLogger(State.class.getName());

    protected final Map<Predicate, Evaluator> evaluators;
    protected final VariableMap vars;

    /**
     * True iff all predicates processed thus far have evaluated to true. 
     */
    protected boolean assertionResult = true;

    public boolean getAssertionResult() {
        return assertionResult;
    }

    protected State(Map<Predicate, Evaluator> evaluators, VariableMap vars) {
        this.evaluators = evaluators;
        this.vars = vars;
    }

    protected abstract void evaluate(Predicate pred);

    static State make(Map<Predicate, Evaluator> evaluators, Object left, VariableMap vars) {
        Class leftClass = left.getClass();
        if (leftClass.isArray()) {
            return new MVState(evaluators, (Object[])left, vars);
        } else if (Collection.class.isAssignableFrom(leftClass)) {
            return new MVState(evaluators, ((Collection)left).toArray(new Object[0]), vars);
        } else {
            return new SVState(evaluators, left, vars);
        }
    }

    static Object convertValue(Object value, DataType type) {
        for (Class clazz : type.getValueClasses()) {
            if (clazz.isAssignableFrom(value.getClass())) {
                // no conversion required
                return value;
            }
        }

        // TODO audit
        logger.fine("Converting " + value.getClass() + " value into " + type.getShortName());
        ValueConverter conv = ValueConverter.Factory.getConverter(type);
        try {
            return conv.convert(value);
        } catch (ConversionException e) {
            // TODO audit
            logger.warning("Value of type " + value.getClass().getSimpleName() + " cannot be converted to " + type.getName());
            return null;
        }
    }

    static boolean evalBinary(final Object value, final BinaryPredicate bpred, final VariableMap vars) {
        Comparable cleft;
        if (value instanceof Comparable) {
            cleft = (Comparable)value;
        } else {
            // TODO audit
            logger.warning(MessageFormat.format("Left value for binary predicate {0} is not Comparable; using value.toString() instead", bpred));
            cleft = value.toString();
        }

        Object right = ServerComparisonAssertion.getValue(bpred.getRightValue(), vars);
        Comparable cright;
        if (right instanceof Comparable) {
            cright = (Comparable) right;
        } else {
            // TODO audit
            logger.warning(MessageFormat.format("Right value for binary predicate {0} is not Comparable; using value.toString() instead", bpred));
            cright = value.toString();
        }
        return bpred.getOperator().compare(cleft, cright, !bpred.isCaseSensitive());
    }

}
