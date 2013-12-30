package com.l7tech.gateway.common.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An Audit implementation that ignores all audits.  Intented to be used for tests.
 */
public class NullAudit implements Audit, AuditHaver {
    @Override
    public void logAndAudit(@NotNull AuditDetailMessage msg, @Nullable String[] params, Throwable e) {
    }

    @Override
    public void logAndAudit(@NotNull AuditDetailMessage msg, String... params) {
    }

    @Override
    public void logAndAudit(@NotNull AuditDetailMessage msg) {
    }

    @Override
    public Audit getAuditor() {
        return this;
    }
}
