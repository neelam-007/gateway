package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;

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
     * @param loggerName the relevant logger name (may be null)
     * @param message The detail message
     * @param params The detail parameters (may be null)
     * @param formatter The audit log formatter
     * @param thrown The exception (may be null)
     */
    void notifyDetailCreated(String source, String loggerName, AuditDetailMessage message, String[] params, AuditLogFormatter formatter, Throwable thrown);

    /**
     * Notification of persistence of an audit detail.
     *
     * @param source The detail source
     * @param loggerName the relevant logger name (may be null)
     * @param message The detail message
     * @param params The detail parameters (may be null)
     * @param formatter The audit log formatter
     * @param thrown The exception (may be null)
     */
    void notifyDetailFlushed(String source, String loggerName, AuditDetailMessage message, String[] params, AuditLogFormatter formatter, Throwable thrown);

    /**
     * Notification of persistence of and audit record.
     *
     * @param record the record being persisted.
     * @param formatter The audit log formatter
     * @param header true if header
     */
    void notifyRecordFlushed(AuditRecord record, AuditLogFormatter formatter, boolean header);
}
