package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.HasOriginalContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;

/**
 * Policy enforcement context used when evaluating an audit sink policy.
 * @see com.l7tech.server.audit.AuditPolicyEvaluator
 */
public class AuditSinkPolicyEnforcementContext extends PolicyEnforcementContextWrapper implements HasOriginalContext {
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

    @Override
    public Message getOriginalRequest() {
        return originalContext == null ? null : originalContext.getRequest();
    }

    @Override
    public Message getOriginalResponse() {
        return originalContext == null ? null : originalContext.getResponse();
    }

    @Override
    public Object getOriginalContextVariable(String name) throws NoSuchVariableException {
        return originalContext == null ? null : originalContext.getVariable(name);
    }

    public String getAuditedRequest() {
        if (auditRecord instanceof MessageSummaryAuditRecord) {
            MessageSummaryAuditRecord record = (MessageSummaryAuditRecord) auditRecord;
            return record.getRequestXml();
        }
        return null;
    }

    public String getAuditedResponse() {
        if (auditRecord instanceof MessageSummaryAuditRecord) {
            MessageSummaryAuditRecord record = (MessageSummaryAuditRecord) auditRecord;
            return record.getResponseXml();
        }
        return null;
    }

    /** @return the original PolicyEnforcementContext if we are handling a message processing audit event, or null. */
    @Override
    public PolicyEnforcementContext getOriginalContext() {
        return originalContext;
    }
}
