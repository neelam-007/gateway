/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.audit;

/**
 * Interface over Auditor that allows optional-audit code to be shared with non-SSG systems
 * @author alex
 */
public interface Audit {
    void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e);

    void logAndAudit(AuditDetailMessage msg, String... params);

    void logAndAudit(AuditDetailMessage msg);
}
