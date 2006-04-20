package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.util.ComparisonOperator;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Tests whether two expressions compare in keeping with the specified {@link com.l7tech.common.util.ComparisonOperator}.
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

        ComparisonOperator op = assertion.getOperator();
        Comparable left = getValue(assertion.getExpression1(), vars);
        if (left == null) {
            auditor.logAndAudit(AssertionMessages.COMPARISON_NULL);
            return AssertionStatus.FAILED;
        }

        Comparable right;
        if (op.isUnary()) {
            right = null;
        } else {
            right = getValue(assertion.getExpression2(), vars);
            if (right == null) {
                auditor.logAndAudit(AssertionMessages.COMPARISON_NULL);
                return AssertionStatus.FAILED;
            }
        }

        if (NUMERIC.matcher(left.toString()).matches() &&
            right != null &&
            NUMERIC.matcher(right.toString()).matches()) {
            Comparable oleft = left;
            Comparable oright = right;
            try {
                left = Double.valueOf(left.toString());
                right = Double.valueOf(right.toString());
            } catch (NumberFormatException e) {
                left = oleft;
                right = oright;
            }
        }


        boolean match = op.compare(left, right, !assertion.isCaseSensitive());
        if (assertion.isNegate()) match = !match;
        auditor.logAndAudit(match ? AssertionMessages.COMPARISON_OK : AssertionMessages.COMPARISON_NOT);
        return match ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
    }

    private String getValue(String expression1, Map variables) {
        return ExpandVariables.process(expression1, variables);
    }

    private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(?:\\.\\d*)?(?:[eE][+-]\\d+)?$");
}
