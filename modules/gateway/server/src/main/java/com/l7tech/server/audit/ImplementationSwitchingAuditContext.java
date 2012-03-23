package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.audit.AuditRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * An audit context that delegates to an initial context (that records information saved to it in RAM),
 * then switches to the final runtime context later on, once it is ready.
 */
public class ImplementationSwitchingAuditContext implements AuditContext {
    private static final Logger logger = Logger.getLogger(ImplementationSwitchingAuditContext.class.getName());

    private final MemoryBufferingAuditContext initialContext;
    private final AuditContext finalContext;
    private final AtomicReference<AuditContext> delegate = new AtomicReference<AuditContext>();

    public ImplementationSwitchingAuditContext(MemoryBufferingAuditContext initialContext, AuditContext finalContext) {
        this.initialContext = initialContext;
        this.finalContext = finalContext;
        delegate.set(initialContext);
    }

    public void switchImplementations() {
        logger.info("Switching over to runtime audit subsystem");
        if (!delegate.compareAndSet(initialContext, finalContext))
            throw new IllegalStateException("ImplementationSwitchingAuditContext has already switched implementations");
        
        logger.info("Processing early audit events");
        initialContext.shutdownBuffering();
        initialContext.replayAllBufferedRecords(finalContext);
        if (initialContext.isAnyUnflushedAuditRecords())
            throw new IllegalStateException("Unflushed buffered audit records are present at the time of switchover to runtime audit system");
    }
    
    @Override
    public void setCurrentRecord(AuditRecord record) {
        delegate.get().setCurrentRecord(record);
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        delegate.get().addDetail(detail, source);
    }

    @Override
    public void addDetail(AuditDetailEvent.AuditDetailWithInfo auditDetailInfo) {
        delegate.get().addDetail(auditDetailInfo);
    }

    @Override
    public boolean isUpdate() {
        return delegate.get().isUpdate();
    }

    @Override
    public void setUpdate(boolean update) {
        delegate.get().setUpdate(update);
    }

    @Override
    public Set getHints() {
        return delegate.get().getHints();
    }

    @Override
    public void flush() {
        delegate.get().flush();
    }

    @Override
    public void clear() {
        delegate.get().clear();
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        return delegate.get().getDetails();
    }

    @Override
    public String[] getContextVariablesUsed() {
        return delegate.get().getContextVariablesUsed();
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
        delegate.get().setContextVariables(variables);
    }
}
