package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetailMessage;

import java.util.logging.Logger;

/**
 * Extension of auditor for use when there is no audit context available.
 *
 * @author Steve Jones
 */
public class LogOnlyAuditor extends Auditor {

    public LogOnlyAuditor(Logger logger) {
        super(logger);
    }

    public void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e) {
        log(msg, params, e);
    }

    public void logAndAudit(AuditDetailMessage msg, String... params) {
        logAndAudit(msg, params, null);
    }

    public void logAndAudit(AuditDetailMessage msg) {
        logAndAudit(msg, null, null);
    }
}
