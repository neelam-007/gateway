package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.EqualityAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Tests whether two expressions evaluate to equal values.
 *
 * @see com.l7tech.policy.assertion.EqualityAssertion
 */
public class ServerEqualityAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerEqualityAssertion.class.getName());

    private final ExpandVariables expandVars = new ExpandVariables();
    private final Auditor auditor;
    private final EqualityAssertion assertion;

    public ServerEqualityAssertion(EqualityAssertion assertion, ApplicationContext springContext) {
        this.assertion = assertion;
        auditor = new Auditor(this, springContext, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String val1 = getValue(assertion.getExpression1(), context.getVariables());
        String val2 = getValue(assertion.getExpression2(), context.getVariables());

        if (val1 == null || val2 == null) return AssertionStatus.FAILED;

        if (val1.equals(val2)) {
            auditor.logAndAudit(AssertionMessages.EQUALITY_EQ);
            return AssertionStatus.NONE;
        } else {
            auditor.logAndAudit(AssertionMessages.EQUALITY_NE);
            return AssertionStatus.FALSIFIED;
        }
    }

    private String getValue(String expression1, Map variables) {
        try {
            return expandVars.process(expression1, variables);
        } catch (ExpandVariables.VariableNotFoundException e) {
            auditor.logAndAudit(AssertionMessages.EQUALITY_NO_SUCH_VAR, new String[] {expression1});
            return null;
        }
    }
}
