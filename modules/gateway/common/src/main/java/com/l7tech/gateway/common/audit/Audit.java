/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.audit;

/**
 * Interface over Auditor that allows optional-audit code to be shared with non-SSG systems
 * @author alex
 */
public interface Audit {
    /**
     * Audit and log the specified detail message, including parameters and an optional Throwable.
     *
     * @param msg  an AuditDetailMessage.  Required.
     * @param params parameters to fill in if this detail message has parameters to fill in, or null
     * @param e a Throwable to save with this detail message, or null
     * @see AssertionMessages for a collection of AuditDetailMessage instances
     */
    void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e);

    /**
     * Audit and log the specified detail message, filled in with the specified parameters.
     *
     * @param msg  an AuditDetailMessage.  Required.
     * @param params parameters to fill in if this detail message has parameters to fill in
     */
    void logAndAudit(AuditDetailMessage msg, String... params);

    /**
     * Audit and log the specified detail message.
     *
     * @param msg  an AuditDetailMessage.  Required.
     */
    void logAndAudit(AuditDetailMessage msg);
}
