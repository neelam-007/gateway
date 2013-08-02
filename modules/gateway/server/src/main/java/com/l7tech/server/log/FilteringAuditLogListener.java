package com.l7tech.server.log;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.log.HybridDiagnosticContextKeys;
import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.server.audit.AuditLogFormatter;
import com.l7tech.server.audit.AuditLogListener;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Config;
import com.l7tech.util.JdkLoggerConfigurator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * AuditLogListener that passes audit information to a MessageSink.
 *
 * <p>This listener will filter out details that are passed at either creation
 * or flush time (depending on configuration)</p>
 *
 * <p>5.0 bug# 5181 - Request Id enhancement to enable audit log formatting to be defined by
 * the following cluster properties:
 * <ul>
 * <li>audit.log.service.headerFormat</li>
 * <li>audit.log.service.footerFormat</li>
 * <li>audit.log.service.detailFormat</li>
 * <li>audit.log.other.format</li>
 * <li>audit.log.other.detailFormat</li>
 * <li></li>
 * </ul>
 * 
 * @author Steve Jones
 */
public class FilteringAuditLogListener implements AuditLogListener, PropertyChangeListener {

    //- PUBLIC

    /**
     * Create a listener with the given sink.
     *
     * @param sink The sink for audit data
     */
    public FilteringAuditLogListener(final Config config,
                                     final MessageSink sink) {
        this.sink = sink;
        this.auditOnCreate.set( !config.getBooleanProperty( PROP_BATCH, PROP_BATCH_DEFAULT ) );
    }

    /**
     * Handle notification of an audit detail creation.
     *
     * @param source the source of the audit
     * @param loggerName the relevant logger name (may be null)
     * @param message The audit detail message
     * @param params The detail parameters (may be null)
     * @param thrown The detail throwable (may be null)
     */
    @Override
    public void notifyDetailCreated(final String source,
                                    final String loggerName,
                                    final AuditDetailMessage message,
                                    final String[] params,
                                    final AuditLogFormatter formatter,
                                    final Throwable thrown) {
        if ( auditOnCreate.get() ) {
            LogRecord record = generateMessage(source, loggerName, message, params, formatter, thrown);
            processMessage(record);
        }
    }

    /**
     * Handle notification of an audit detail flush.
     *
     * @param source the source of the audit
     * @param loggerName the relevant logger name (may be null)
     * @param message The audit detail message
     * @param params The detail parameters (may be null)
     * @param thrown The detail throwable (may be null)
     */
    @Override
    public void notifyDetailFlushed(final String source,
                                    final String loggerName,
                                    final AuditDetailMessage message,
                                    final String[] params,
                                    final AuditLogFormatter formatter,
                                    final Throwable thrown) {
        if ( !auditOnCreate.get() ) {
            LogRecord record = generateMessage(source, loggerName, message, params, formatter, thrown);
            processMessage(record);
        }
    }

    /**
     * Handle notification of an audit record flush.
     *
     * @param audit The audit record
     * @param header True if is header
     */
    @Override
    public void notifyRecordFlushed(final AuditRecord audit, final AuditLogFormatter formatter, final boolean header) {
        LogRecord record = generateMessage(audit, formatter, header);
        if ( record != null )
            processMessage(record);
    }

    /**
     * Handle update to audit batching server property.
     *
     * @param evt The property update event
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if ( PROP_BATCH.equals( evt.getPropertyName() ) ) {
            auditOnCreate.set(!Boolean.parseBoolean((String)evt.getNewValue()));
        }
        // check formatter properties
        AuditLogFormatter.notifyPropertyChange(evt.getPropertyName());
    }

    //- PRIVATE

    private static final boolean PROP_BATCH_DEFAULT = true;
    private static final String PROP_BATCH = "auditBatchExternal";

    private final MessageSink sink;
    private final AtomicBoolean auditOnCreate = new AtomicBoolean(PROP_BATCH_DEFAULT);

    /**
     * Generate a LogRecord for the AuditDetailMessage.
     *
     * @param source source that generated the log
     * @param loggerName the logger name to use, or null
     * @param message the audit detail message
     * @param params message parameters
     * @param formatter the logging formatter to handle the message formatting
     * @param thrown the exception that occurred
     * @return audit log record that can be sent to the log sink
     */
    @SuppressWarnings("unchecked")
    private LogRecord generateMessage(final String source,
                                      final String loggerName,
                                      final AuditDetailMessage message,
                                      final String[] params,
                                      final AuditLogFormatter formatter,
                                      final Throwable thrown) {
        AuditLogRecord record = new AuditLogRecord(message.getLevel(), formatter.formatDetail(message));

        if (loggerName != null) {
            record.setLoggerName(loggerName);
        } else {
            record.setLoggerName(source);
        }
        if (thrown != null)
            record.setThrown(thrown);

        if (params != null)
            record.setParameters(params);

        if (JdkLoggerConfigurator.serviceNameAppenderState()) {
            String serviceName = getServiceName();
            if (serviceName != null && serviceName.length() > 0) {
                record.setMessage(record.getMessage() + " @ " + serviceName);
            }
        }

        return record;
    }

    /**
     * Get the name for the current service.
     */
    private String getServiceName() {
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.getCurrent();

        if ( pec != null && pec.getService() != null ) {
            return pec.getService().getName() + " [" + pec.getService().getGoid() + "]";
        }

        return null;
    }

    /**
     * Generate a LogRecord
     *
     * @param audit the audit record to generate
     * @param formatter the logging formatter to handle the message formatting
     * @param header flag specifying whether the message is for the audit header vs footer
     * @return audit log record that can be sent to the log sink
     */
    @SuppressWarnings("unchecked")
    private LogRecord generateMessage(final AuditRecord audit, final AuditLogFormatter formatter, final boolean header) {

        AuditLogRecord record = null;
        if (header && audit instanceof MessageSummaryAuditRecord)
            record = new AuditLogRecord(Level.INFO, formatter.format(audit, header));
        else if (!header)
            record = new AuditLogRecord(audit.getLevel(), formatter.format(audit, header));

        if ( record != null ) {
            // TODO move this to the AuditRecord subclasses
            if ( audit instanceof MessageSummaryAuditRecord) {
                record.setLoggerName("com.l7tech.server.message");
            } else if ( audit instanceof AdminAuditRecord ) {
                record.setLoggerName("com.l7tech.server.admin");
            } else {
                record.setLoggerName("com.l7tech.server");
            }
        }

        return record;
    }

    /**
     * Pass the given record to the sink as an audit message
     */
    private void processMessage(final LogRecord record) {
        HybridDiagnosticContext.put( HybridDiagnosticContextKeys.LOGGER_NAME, record.getLoggerName() );
        sink.message(MessageCategory.AUDIT, record);
    }

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
