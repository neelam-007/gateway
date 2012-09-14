package com.l7tech.server.event.admin;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.server.event.HasAuditDetails;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

/**
 * An AdminEvent whose eventual audit record should include zero or more audit detail subrecords.
 */
public class DetailedAdminEvent extends AdminEvent implements HasAuditDetails {
    private Collection<AuditDetail> auditDetails = Collections.emptyList();

    public DetailedAdminEvent(Object source) {
        super(source);
    }

    public DetailedAdminEvent(Object source, String note) {
        super(source, note);
    }

    public DetailedAdminEvent(Object source, String note, Level level) {
        super(source, note, level);
    }

    @NotNull
    @Override
    public Collection<AuditDetail> getAuditDetails() {
        return auditDetails;
    }

    public void setAuditDetails(@NotNull Collection<AuditDetail> auditDetails) {
        this.auditDetails = auditDetails;
    }
}
