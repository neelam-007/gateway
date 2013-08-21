package com.l7tech.server.event;

import com.l7tech.gateway.common.audit.AuditDetail;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Interface implemented by objects that may offer audit details.
 */
public interface HasAuditDetails {
    /**
     * Get the audit details.
     * <p/>
     * Depending on the implementor of this interface, the details may always be not-yet-saved details
     * with object id {@link com.l7tech.objectmodel.PersistentEntity#DEFAULT_GOID}, or they may always be
     * already-persisted details with a new OID.
     * <p/>
     * Callers should not attempt to modify the details returned by this method unless a more-specific
     * implementor contract promises it is OK to do so.
     *
     * @return zero or more audit detail instances.  Never null.
     */
    @NotNull
    Collection<AuditDetail> getAuditDetails();
}
