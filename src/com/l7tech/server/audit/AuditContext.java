/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.AuditRecord;

import java.util.logging.Level;

/**
 * Implementations hold the transient state of the audit system for the current thread.
 * <p>
 * When you have this thread's audit context, call {@link #setCurrentRecord} to set the audit record for the current operation.
 * <p>
 * Records that are added to the context should be persisted to the database later, when {@link #flush} or {#link #close}
 * is called, if their level meets or exceeds the corresponding threshold.
 *
 * Call {@link com.l7tech.server.ServerConfig#getProperty}, specifying {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD} or
 * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD} as the parameter, to determine the current
 * threshold for {@link com.l7tech.common.audit.MessageSummaryAuditRecord} and {@link com.l7tech.common.audit.AdminAuditRecord}
 * records, respectively.
 * <p>
 * By contrast, {@link com.l7tech.common.audit.SystemAuditRecord} records are persisted in {@link #flush} or
 * {@link #close} regardless of their level.
 */
public interface AuditContext {
    /** Message audit threshold to be used if {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD} is unset or invalid */
    Level DEFAULT_MESSAGE_THRESHOLD = Level.WARNING;
    /** Message audit threshold to be used if {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_ADMIN_THRESHOLD} is unset or invalid */
    Level DEFAULT_ADMIN_THRESHOLD = Level.INFO;
    /** Associated logs threshold to be used if {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD} is unset or invalid */
    Level DEFAULT_ASSOCIATED_LOGS_THRESHOLD = Level.INFO;

    /**
     * Sets the current {@link com.l7tech.common.audit.AuditRecord} for this context.
     */
    void setCurrentRecord(AuditRecord record);

    /**
     * Adds an {@link AuditDetail} record to the current context.  AuditDetail records are saved when their parent AuditRecord is saved via a call to {@link #flush}.
     * @param detail the {@link AuditDetail} record to add. Must not be null.
     */
    void addDetail(AuditDetail detail);

    /**
     * Flushes the current {@link AuditRecord} and any associated {@link AuditDetail} records to the database.
     */
    void flush();

    /**
     * Closes the current context, calling {@link #flush} beforehand if necessary.
     *
     * Once closed, a context cannot be reopened.
     */
    void close();

}
