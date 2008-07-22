package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;

/**
 * @author Steve Jones
 */
public interface AuditContextStubInt extends AuditContext {
    AuditRecord getLastRecord();
}
