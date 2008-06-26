package com.l7tech.server.ems;

import com.l7tech.server.audit.AuditDetailFilter;
import com.l7tech.common.audit.AuditDetailMessage;

/**
 * The EMS implementation of AuditDetailFilter.
 * Currently this class's isAuditable method always returns false.
 */
public class EmsAuditDetailFilter implements AuditDetailFilter {
    public boolean isAuditable(AuditDetailMessage message) {
        return false;
    }
}
