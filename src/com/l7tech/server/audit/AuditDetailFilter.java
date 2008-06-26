package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditDetailMessage;

/**
 *
 */
public interface AuditDetailFilter {
    boolean isAuditable( AuditDetailMessage message );
}
