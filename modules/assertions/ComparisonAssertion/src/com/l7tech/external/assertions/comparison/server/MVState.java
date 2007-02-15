/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server;

import com.l7tech.common.logic.BinaryPredicate;
import com.l7tech.common.logic.DataTypePredicate;
import com.l7tech.common.logic.Predicate;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.MultiValuedEvaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.SingleValuedEvaluator;
import com.l7tech.policy.variable.VariableMap;

import java.util.Map;

/**
 * Manages the progress of a multivalued value through {@link ServerComparisonAssertion}.
 * @author alex
*/
class MVState extends State {
    private Object[] values;

    MVState(Map<Predicate,Evaluator> evaluators, Object[] values, VariableMap vars) {
        super(evaluators, vars);
        this.values = values;
    }

    protected void evaluate(final Predicate pred) {
        Evaluator eval = evaluators.get(pred);
        boolean predResult;
        if (pred instanceof DataTypePredicate) {
            if (this.type != null) throw new IllegalStateException("DataType already set");
            this.type = ((DataTypePredicate) pred).getType();

            // Try to convert all the values; fail without mutating if any one cannot be converted
            Object[] newvals = new Object[values.length];
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                Object newval = convertValue(value, type);
                if (newval == null) {
                    // Unable to convert this value to the desired type
                    assertionResult = false;
                    return;
                }
                newvals[i] = newval;
            }
            // mutate so subsequent predicates see converted value
            values = newvals;

            return;
        } else if (eval instanceof MultiValuedEvaluator) {
            predResult = ((MultiValuedEvaluator) eval).evaluate(values);
        } else if (eval instanceof SingleValuedEvaluator || pred instanceof BinaryPredicate) {
            // Left is multivalued, must split into single values for this predicate
            boolean tempResult = true;
            for (Object value : values) {
                if (pred instanceof BinaryPredicate) {
                    BinaryPredicate bpred = (BinaryPredicate) pred;
                    tempResult &= evalBinary(value, bpred, vars);
                } else {
                    tempResult &= ((SingleValuedEvaluator) eval).evaluate(value);
                }
            }

            predResult = tempResult;
        } else {
            throw new IllegalStateException("Unable to evaluate predicate " + pred + " against values " + values);
        }

        if (pred.isNegated()) predResult = !predResult;
        assertionResult &= predResult;
    }
}
