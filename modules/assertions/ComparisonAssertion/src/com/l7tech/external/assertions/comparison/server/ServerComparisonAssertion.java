package com.l7tech.external.assertions.comparison.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import com.l7tech.external.assertions.comparison.DataTypePredicate;
import com.l7tech.external.assertions.comparison.Predicate;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.EvaluatorFactory;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Tests a list of boolean {@link com.l7tech.external.assertions.comparison.Predicate}s against a preconfigured left value; if all return true, the assertion
 * succeeds.
 *
 * @see com.l7tech.external.assertions.comparison.ComparisonAssertion
 */
public class ServerComparisonAssertion extends AbstractServerAssertion<ComparisonAssertion> {
    private static final Logger logger = Logger.getLogger(ServerComparisonAssertion.class.getName());

    private final Auditor auditor;
    private final ComparisonAssertion assertion;
    private final String[] variablesUsed;
    private final Map<Predicate, Evaluator> evaluators;
    private final Predicate[] predicates;

    public ServerComparisonAssertion(ComparisonAssertion assertion, ApplicationContext springContext) {
        super(assertion);
        this.assertion = assertion;
        this.auditor = new Auditor(this, springContext, logger);
        this.variablesUsed = assertion.getVariablesUsed();

        this.predicates = assertion.getPredicates();
        Map<Predicate, Evaluator> evaluators = new HashMap<Predicate, Evaluator>();
        for (Predicate predicate : predicates) {
            if (!(predicate instanceof BinaryPredicate || predicate instanceof DataTypePredicate)) {
                evaluators.put(predicate, EvaluatorFactory.makeEvaluator(predicate));
            }
        }
        this.evaluators = Collections.unmodifiableMap(evaluators);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);

        Object left = getValue(assertion.getExpression1(), vars);
        if (left == null) {
            auditor.logAndAudit(AssertionMessages.COMPARISON_NULL);
            return AssertionStatus.FAILED;
        }

        State state = State.make(evaluators, left, vars, auditor);

        Predicate failedPredicate = null;
        for (Predicate predicate : predicates) {
            state.evaluate(predicate);
            if (!state.getAssertionResult()) {
                failedPredicate = predicate;
                break;
            }
        }

        if (state.assertionResult) {
            auditor.logAndAudit(AssertionMessages.COMPARISON_OK);
            return AssertionStatus.NONE;
        } else {
            auditor.logAndAudit(AssertionMessages.COMPARISON_NOT,
                    failedPredicate == null ? "<unknown reason>" : failedPredicate.toString());
            return AssertionStatus.FALSIFIED;
        }
    }

    static Object getValue(String expression1, Map variables) {
        return ExpandVariables.processSingleVariableAsObject(expression1, variables);
    }
}
