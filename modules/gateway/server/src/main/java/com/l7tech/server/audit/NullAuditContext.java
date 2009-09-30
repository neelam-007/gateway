package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An audit context that silently ignores all information given to it.
 */
public class NullAuditContext implements AuditContext {
    @Override
    public void setCurrentRecord(AuditRecord record) {
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
    }

    @Override
    public void addDetail(AuditDetail detail, Object source, Throwable thrown) {
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public void setUpdate(boolean update) {
    }

    @Override
    public Set getHints() {
        return Collections.emptySet();
    }

    @Override
    public void flush() {
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        return Collections.emptyMap();
    }

    @Override
    public String[] getContextVariablesUsed() {
        return new String[0];
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
    }
}
