package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent.AuditDetailWithInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An audit context that silently ignores all information given to it.
 */
public class NullAuditContext implements AuditContext {
    @Override
    public void addDetail(AuditDetail detail, Object source) {
    }

    @Override
    public void addDetail(AuditDetailWithInfo detail) {
    }

    @Override
    public Set getHints() {
        return Collections.emptySet();
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        return Collections.emptyMap();
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
    }
}
