package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditRecord;

/**
 * @author Steve Jones
 */
public interface AuditContextStubInt extends AuditContext {
    AuditRecord getLastRecord();
}
