package com.l7tech.server.log;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.server.audit.AuditLogFormatter;
import com.l7tech.server.audit.AuditLogListener;
import com.l7tech.util.ExceptionUtils;

import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Audit log listener that immediately logs the information using JUL.
 */
public class LoggingAuditLogListener implements AuditLogListener {

    //- PUBLIC

    @Override
    public void notifyDetailCreated(final String source,
                                    final AuditDetailMessage message,
                                    final String[] params,
                                    final AuditLogFormatter formatter,
                                    final Throwable thrown) {
        Throwable logThrown = message.getLevel().intValue() < Level.WARNING.intValue() ? ExceptionUtils.getDebugException(thrown) : thrown;
        String logMessage =  params == null || params.length==0 ? message.getMessage() : MessageFormat.format(message.getMessage(), params);

        AuditLogRecord record = new AuditLogRecord( message.getLevel(), logMessage );
        record.setLoggerName( source );
        record.setThrown( logThrown );

        logger.log( record );
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void notifyRecordFlushed(final AuditRecord audit,
                                    final AuditLogFormatter formatter,
                                    final boolean header) {

        AuditLogRecord record = null;

        if (header && audit instanceof MessageSummaryAuditRecord)
            record = new AuditLogRecord(Level.INFO, formatter.format(audit, header));
        else if (!header)
            record = new AuditLogRecord(audit.getLevel(), formatter.format(audit, header));

        if ( record != null ) {
            // TODO move this to the AuditRecord subclasses
            if ( audit instanceof MessageSummaryAuditRecord) {
                record.setLoggerName("com.l7tech.server.message");
            } else if ( audit instanceof AdminAuditRecord) {
                record.setLoggerName("com.l7tech.server.admin");
            } else {
                record.setLoggerName("com.l7tech.server");
            }

            logger.log( record );
        }
    }

    @Override
    public void notifyDetailFlushed(final String source,
                                    final AuditDetailMessage message,
                                    final String[] params,
                                    final AuditLogFormatter formatter,
                                    final Throwable thrown) {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(LoggingAuditLogListener.class.getName());


    /**
     * Audit subclass for LogRecord to prevent stacktrace generation.
     */
    private static final class AuditLogRecord extends LogRecord {

        public AuditLogRecord(Level level, String msg) {
            super(level, msg);
        }

        @Override
        public String getSourceClassName() {
            return getLoggerName(); // Use logger name to avoid infer caller expense (Bug #2862)
        }

        @Override
        public String getSourceMethodName() {
            return null;
        }
    }
}
