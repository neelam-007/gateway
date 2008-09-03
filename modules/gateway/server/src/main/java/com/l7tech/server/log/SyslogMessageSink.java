package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.IOException;
import java.text.MessageFormat;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.server.log.syslog.Syslog;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.SyslogSeverity;
import com.l7tech.server.ServerConfig;

/**
 * MessageSink that writes to Syslog.
 *
 * @author Steve Jones
 */
class SyslogMessageSink extends MessageSinkSupport {

    //- PUBLIC

    /**
     * Syslog format pattern that includes a hostname
     */
    public static final String LOG_PATTERN_STANDARD = "<{2}>{3} {5} {7}[{8}]: {9}";

    /**
     * Syslog format pattern that does not have a hostname
     */
    public static final String LOG_PATTERN_NO_HOST = "<{2}>{3} {7}[{8}]: {9}";

    public void close() throws IOException {
        syslog.close();
    }

    //- PACKAGE

    SyslogMessageSink( final ServerConfig serverConfig,
                       final SinkConfiguration configuration,
                       final SyslogManager manager) throws ConfigurationException {
        super( configuration );
        this.syslog = buildSyslog( configuration, manager, serverConfig.getHostname() );
        this.process = "SSG-default_";
    }

    void processMessage(final MessageCategory category, final LogRecord record) {
        syslog.log(
                getSeverity(record.getLevel()),
                process,
                record.getThreadID(),
                record.getMillis(),
                getFormattedMessage(record.getMessage(), record.getResourceBundle(), record.getParameters())
        );
    }

    //- PRIVATE

    private final Syslog syslog;
    private final String process;

    /**
     * Map the log record level to a Syslog severity.
     */
    private SyslogSeverity getSeverity( final Level level ) {
        SyslogSeverity severity;

        int levelValue = level.intValue();

        if ( levelValue >= Level.SEVERE.intValue() ) {
            severity = SyslogSeverity.ERROR;
        } else if ( levelValue >= Level.WARNING.intValue() ) {
            severity = SyslogSeverity.WARNING;
        } else if ( levelValue >= Level.INFO.intValue() ) {
            severity = SyslogSeverity.INFORMATIONAL;
        } else {
            severity = SyslogSeverity.DEBUG;   
        }

        return severity;
    }

    /**
     * Perform any localization and parameter substitution.
     */
    private String getFormattedMessage( final String message,
                                        final ResourceBundle catalog,
                                        final Object[] parameters ) {
        String format = message;

    	if ( catalog != null ) {
	        try {
	            format = catalog.getString( message );
	        } catch ( MissingResourceException ex ) {
	    	}
        }

        if (parameters != null && parameters.length > 0) {
            // check for parameters before formatting (this is how the JDK classes do it)
            if ( format.indexOf("{0") >= 0 || format.indexOf("{1") >=0 ||
                 format.indexOf("{2") >= 0 || format.indexOf("{3") >=0 ) {
                format = MessageFormat.format( format, parameters );
            }
        }

        return format;
    }

    /**
     * Construct a syslog for the given config
     */
    private Syslog buildSyslog( final SinkConfiguration configuration,
                                final SyslogManager manager,
                                final String host ) throws ConfigurationException {
        SyslogProtocol protocol;
        String configProtocol = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL);
        if ( SinkConfiguration.SYSLOG_PROTOCOL_TCP.equals(configProtocol) ) {
            protocol = SyslogProtocol.TCP;
        } else {
            protocol = SyslogProtocol.UDP;    
        }

        String syslogHost = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_HOST);
        int syslogPort = Integer.parseInt(configuration.getProperty(SinkConfiguration.PROP_SYSLOG_PORT));
        String format = LOG_PATTERN_STANDARD;
        if ( !Boolean.valueOf(configuration.getProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME)) ) {
            format = LOG_PATTERN_NO_HOST;
        }

        String timeZoneId = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE);
        int facility = Integer.parseInt(configuration.getProperty(SinkConfiguration.PROP_SYSLOG_FACILITY));
        String charset = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET);

        try {
            return manager.getSyslog(protocol, syslogHost, syslogPort, format, timeZoneId, facility, host, charset, null);
        } catch (IllegalArgumentException iae) {
            throw new ConfigurationException("Error creating syslog client", iae);    
        }
    }
}
