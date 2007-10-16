/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server;

import com.l7tech.external.assertions.comparison.Predicate;
import com.l7tech.external.assertions.comparison.DataTypePredicate;
import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.MultiValuedEvaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.SingleValuedEvaluator;
import com.l7tech.server.audit.Auditor;

import java.util.Map;

/**
 * Manages the progress of a <b>single-valued value</b> through {@link ServerComparisonAssertion}.
 * @author alex
 */
class SVState extends State {
    private Object value;

    SVState(final Map<Predicate, Evaluator> evaluators, Object value, Map<String, Object> vars, Auditor auditor) {
        super(evaluators, vars, auditor);
        this.value = value;
    }

    protected void evaluate(final Predicate pred) {
        Evaluator eval = evaluators.get(pred);
        boolean predResult;
        if (pred instanceof DataTypePredicate) {
            DataTypePredicate dtpred = (DataTypePredicate) pred;
            if (this.type != null) throw new IllegalStateException("DataType already set");
            this.type = dtpred.getType();
            Object val = convertValue(value, type);
            if (val == null) {
                // Unable to convert this value to the desired type
                assertionResult = false;
                return;
            }

            // mutate so subsequent predicates see converted value
            value = val;
            return;
        } else if (pred instanceof BinaryPredicate) {
            predResult = evalBinary(value, (BinaryPredicate) pred, vars);
        } else if (eval instanceof MultiValuedEvaluator) {
            // Left is single-valued, wrap in array for MultiValuedEvalator
            predResult = ((MultiValuedEvaluator) eval).evaluate(new Object[] { value });
        } else if (eval instanceof SingleValuedEvaluator) {
            predResult = ((SingleValuedEvaluator) eval).evaluate(value);
        } else {
            throw new IllegalStateException("Unable to evaluate predicate " + pred + " against value " + value);
        }

        if (pred.isNegated()) predResult = !predResult;
        assertionResult &= predResult;
    }

}
