package com.l7tech.external.assertions.comparison.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import com.l7tech.external.assertions.comparison.DataTypePredicate;
import com.l7tech.external.assertions.comparison.Predicate;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.EvaluatorFactory;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests a list of boolean {@link com.l7tech.external.assertions.comparison.Predicate}s against a preconfigured left value; if all return true, the assertion
 * succeeds.
 *
 * @see com.l7tech.external.assertions.comparison.ComparisonAssertion
 */
public class ServerComparisonAssertion extends AbstractServerAssertion<ComparisonAssertion> {

    private final String[] variablesUsed;
    private final Map<Predicate, Evaluator> evaluators;
    private final Predicate[] predicates;

    public ServerComparisonAssertion( final ComparisonAssertion assertion ) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();

        this.predicates = assertion.getPredicates();
        final Map<Predicate, Evaluator> evaluators = new HashMap<Predicate, Evaluator>();
        for (Predicate predicate : predicates) {
            if (!(predicate instanceof BinaryPredicate || predicate instanceof DataTypePredicate)) {
                evaluators.put(predicate, EvaluatorFactory.makeEvaluator(predicate));
            }
        }
        this.evaluators = Collections.unmodifiableMap(evaluators);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        final Object left;

        if(assertion.isFailIfVariableNotFound()) {
            left = getValue(assertion.getExpression1(), vars, getAudit());

            if(left == null) {
                logAndAudit(AssertionMessages.COMPARISON_NULL);
                return AssertionStatus.FAILED;
            }
        } else {
            left = ExpandVariables.process(assertion.getExpression1(), vars, getAudit());
        }

        final State state = makeState( left, vars );

        Predicate failedPredicate = null;
        for ( final Predicate predicate : predicates ) {
            state.evaluate(predicate);
            if (!state.getAssertionResult()) {
                failedPredicate = predicate;
                break;
            }
        }

        if ( state.assertionResult ) {
            logAndAudit( AssertionMessages.COMPARISON_OK );
            return AssertionStatus.NONE;
        } else {
            logAndAudit( AssertionMessages.COMPARISON_NOT,
                    failedPredicate == null ? "<unknown reason>" : assertion.getExpression1() + " " + failedPredicate.toString() );
            return AssertionStatus.FALSIFIED;
        }
    }

    protected State makeState( @NotNull final Object value,
                               final Map<String,Object> variables) {
        return State.make(evaluators, assertion.getMultivaluedComparison(), value, variables, getAudit());
    }

    @Nullable
    static Object getValue( final String expression,
                            final Map<String,Object> variables,
                            final Audit auditor ) {

        return ExpandVariables.processSingleVariableAsObject(expression, variables, auditor);
    }
}
