package com.l7tech.server.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.NoSuchVariableException;

/**
 *
 */
public class PolicyEnforcementContextSelector implements ExpandVariables.Selector<PolicyEnforcementContext> {
    @Override
    public Selection select(PolicyEnforcementContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        try {
            return new Selection(context.getVariable(name));
        } catch (NoSuchVariableException e) {
            return null;
        }
    }

    @Override
    public Class<PolicyEnforcementContext> getContextObjectClass() {
        return PolicyEnforcementContext.class;
    }
}
