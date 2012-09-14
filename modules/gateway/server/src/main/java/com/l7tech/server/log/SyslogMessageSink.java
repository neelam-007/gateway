package com.l7tech.server.log;

import com.l7tech.util.ConfigurableLogFormatter;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.log.syslog.Syslog;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.SyslogSeverity;
import com.l7tech.util.Pair;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

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

    /**
     * Log formats for Syslog Settings format (RAW, STANDARD, VERBOSE)
     *
     * %1 - Java Time in millis
     * %2 - Log Level (INFO, WARNING, ...)
     * %3 - Logger Name (class originating log message)
     * %4 - Actual log message
     * %5 - Thread ID (already logged by Syslog by default)
     * %6 - SourceMethod
     * %7 - don't use - Exception string - already logged if it exists
     * %8 - LogSink name
     */
    static final String SYSLOG_FORMAT_RAW = "%4$s";
    static final String SYSLOG_FORMAT_STANDARD = "%2$s %3$s: %4$s";
    static final String SYSLOG_FORMAT_VERBOSE = "[%8$s] %2$s %3$s %6$s: %4$s";

    @Override
    public void close() throws IOException {
        syslog.close();
    }

    //- PACKAGE

    SyslogMessageSink( final ServerConfig serverConfig,
                       final SinkConfiguration configuration,
                       final SyslogManager manager) throws ConfigurationException {
        this( serverConfig, configuration, manager, false );
    }

    SyslogMessageSink( final ServerConfig serverConfig,
                       final SinkConfiguration configuration,
                       final SyslogManager manager,
                       final boolean isTest) throws ConfigurationException {
        super( configuration );
        this.syslog = buildSyslog( configuration, manager, serverConfig.getHostname() );
        this.process = "SSG";
        this.isTest = isTest;
    }

    @Override
    void processMessage(final MessageCategory category, final LogRecord record) {
        final String name = getConfiguration().getName();
        final String formatName = getConfiguration().getProperty(SinkConfiguration.PROP_SYSLOG_FORMAT);

        String formatPattern;

        // see if customized in logging properties
        formatPattern = LogManager.getLogManager().getProperty("sink.format." + name + "." + formatName);
        if ( formatPattern == null ) {
            formatPattern = LogManager.getLogManager().getProperty("sink.format." + formatName);
        }

        // use default if not set
        if ( formatPattern == null ) {
            if ( SinkConfiguration.SYSLOG_FORMAT_STANDARD.equals(formatName) ) {
                formatPattern = SYSLOG_FORMAT_STANDARD;
            } else if ( SinkConfiguration.SYSLOG_FORMAT_VERBOSE.equals(formatName) ) {
                formatPattern = SYSLOG_FORMAT_VERBOSE;
            } else {
                // default to pre ssg 7 format
                formatPattern = SYSLOG_FORMAT_RAW;
            }
        }

        syslog.log(
                getSeverity(record.getLevel()),
                process,
                record.getThreadID(),
                record.getMillis(),
                new ConfigurableLogFormatter(formatPattern).format(record,new Object[] {this.getConfiguration().getName()})
        );
    }

    //- PRIVATE

    private final Syslog syslog;
    private final String process;
    private final boolean isTest;

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
     * Construct the primary syslog for this message sink.
     */
    private Syslog buildSyslog( final SinkConfiguration configuration,
                                final SyslogManager manager,
                                final String host) throws ConfigurationException
    {
        // the syslog hostlist must be > 0
        return buildSelectedSyslog(configuration, manager, host);
    }

    /**
     * Construct a syslog for the given sink configuration that connects to the host referenced by
     * the hostIndex.
     */
    private Syslog buildSelectedSyslog( final SinkConfiguration configuration,
                                final SyslogManager manager,
                                final String host) throws ConfigurationException
    {
        SyslogProtocol protocol;
        String configProtocol = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL);
        if ( SinkConfiguration.SYSLOG_PROTOCOL_TCP.equals(configProtocol)) { 
            protocol = SyslogProtocol.TCP;
        } else if ( SinkConfiguration.SYSLOG_PROTOCOL_SSL.equals(configProtocol) ) {
            protocol = SyslogProtocol.SSL;
        } else {
            protocol = SyslogProtocol.UDP;
        }

        // get the host:port from the configuration
        String[][] syslogHosts = getSyslogHost(configuration);

        String format = LOG_PATTERN_STANDARD;
        if ( !Boolean.valueOf(configuration.getProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME)) ) {
            format = LOG_PATTERN_NO_HOST;
        }

        String timeZoneId = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE);
        int facility = Integer.parseInt(configuration.getProperty(SinkConfiguration.PROP_SYSLOG_FACILITY));
        String charset = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET);

        // get properties for SSL with client auth if flag is set
        String sslKeystoreAlias = null;
        String sslKeystoreId = null;
        if (SyslogProtocol.SSL.equals(protocol) && "true".equals(configuration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_CLIENTAUTH))) {
            sslKeystoreAlias = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS);
            sslKeystoreId = configuration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID);
        }

        try {
            return manager.getSyslog(protocol, syslogHosts, format, timeZoneId, facility, host, charset, null, sslKeystoreAlias, sslKeystoreId);
        } catch (IllegalArgumentException iae) {
            throw new ConfigurationException("Error creating syslog client", iae);
        }
    }

    /**
     * Returns the host & port number values from the configuration's SyslogHost list given
     * the specified list index.
     *
     * @param configuration the sink configuration
     * @return 2-dimentional array of string values where: index 0 = host; and index 1 = port;
     * @throws ConfigurationException if the host value pulled from the configuration is not valid
     */
    private String[][] getSyslogHost(final SinkConfiguration configuration)
        throws ConfigurationException
    {
        String[][] result = new String[configuration.syslogHostList().size()][];

        int index = 0;
        for (String value : configuration.syslogHostList()) {

            Pair<String,String> hostAndPort = InetAddressUtil.getHostAndPort(value, null);

            // this error should not occur if the UI is validating the input data correctly
            if (hostAndPort.right == null) {
                throw new ConfigurationException("Invalid Syslog host format encountered=" + value);
            }

            result[index++] = new String[] { hostAndPort.left, hostAndPort.right };
        }

        return result;
    }
}
