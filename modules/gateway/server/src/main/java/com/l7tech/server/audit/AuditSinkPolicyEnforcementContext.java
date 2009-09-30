package com.l7tech.server.audit;

import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;

/**
 * Policy enforcement context used when evaluating an audit sink policy.
 * @see com.l7tech.server.audit.AuditPolicyEvaluator
 */
public class AuditSinkPolicyEnforcementContext extends PolicyEnforcementContext {
    private final AuditRecord auditRecord;

    public AuditSinkPolicyEnforcementContext(AuditRecord auditRecord) {
        super(new Message(), new Message());
        this.auditRecord = auditRecord;
    }

    @Override
    protected boolean isBuiltinVariable(String name) {
        return name.equalsIgnoreCase("audit") || name.toLowerCase().startsWith("audit.") || super.isBuiltinVariable(name);
    }

    @Override
    protected Object getBuiltinVariable(String name) throws NoSuchVariableException {
        if (name.equalsIgnoreCase("audit")) {
            return auditRecord;
        }

        // TODO add rest of audit record context variables from func spec

        return super.getBuiltinVariable(name);
    }

    @Override
    protected void setBuiltinVariable(String name, Object value) throws NoSuchVariableException {
        if (name.equalsIgnoreCase("audit") || name.toLowerCase().startsWith("audit.")) {
            throw new VariableNotSettableException(name);
        }

        super.setBuiltinVariable(name, value);
    }
}
