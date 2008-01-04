/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.AuditRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Implementations hold the transient state of the audit system for the current
 * thread.
 * <p>
 * When you have this thread's audit context, call {@link #setCurrentRecord} to
 * set the audit record for the current operation.
 * </p>
 * <p>
 * Records that are added to the context should be persisted to the database
 * later, when {@link #flush} or {#link #close} is called, if their level
 * meets or exceeds the corresponding threshold.
 * </p>
 * <p>
 * Call {@link com.l7tech.server.ServerConfig#getProperty}, specifying
 * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD} or
 * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_ADMIN_THRESHOLD} as
 * the parameter, to determine the current threshold for
 * {@link com.l7tech.common.audit.MessageSummaryAuditRecord} and
 * {@link com.l7tech.common.audit.AdminAuditRecord}
 * records, respectively.
 * </p>
 * <p>
 * By contrast, {@link com.l7tech.common.audit.SystemAuditRecord} records are
 * persisted in {@link #flush} regardless of their level.
 * </p>
 * <p>
 * There is currently no implementation of this interface for use inside
 * non-Gateway code.
 * </p>
 */
public interface AuditContext {
    /**
     * Message audit threshold to be used if
     * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD} is
     * unset or invalid
     */
    Level DEFAULT_MESSAGE_THRESHOLD = Level.WARNING;

    /**
     * Message audit threshold to be used if
     * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_SYSTEM_CLIENT_THRESHOLD} is
     * unset or invalid
     */
    Level DEFAULT_SYSTEM_CLIENT_THRESHOLD = Level.WARNING;

    /**
     * Message audit threshold to be used if
     * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_ADMIN_THRESHOLD} is
     * unset or invalid
     */
    Level DEFAULT_ADMIN_THRESHOLD = Level.INFO;

    /**
     * Associated logs threshold to be used if
     * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD}
     * is unset or invalid
     */
    Level DEFAULT_ASSOCIATED_LOGS_THRESHOLD = Level.INFO;

    /**
     * USe associated logs threshold to be used if
     * {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_USE_ASSOCIATED_LOGS_THRESHOLD}
     * is unset or invalid
     */
    Boolean DEFAULT_USE_ASSOCIATED_LOGS_THRESHOLD = Boolean.FALSE;

    /**
     * Sets the current {@link com.l7tech.common.audit.AuditRecord} for this context.
     *
     * @param record the record to set
     */
    void setCurrentRecord(AuditRecord record);

    /**
     * Adds an {@link com.l7tech.common.audit.AuditDetail} record to the current context.
     *
     * <p>
     * AuditDetail records are saved when their parent AuditRecord is saved via
     * a call to {@link #flush}.
     * </p>
     *
     * @param detail the {@link com.l7tech.common.audit.AuditDetail} record to add. Must not be null.
     * @param source the source of the {@link com.l7tech.common.audit.AuditDetailEvent}.
     */
    void addDetail(AuditDetail detail, Object source);

    /**
     * Adds an {@link com.l7tech.common.audit.AuditDetail} record to the current context.
     *
     * <p>
     * AuditDetail records are saved when their parent AuditRecord is saved via
     * a call to {@link #flush}.
     * </p>
     *
     * @param detail the {@link com.l7tech.common.audit.AuditDetail} record to add. Must not be null.
     * @param source the source of the {@link com.l7tech.common.audit.AuditDetailEvent}.
     * @param thrown the exception associated with the {@link com.l7tech.common.audit.AuditDetailEvent}.
     */
    void addDetail(AuditDetail detail, Object source, Throwable thrown);

    /**
     * Sets whether the current record (set by {@link #setCurrentRecord}) is an update to a previous audit record.
     *
     * @param update    true if updating; false if creating
     */
    void setUpdate(boolean update);

    /**
     * Get the currently acumulated hints.
     *
     * @return the Set of AuditDetailMessage.Hint's
     */
    Set getHints();

    /**
     * Flushes the current {@link AuditRecord} and any associated
     * {@link AuditDetail} records to the database.
     * <p>
     * The context can be reused once this operation has completed.
     * </p>
     */
    void flush();

    /**
     * Returns an unmodifiable Map&lt;Object, List&lt;AuditDetail&gt;&gt; of this AuditContext's details.
     * The Object is the source of the event, often a {@link com.l7tech.server.policy.assertion.ServerAssertion}.
     * @return an unmodifiable Map&lt;Object, List&lt;AuditDetail&gt;&gt; of this AuditContext's details.
     */
    Map<Object, List<AuditDetail>> getDetails();
}
