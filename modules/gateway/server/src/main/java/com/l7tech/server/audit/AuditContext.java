package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent.AuditDetailWithInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementations hold the transient state of the audit system for the current
 * thread.
 * <p>
 * When you have this thread's audit context, call {@link #addDetail} to
 * add audit details to it.
 * </p>
 * <p>
 * Records that are added to the context should be persisted to the database
 * later, when the context is flushed (implicitly), if their level
 * meets or exceeds the corresponding threshold.
 * </p>
 * <p>
 * Call {@link com.l7tech.server.ServerConfig#getProperty}, specifying
 * {@link com.l7tech.server.ServerConfigParams#PARAM_AUDIT_ADMIN_THRESHOLD} as
 * {@link com.l7tech.server.ServerConfigParams#PARAM_AUDIT_MESSAGE_THRESHOLD} or
  * the parameter, to determine the current threshold for
 * {@link com.l7tech.gateway.common.audit.MessageSummaryAuditRecord} and
 * {@link com.l7tech.gateway.common.audit.AdminAuditRecord}
 * records, respectively.
 * </p>
 * <p>
 * By contrast, {@link com.l7tech.gateway.common.audit.SystemAuditRecord} records are
 * persisted regardless of their level.
 * </p>
 * <p>
 * There is currently no implementation of this interface for use inside
 * non-Gateway code.
 * </p>
 */
public interface AuditContext {

    /**
     * Adds an {@link com.l7tech.gateway.common.audit.AuditDetail} record to the current context.
     *
     * <p>
     * AuditDetail records are saved when their parent AuditRecord is saved.
     * </p>
     *
     * @param detail the {@link com.l7tech.gateway.common.audit.AuditDetail} record to add. Must not be null.
     * @param source the source of the {@link com.l7tech.gateway.common.audit.AuditDetailEvent}.
     */
    void addDetail(AuditDetail detail, Object source);

    /**
     * Adds an {@link com.l7tech.gateway.common.audit.AuditDetail} record to the current context.
     *
     * <p>
     * AuditDetail records are saved when their parent AuditRecord is saved.
     * </p>
     *
     * @param auditDetailInfo the audit detail and associated information. Must not be null.
     */
    void addDetail(AuditDetailWithInfo auditDetailInfo);

    /**
     * Get the currently acumulated hints.
     *
     * @return the Set of AuditDetailMessage.Hint's
     */
    Set getHints();

    /**
     * Returns an unmodifiable Map&lt;Object, List&lt;AuditDetail&gt;&gt; of this AuditContext's details.
     * The Object is the source of the event, often a {@link com.l7tech.server.policy.assertion.ServerAssertion}.
     * @return an unmodifiable Map&lt;Object, List&lt;AuditDetail&gt;&gt; of this AuditContext's details.
     */
    Map<Object, List<AuditDetail>> getDetails();

    /**
     * Sets the context variables Map that will be used in the audit log formatter.
     *
     * @param variables the Map for the context variables values
     * @see com.l7tech.server.audit.AuditLogFormatter
     */
    void setContextVariables(Map<String, Object> variables);
}
