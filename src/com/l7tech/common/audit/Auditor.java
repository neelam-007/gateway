package com.l7tech.common.audit;

import com.l7tech.server.audit.AuditContext;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class Auditor {

    AuditContext context;
    Logger logger;

    public Auditor(AuditContext context, Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    public void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e) {
        if(context == null) throw new RuntimeException("AuditContext is NULL. Cannot add AuditDetail to the audit record.");

        context.addDetail(new AuditDetail(msg,
                params == null ? null : params,
                e));

        if(logger == null) return;  // todo: log will be removed once the audit feature is enhanced to replace log

        LogRecord rec = new LogRecord(msg.getLevel(), msg.getMessage());
        rec.setLoggerName(logger.getName()); // Work around NPE in LoggerNameFilter
        if (e != null) rec.setThrown(e);
        if (params != null) rec.setParameters(params);
        logger.log(rec);
    }

    public void logAndAudit(AuditDetailMessage msg, String[] params) {
        logAndAudit(msg, params, null);
    }

    public void logAndAudit(AuditDetailMessage msg) {
        logAndAudit(msg, null, null);
    }
}
