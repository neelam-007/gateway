package com.l7tech.gateway.common.audit;

/**
 * Interface implemented by components that own an Audit instance and wish to allow access to it.
 */
public interface AuditHaver {
    /**
     * Get the auditor to use.
     *
     * @return The auditor (must not be null)
     */
    Audit getAuditor();
}
