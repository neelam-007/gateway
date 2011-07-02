package com.l7tech.gateway.common.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Audit implementation that logs to the given Logger.
 *
 * @author Steve Jones
 */
public class LoggingAudit implements Audit, AuditHaver {

    //- PUBLIC

    /**
     * Create an Audit that logs the given detail messages.
     *
     * @param logger the Logger to use.  Required.
     */
    public LoggingAudit( @NotNull final Logger logger ) {
        this.logger = logger;
    }

    @Override
    public void logAndAudit( @NotNull  final AuditDetailMessage msg,
                             @Nullable final String[] params,
                             @Nullable final Throwable e ) {
        log(msg, params, e);
    }

    @Override
    public void logAndAudit( @NotNull  final AuditDetailMessage msg,
                             @Nullable final String... params ) {
        logAndAudit(msg, params, null);
    }

    @Override
    public void logAndAudit( @NotNull final AuditDetailMessage msg ) {
        logAndAudit(msg, null, null);
    }

    @Override
    public final Audit getAuditor() {
        return this;
    }

    /**
     * Get a factory for LoggingAudits.
     *
     * @return The factory.
     */
    @NotNull
    public static AuditFactory factory() {
        return new LoggingAuditFactory();
    }

    /**
     * Factory for LoggingAudits.
     */
    public static final class LoggingAuditFactory implements AuditFactory {
        @Override
        public Audit newInstance( final Object source, final Logger logger ) {
            return new LoggingAudit( logger );
        }
    }

    //- PROTECTED

    protected final Logger logger;

    protected void log( final AuditDetailMessage msg,
                        final String[] params,
                        final Throwable e) {
        if ( logger.isLoggable(msg.getLevel()) ) {
            logDetail( msg, params, e );
        }
    }

    protected void logDetail( final AuditDetailMessage msg,
                              final String[] params,
                              final Throwable e) {
        LogRecord record = new LogRecord(msg.getLevel(), formatDetail(msg));
        record.setParameters(params);
        record.setThrown(e);
        record.setSourceClassName("");
        record.setSourceMethodName("");
        record.setLoggerName(logger.getName());
        logger.log( record );
    }

    protected String formatDetail( final AuditDetailMessage msg ) {
        return msg.getMessage();
    }

}
