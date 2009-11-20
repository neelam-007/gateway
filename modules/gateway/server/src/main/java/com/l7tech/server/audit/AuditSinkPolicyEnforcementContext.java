package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.message.Message;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;
import com.l7tech.policy.variable.NoSuchVariableException;

/**
 * Policy enforcement context used when evaluating an audit sink policy.
 * @see com.l7tech.server.audit.AuditPolicyEvaluator
 */
public class AuditSinkPolicyEnforcementContext extends PolicyEnforcementContextWrapper {
    private final AuditRecord auditRecord;
    private final PolicyEnforcementContext originalContext;

    public AuditSinkPolicyEnforcementContext( final AuditRecord auditRecord,
                                              final PolicyEnforcementContext delegate,
                                              final PolicyEnforcementContext originalContext ) {
        super(delegate);
        this.auditRecord = auditRecord;
        this.originalContext = originalContext;
    }

    public AuditRecord getAuditRecord() {
        return auditRecord;
    }

    public Message getOriginalRequest() {
        return originalContext == null ? null : originalContext.getRequest();
    }

    public Message getOriginalResponse() {
        return originalContext == null ? null : originalContext.getResponse();
    }

    public Object getOriginalContextVariable(String name) throws NoSuchVariableException {
        return originalContext == null ? null : originalContext.getVariable(name);
    }

    public PolicyEnforcementContext getOriginalContext() {
        return originalContext;
    }
}
