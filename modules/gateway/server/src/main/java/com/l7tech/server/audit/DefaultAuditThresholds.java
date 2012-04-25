package com.l7tech.server.audit;

import java.util.logging.Level;

/**
 * Holds default audit thresholds, those used when not customized using cluster properties.
 */
public interface DefaultAuditThresholds {
    /**
     * Message audit threshold to be used if
     * {@link com.l7tech.server.ServerConfigParams#PARAM_AUDIT_MESSAGE_THRESHOLD} is
     * unset or invalid
     */
    Level DEFAULT_MESSAGE_THRESHOLD = Level.WARNING;

    /**
     * Message audit threshold to be used if
     * {@link com.l7tech.server.ServerConfigParams#PARAM_AUDIT_SYSTEM_CLIENT_THRESHOLD} is
     * unset or invalid
     */
    Level DEFAULT_SYSTEM_CLIENT_THRESHOLD = Level.WARNING;

    /**
     * Message audit threshold to be used if
     * {@link com.l7tech.server.ServerConfigParams#PARAM_AUDIT_ADMIN_THRESHOLD} is
     * unset or invalid
     */
    Level DEFAULT_ADMIN_THRESHOLD = Level.INFO;

    /**
     * Associated logs threshold to be used if
     * {@link com.l7tech.server.ServerConfigParams#PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD}
     * is unset or invalid
     */
    Level DEFAULT_ASSOCIATED_LOGS_THRESHOLD = Level.INFO;

    /**
     * Use associated logs threshold to be used if
     * {@link com.l7tech.server.ServerConfigParams#PARAM_AUDIT_USE_ASSOCIATED_LOGS_THRESHOLD}
     * is unset or invalid
     */
    Boolean DEFAULT_USE_ASSOCIATED_LOGS_THRESHOLD = Boolean.FALSE;
}
