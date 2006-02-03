package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.ComparisonAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Tests whether two expressions compare in keeping with the specified {@link com.l7tech.policy.assertion.ComparisonAssertion.Operator}.
 *
 * @see com.l7tech.policy.assertion.ComparisonAssertion
 */
public class ServerComparisonAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerComparisonAssertion.class.getName());

    private final Auditor auditor;
    private final ComparisonAssertion assertion;
    private final String[] variablesUsed;

    public ServerComparisonAssertion(ComparisonAssertion assertion, ApplicationContext springContext) {
        this.assertion = assertion;
        auditor = new Auditor(this, springContext, logger);
        variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map vars = context.getVariableMap(variablesUsed, auditor);

        ComparisonAssertion.Operator op = assertion.getOperator();
        String val1 = getValue(assertion.getExpression1(), vars);
        if (val1 == null) {
            auditor.logAndAudit(AssertionMessages.COMPARISON_NULL);
            return AssertionStatus.FAILED;
        }

        String val2;
        if (op.isUnary()) {
            val2 = null;
        } else {
            val2 = getValue(assertion.getExpression2(), vars);
            if (val2 == null) {
                auditor.logAndAudit(AssertionMessages.COMPARISON_NULL);
                return AssertionStatus.FAILED;
            }
        }

        boolean match;
        ComparisonAssertion.Comparer comparer = op.getComparer();

        if (comparer != null) {
            match = comparer.matches(val1, val2, assertion);
        } else if (op.isUnary()) {
            if (op == ComparisonAssertion.Operator.EMPTY) {
                match = val1.length() == 0;
            } else {
                auditor.logAndAudit(AssertionMessages.COMPARISON_BAD_OPERATOR, new String[] { op.toString() });
                return AssertionStatus.FAILED;
            }
        } else {
            if (!assertion.isCaseSensitive()) {
                val1 = val1.toLowerCase();
                val2 = val2.toLowerCase();
            }

            int comp = val1.compareTo(val2);
            if (comp > 0) comp = 1;
            if (comp < 0) comp = -1;
            match = comp == op.getCompareVal1() || comp == op.getCompareVal2();
        }

        if (assertion.isNegate()) match = !match;
        auditor.logAndAudit(match ? AssertionMessages.COMPARISON_OK : AssertionMessages.COMPARISON_NOT);
        return match ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
    }

    private String getValue(String expression1, Map variables) {
        return ExpandVariables.process(expression1, variables);
    }
}
