package com.l7tech.common.audit;

import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class Auditor {
    private final ApplicationContext context;
    private final Object source;
    private final Logger logger;

    public Auditor(Object source, ApplicationContext context, Logger logger) {
        if(source  == null) throw new RuntimeException("Event source is NULL. Cannot add AuditDetail to the audit record.");
        if(logger == null) throw new RuntimeException("ApplicationContext is NULL. Cannot add AuditDetail to the audit record.");

        this.source = source;
        this.context = context;
        this.logger = logger;
    }

    public void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e) {
        if (context != null)
            context.publishEvent(new AuditDetailEvent(source, new AuditDetail(msg, params == null ? null : params, e)));

        if (logger == null) return;

        if (logger.isLoggable(msg.getLevel())) {
            LogRecord rec = new AuditHackLogRecord(msg);
            rec.setLoggerName(logger.getName());
            if (e != null) rec.setThrown(e);
            if (params != null) rec.setParameters(params);
            if (JdkLoggerConfigurator.serviceNameAppenderState()) {
                String serviceName = getServiceName();
                if (serviceName != null && serviceName.length() > 0) {
                    rec.setMessage(rec.getMessage() + " @ " + serviceName);
                }
            }
            logger.log(rec);
        }
    }

    private String getServiceName() {
        PolicyEnforcementContext pec = PolicyEnforcementContext.getCurrent();
        if (pec != null && pec.getService() != null) {
            return pec.getService().getName() + " [" + pec.getService().getOid() + "]";
        }
        return "";
    }

    public void logAndAudit(AuditDetailMessage msg, String[] params) {
        logAndAudit(msg, params, null);
    }

    public void logAndAudit(AuditDetailMessage msg) {
        logAndAudit(msg, null, null);
    }

    // Don't look at this, this is a nasty hack so that the log records created here don't have Auditor.logAndAudit as their source class and method names.
    private static class AuditHackLogRecord extends LogRecord {
        private boolean needToInferCaller = true;
        private String sourceClassName;
        private String sourceMethodName;

        public AuditHackLogRecord(AuditDetailMessage msg) {
            super(msg.getLevel(), msg.getMessage());
        }

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
            return getLoggerName(); // Use logger name to avoid infer caller expense (Bug #2862)
//            if (needToInferCaller) inferCaller();
//            return sourceClassName;
        }

        public String getSourceMethodName() {
            return null;  // don't infer caller; it'll be bogus in a production build, and causes slowdown (Bug #2862)
//            if (needToInferCaller) inferCaller();
//            return sourceMethodName;
        }
    }
}
