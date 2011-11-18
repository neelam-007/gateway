package com.l7tech.server.event.system;

import com.l7tech.gateway.common.audit.AuditDetail;

import java.util.Collection;

/**
 * Interface for system events that are optionally audited.
 */
public interface AuditAwareSystemEvent {

    /**
     * Should an audit record be generated for this system event?
     *
     * @param details The associated details.
     * @return True if the event should be audited.
     */
    boolean shouldBeAudited( final Collection<AuditDetail> details );
}
