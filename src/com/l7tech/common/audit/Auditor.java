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

        // Don't look at this, this is a nasty hack so that the log records created here don't have Auditor.logAndAudit as their source class and method names.
        LogRecord rec = new LogRecord(msg.getLevel(), msg.getMessage()) {
            private boolean needToInferCaller = true;
            private String sourceClassName;
            private String sourceMethodName;

            private void inferCaller() {
                needToInferCaller = false;
                // Get the stack trace.
                StackTraceElement stack[] = (new Throwable()).getStackTrace();
                // First, search back to a method in the Logger class.
                int ix = 0;
                while (ix < stack.length) {
                    StackTraceElement frame = stack[ix];
                    String cname = frame.getClassName();
                    if (cname.equals("java.util.logging.Logger")) {
                        break;
                    }
                    ix++;
                }
                // Now search for the frame AFTER the first frame before the "Logger" class (to avoid logging everything as belonging to Auditor)
                boolean seenMe = false;
                for (; ix < stack.length; ix++) {
                    StackTraceElement frame = stack[ix];
                    String cname = frame.getClassName();
                    if (!cname.equals("java.util.logging.Logger")) {
                        // We've found the relevant frame. (skip one)
                        if (seenMe) {
                            if (cname.equals(Auditor.class.getName())) continue;
                            sourceClassName = cname;
                            sourceMethodName = frame.getMethodName();
                            needToInferCaller = false;
                            return;
                        } else {
                            seenMe = true;
                            continue;
                        }
                    }
                }
                // We haven't found a suitable frame, so just punt.  This is
                // OK as we are only commited to making a "best effort" here.
            }

            public String getSourceClassName() {
                if (needToInferCaller) inferCaller();
                return sourceClassName;
            }

            public String getSourceMethodName() {
                if (needToInferCaller) inferCaller();
                return sourceMethodName;
            }
        };
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
