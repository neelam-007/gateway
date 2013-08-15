package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.HasAuditDetails;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

/**
 * Event sent representing a system event whose audit record include zero or more audit detail subrecords.
 */
public abstract class DetailedSystemEvent extends SystemEvent implements HasAuditDetails {
    private Collection<AuditDetail> details = Collections.emptyList();

    public DetailedSystemEvent(Object source, Component component) {
        super(source, component);
    }

    public DetailedSystemEvent(Object source, Component component, String ipAddress, Level level) {
        super(source, component, ipAddress, level);
    }

    public DetailedSystemEvent(Object source, Component component, @Nullable String ipAddress, Level level, String message) {
        super(source, component, ipAddress, level, message);
    }

    public DetailedSystemEvent(Object source, Component component, String ipAddress, Level level, String message, Goid identityProviderOid, String userName, String userId) {
        super(source, component, ipAddress, level, message, identityProviderOid, userName, userId);
    }

    @NotNull
    @Override
    public Collection<AuditDetail> getAuditDetails() {
        return details;
    }

    /**
     * Set the audit details that should be included in any audit record generated for this system event.
     *
     * @param auditDetails details to set.  required.
     */
    public void setAuditDetails(@NotNull Collection<AuditDetail> auditDetails) {
        this.details = Collections.unmodifiableList(new ArrayList<AuditDetail>(auditDetails));
    }
}
