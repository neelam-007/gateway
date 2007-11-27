package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.AuditRecord;

/**
 * Interface for connecting logging to audits.
 *
 * @author Steve Jones
 */
public interface AuditLogListener {

    /**
     * Notification of creation of an audit detail.
     *
     * @param source The detail source
     * @param message The detail message
     * @param params The detail parameters (may be null)
     * @param thrown The exception (may be null)
     */
    void notifyDetailCreated(String source, AuditDetailMessage message, String[] params, Throwable thrown);

    /**
     * Notification of persistence of an audit detail.
     *
     * @param source The detail source
     * @param message The detail message
     * @param params The detail parameters (may be null)
     * @param thrown The exception (may be null)
     */
    void notifyDetailFlushed(String source, AuditDetailMessage message, String[] params, Throwable thrown);

    /**
     * Notification of persistence of and audit record.
     *
     * @param record the record being persisted.
     */
    void notifyRecordFlushed(AuditRecord record);
}
