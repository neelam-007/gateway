package com.l7tech.server.trace;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.HasOriginalContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.util.logging.Level;

/**
 * PEC used to execute trace policies during tracing.
 */
public class TracePolicyEnforcementContext extends PolicyEnforcementContextWrapper implements HasOriginalContext {
    private final PolicyEnforcementContext tracedContext;

    private ServerAssertion tracedAssertion; // assertion currently being traced
    private AssertionStatus tracedStatus; // status returned by tracedAssertion

    public TracePolicyEnforcementContext(final PolicyEnforcementContext contextToTrace) {
        super(PolicyEnforcementContextFactory.createUnregisteredPolicyEnforcementContext(null, null, true));
        this.tracedContext = contextToTrace;

        final PublishedService svc = new PublishedService();
        svc.setName("[Internal trace policy pseudo-service]");
        svc.setSoap(false);
        this.setService(svc);
    }

    @Override
    public Message getOriginalRequest() {
        return tracedContext.getRequest();
    }

    @Override
    public Message getOriginalResponse() {
        return tracedContext.getResponse();
    }

    @Override
    public Object getOriginalContextVariable(String name) throws NoSuchVariableException {
        return tracedContext.getVariable(name);
    }

    @Override
    public PolicyEnforcementContext getOriginalContext() {
        return tracedContext;
    }

    @Override
    public void setAuditLevel(Level auditLevel) {
        tracedContext.setAuditLevel(auditLevel);
    }

    @Override
    public Level getAuditLevel() {
        return tracedContext.getAuditLevel();
    }

    @Override
    public boolean isAuditSaveRequest() {
        return tracedContext.isAuditSaveRequest();
    }

    @Override
    public void setAuditSaveRequest(boolean auditSaveRequest) {
        tracedContext.setAuditSaveRequest(auditSaveRequest);
    }

    @Override
    public boolean isAuditSaveResponse() {
        return tracedContext.isAuditSaveResponse();
    }

    @Override
    public void setAuditSaveResponse(boolean auditSaveResponse) {
        tracedContext.setAuditSaveResponse(auditSaveResponse);
    }

    /**
     * Check if this is the final invocation.
     *
     * @return true if a request has finished tracing and this is the final time this trace context will ever be invoked before it is closed.
     */
    public boolean isFinalInvocation() {
        // We'll assume this is the final invocation if the assertion being traced is the top-level assertion of the root service policy.
        return tracedAssertion != null &&
                tracedAssertion.getAssertion() != null &&
                tracedContext.getAssertionOrdinalPath().isEmpty() &&
                tracedAssertion.getAssertion().getParent() == null;
    }

    /**
     * Get the assertion currently being traced.
     *
     * @return the assertion being traced, or null if not yet set.
     */
    public ServerAssertion getTracedAssertion() {
        return tracedAssertion;
    }

    public void setTracedAssertion(ServerAssertion assertion) {
        tracedAssertion = assertion;
    }

    /**
     * Get the assertion status returned by the last assertion that finished.
     *
     * @return the assertion status, or null if not yet set.
     */
    public AssertionStatus getTracedStatus() {
        return tracedStatus;
    }

    public void setTracedStatus(AssertionStatus status) {
        tracedStatus = status;
    }
}
