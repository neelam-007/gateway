package com.l7tech.gateway.common.audit;

import java.util.logging.Logger;

/**
 * Factory for Audits.
 */
public interface AuditFactory {

    /**
     * Create an Audit for the specified source and logger.
     *
     * @param source Source object for audit events.  Required.
     * @param logger Logger to which details will be written.  Required.
     * @return The Audit to use
     */
    Audit newInstance( Object source, Logger logger );
}
