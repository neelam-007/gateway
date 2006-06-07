/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerSetVariableAssertion extends AbstractServerAssertion<SetVariableAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSetVariableAssertion.class.getName());
    private final Auditor auditor;
    private final String[] varsUsed;

    public ServerSetVariableAssertion(SetVariableAssertion assertion, ApplicationContext spring) {
        super(assertion);
        auditor = new Auditor(this, spring, logger);
        varsUsed = ExpandVariables.getReferencedNames(assertion.getExpression());
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map vars = context.getVariableMap(varsUsed, auditor);
        String value = ExpandVariables.process(assertion.getExpression(), vars);
        context.setVariable(assertion.getVariableToSet(), value);
        return AssertionStatus.NONE;
    }
}
