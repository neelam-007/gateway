package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetailMessage;

/**
 *
 */
public interface AuditDetailFilter {
    boolean isAuditable( AuditDetailMessage message );
}
