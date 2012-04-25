package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 *
 */
public class AuditContextFactoryStub extends AuditContextFactory {
    public AuditContextFactoryStub(Config config, String nodeId) {
        super(null);
    }

    @Override
    public void emitAuditRecord(AuditRecord auditRecord, boolean update) {
        AuditContextStub context = (AuditContextStub) newContext();
        context.setCurrentRecord(auditRecord);
        context.flush();
    }

    @Override
    public <T> T doWithNewAuditContext(@NotNull Callable<T> callable, @NotNull Functions.Nullary<AuditRecord> recordFactory) throws Exception {
        final AuditContext prev = currentAuditContext.get();
        final AuditContextStub current = (AuditContextStub)newContext();
        currentAuditContext.set(current);
        try {
            return callable.call();
        } finally {
            try {
                current.setCurrentRecord(recordFactory.call());
                current.flush();
            } finally {
                currentAuditContext.set(prev);
            }
        }
    }

    /**
     * Set the audit context for the current thread.  This is only for use in unit tests where wrapping
     * entire test cases in doWithNewAuditContext would be too cumbersome.
     *
     * @param context the context to set.  Required.  Any previous context is lost.
     */
    public static void setCurrent(AuditContextStub context) {
        currentAuditContext.set(context);
    }

    @Override
    protected AuditContext newContext() {
        return new AuditContextStub();
    }
}
