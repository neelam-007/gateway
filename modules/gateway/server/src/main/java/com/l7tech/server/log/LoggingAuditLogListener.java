package com.l7tech.server.log;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.server.audit.AuditLogFormatter;
import com.l7tech.server.audit.AuditLogListener;

import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * Audit log listener that immediately logs the information using JUL.
 */
public class LoggingAuditLogListener implements AuditLogListener {

    //- PUBLIC

    public void notifyDetailCreated(final String source,
                                    final AuditDetailMessage message,
                                    final String[] params,
                                    final AuditLogFormatter formatter,
                                    final Throwable thrown) {
        if ( params == null || params.length==0 ) {
            logger.logp(message.getLevel(), source, null, message.getMessage(), thrown);
        } else {
            logger.logp(message.getLevel(), source, null, MessageFormat.format(message.getMessage(), params), thrown);
        }
    }

    public void notifyRecordFlushed(final AuditRecord record,
                                    final AuditLogFormatter formatter,
                                    final boolean header) {
    }

    public void notifyDetailFlushed(final String source,
                                    final AuditDetailMessage message,
                                    final String[] params,
                                    final AuditLogFormatter formatter,
                                    final Throwable thrown) {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(LoggingAuditLogListener.class.getName());
}
