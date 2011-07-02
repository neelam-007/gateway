package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.logging.Logger;

/**
 *
 */
public class PolicyEnforcementContextSelector implements ExpandVariables.Selector<PolicyEnforcementContext> {
    private static final Logger logger = Logger.getLogger(PolicyEnforcementContextSelector.class.getName());
    private static final Audit auditor = new LoggingAudit(logger);

    @Override
    public Selection select(String contextName, PolicyEnforcementContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        Object object = ExpandVariables.processSingleVariableAsObject(
                "${"+name+"}",
                context.getVariableMap(new String[] { name }, auditor),
                auditor);
        return new Selection(object);
    }

    @Override
    public Class<PolicyEnforcementContext> getContextObjectClass() {
        return PolicyEnforcementContext.class;
    }
}
