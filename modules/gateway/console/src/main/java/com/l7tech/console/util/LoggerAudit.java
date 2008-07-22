package com.l7tech.console.util;

import java.util.logging.Logger;
import java.util.logging.LogRecord;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetailMessage;

/**
 * Audit implementation that just logs messages to the given logger.
 *
 * @author Steve Jones
 */
public class LoggerAudit implements Audit {

    //- PUBLIC

    /**
     * Create a logger audit with the given logger.
     *
     * @param logger The logger to log to.
     */
    public LoggerAudit(final Logger logger) {
        this.logger = logger;
    }

    public void logAndAudit(final AuditDetailMessage msg, final String[] params, final Throwable e) {
        log(msg, params, e);
    }

    public void logAndAudit(final AuditDetailMessage msg, final String... params) {
        logAndAudit(msg, params, null);
    }

    public void logAndAudit(final AuditDetailMessage msg) {
        logAndAudit(msg, null, null);
    }

    //- PRIVATE

    private final Logger logger;

    /**
     * Log to the logger.
     */
    private void log(final AuditDetailMessage msg, final String[] params, final Throwable e) {
        if (logger.isLoggable(msg.getLevel())) {
            LogRecord rec = new LogRecord(msg.getLevel(), msg.getMessage());
            rec.setLoggerName(logger.getName());
            if (e != null) rec.setThrown(e);
            if (params != null) rec.setParameters(params);
            logger.log(rec);
        }
    }
}
