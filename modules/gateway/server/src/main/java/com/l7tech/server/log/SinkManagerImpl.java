package com.l7tech.server.log;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.SyslogEvent;
import com.l7tech.server.log.syslog.SyslogConnectionListener;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.TestingSyslogManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ValidationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.*;

/**
 *
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SinkManagerImpl
        extends HibernateEntityManager<SinkConfiguration, EntityHeader>
        implements SinkManager, ApplicationContextAware, PropertyChangeListener {

    //- PUBLIC

    public SinkManagerImpl( final ServerConfig serverConfig,
                            final SyslogManager syslogManager,
                            final TrafficLogger trafficLogger,
                            final ApplicationEventProxy eventProxy ) {
        if ( serverConfig == null ) throw new IllegalArgumentException("serverConfig must not be null");
        if ( syslogManager == null ) throw new IllegalArgumentException("syslogManager must not be null");

        this.serverConfig = serverConfig;
        this.syslogManager = syslogManager;
        this.trafficLogger = trafficLogger;

        eventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                handleEvent(event);
            }
        });
    }

    /**
     * Get the file storage allocation for logs.
     *
     * @return The size in bytes.
     */
    @Override
    public long getMaximumFileStorageSpace() {
        long storage = DEFAULT_FILE_SPACE_LIMIT;

        String value = serverConfig.getProperty( SCPROP_FILE_LIMIT );
        if ( value != null ) {
            try {
                storage = Long.parseLong(value);
            } catch ( NumberFormatException nfe ) {
                logger.log(Level.WARNING, "Error parsing property ''{0}'', with value ''{1}'' as number.",
                        new String[]{ SCPROP_FILE_LIMIT, value });
            }
        }

        return storage;
    }

    /**
     * Get the used file storage space in bytes.
     *
     * <p>This will calculate the space allocated for all currently enabled
     * log sinks.</p>
     *
     * @return The size in bytes.
     */
    @Override
    public long getReservedFileStorageSpace() {
        long reservedSpace = 0;

        Collection<SinkConfiguration> sinkConfigs = loadSinkConfigurations();
        for ( SinkConfiguration sinkConfiguration : sinkConfigs ) {
            if ( sinkConfiguration.isEnabled() && isValid( sinkConfiguration ) ) {
                reservedSpace += getReservedSpace( sinkConfiguration );
            }
        }

        return reservedSpace;
    }

    @Override
    public Class<SinkConfiguration> getImpClass() {
        return SinkConfiguration.class;
    }

    @Override
    public Class<SinkConfiguration> getInterfaceClass() {
        return SinkConfiguration.class;
    }

    @Override
    public String getTableName() {
        return "sink_config";
    }

    /**
     * Test the given sinkConfiguration (useful for syslog).
     *
     * 5.0 introduced the abililty to configure multiple failover hosts for syslog.  The test functionality has been
     * updated to send a test message to each syslog server in the SinkConfiguration.
     *
     * @param sinkConfiguration The configuration to test.
     * @param testMessage The message to send.
     * @return True if the message may have been sent
     */
    @Override
    public boolean test(final SinkConfiguration sinkConfiguration, final String testMessage) {
        boolean success = true;

        // what about testing file logging
        if ( sinkConfiguration != null ) {

            List<Closeable> resources = new ArrayList<Closeable>(10);
            TestingSyslogManager testManager = new TestingSyslogManager();
            resources.add(testManager);

            try {
                MessageSinkSupport sink = new SyslogMessageSink( serverConfig, sinkConfiguration, testManager, true );

                for ( MessageCategory category : MessageCategory.values() ) {
                    if ( sink.isCategoryEnabled(category) ) {
                        LogRecord testRecord = new LogRecord( Level.INFO, testMessage==null ? "Test message." : testMessage );

                        // up level if required for message to be sent
                        Level testLevel = Level.parse(sinkConfiguration.getSeverity().toString());
                        if ( testLevel.intValue() > Level.INFO.intValue() ) {
                            testRecord.setLevel(testLevel);
                        }

                        sink.message(category, testRecord);
                        resources.add(sink);

                        // test each host in the list
                        for (int i=1; i<sinkConfiguration.syslogHostList().size(); i++) {
                            testManager.setTestingHost(i);
                            sink = new SyslogMessageSink( serverConfig, sinkConfiguration, testManager, true );
                            sink.message(category, testRecord);
                            resources.add(sink);
                        }

                        break;
                    }
                }
            } catch (MessageSinkSupport.ConfigurationException cex) {
                // should send feedback to UI
                success = false;

            } finally {
                // we don't know how long it will take to send a message so leave open for 60 secs
                delayedClose(resources.toArray(new Closeable[resources.size()]), 60000L);
            }
        }

        return success;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        if ( this.applicationContext == null )
            this.applicationContext = applicationContext;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        updateLogLevels( (String)evt.getOldValue(), (String)evt.getNewValue() );
    }

    @Override
    public MessageSink getPublishingSink() {
        return publishingSink;
    }

    public static MessageSink getPublishingSink( final Object sinkManager ) {
        return ((SinkManager)sinkManager).getPublishingSink();
    }

    //- PROTECTED

    @Override
    protected void initDao() {
        logger.info("Redirecting logging to configured log sinks.");

        installHandlers();
        updateLogLevels(null, null);
        installLogConfigurationListener();
        installConnectionListener();
        closeMarkedHandlers();
        rebuildLogSinks();

        logger.info("Redirected logging to configured log sinks.");
    }

    // - PACKAGE

    static final String TRAFFIC_LOGGER_NAME = "com.l7tech.traffic";

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SinkManager.class.getName());

    private static final String SCPROP_LOG_LEVELS = "logLevels";
    private static final String SCPROP_FILE_LIMIT = "logFileSpaceMax";
    private static final String DEFAULT_TRAFFIC_LOG_NAME_POSTFIX = "_%g_%u.log";
    private static final long ONE_GIGABYTE = 1024L * 1024L * 1024L;
    private static final long DEFAULT_FILE_SPACE_LIMIT = ONE_GIGABYTE * 5L; //5GB

    private final DispatchingMessageSink dispatchingSink = new DispatchingMessageSink();
    private final MessageSink publishingSink = new DelegatingMessageSink(dispatchingSink);
    private final ServerConfig serverConfig;
    private final SyslogManager syslogManager;
    private final TrafficLogger trafficLogger;
    private ApplicationContext applicationContext;

    /**
     * Handle application event
     */
    private void handleEvent(final ApplicationEvent event) {
        if ( event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent evt = (EntityInvalidationEvent) event;

            if ( SinkConfiguration.class.isAssignableFrom(evt.getEntityClass()) ) {
                rebuildLogSinks();
            }
        }
    }

    /**
     * Re-install logging handlers.
     */
    private void installHandlers() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(new SinkHandler(dispatchingSink, MessageCategory.LOG));

        Logger trafficLogger = Logger.getLogger(TRAFFIC_LOGGER_NAME);
        trafficLogger.addHandler(new SinkHandler(dispatchingSink, MessageCategory.TRAFFIC));

        String value = LogManager.getLogManager().getProperty(TRAFFIC_LOGGER_NAME + ".useParentHandlers");
        if ( value != null ) {
            trafficLogger.setUseParentHandlers( Boolean.valueOf(value) );
        }
    }

    /**
     * Remove any startup aware Handlers
     */
    private void closeMarkedHandlers() {
        Logger rootLogger = Logger.getLogger("");
        for ( Handler handler : rootLogger.getHandlers() )  {
            if ( handler instanceof StartupAwareHandler ) {
                rootLogger.removeHandler( handler );
                handler.close();
            }
        }
    }

    /**
     * Update the log levels with any customized properties.
     *
     * We detect if a log level has been removed so we can set it to it's parent value.
     */
    private void updateLogLevels(final String oldValue, final String newValue) {
        String loggingLevels = newValue;
        if ( loggingLevels == null )
            loggingLevels = serverConfig.getProperty( SCPROP_LOG_LEVELS );

        try {
            // load props
            Properties levelProps = new Properties();
            levelProps.load(new StringReader(loggingLevels));

            Properties oldProps = new Properties();
            if (oldValue != null)
                oldProps.load(new StringReader(oldValue));

            // get locally configured levels, these should
            // not be overridden
            Properties properties = JdkLoggerConfigurator.getNonDefaultProperties();

            // set configured levels
            for ( Map.Entry<Object,Object> property : levelProps.entrySet() ) {
                String key = (String) property.getKey();
                String value = (String) property.getValue();

                if ( key.endsWith(".level") && properties.getProperty(key)==null) {
                    String name = key.substring(0, key.length()-6);
                    try {
                        Level configLevel = Level.parse(value);
                        Logger configLogger = Logger.getLogger(name);
                        configLogger.setLevel(configLevel);
                    } catch (IllegalArgumentException iae) {
                        // ignore invalid level
                        logger.log(Level.CONFIG, "Ignoring invalid level ''{0}'', for logger ''{1}''.",
                                new Object[]{value, name});
                    }
                }
            }

            // reset removed levels (so parent or original config level is used)
            LogManager manager = LogManager.getLogManager();
            for ( Object KeyObj : oldProps.keySet() ) {
                String key = (String) KeyObj;

                if ( key.endsWith(".level") &&
                        levelProps.getProperty(key)==null ) {
                    String name = key.substring(0, key.length()-6);
                    Logger configLogger = Logger.getLogger(name);
                    boolean levelSet = false;
                    String levelStr = manager.getProperty(key);
                    if ( levelStr != null ) {
                        try {
                            configLogger.setLevel( Level.parse(levelStr) );
                            levelSet = true;
                        } catch (IllegalArgumentException iae) {
                            // ignore invalid level from logging config file
                        }
                    }
                    if (!levelSet) {
                        configLogger.setLevel(null);
                    }
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Unexpected error processing logging levels.", ioe);
        }
    }

    /**
     * Install a log configuration listener to re-install our Handlers when the
     * configuration changes.
     */
    private void installLogConfigurationListener() {
        LogManager.getLogManager().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                installHandlers();
                updateLogLevels(null, null);
            }
        });
    }

    /**
     * Install a syslog connection listener to audit connection errors.
     */
    private void installConnectionListener() {
        syslogManager.setConnectionListener(new
                SyslogConnectionListener(){
            @Override
            public void notifyConnected(final SocketAddress address) {
                fireEvent(address, true);
            }

            @Override
            public void notifyDisconnected(final SocketAddress address) {
                fireEvent(address, false);
            }

            private void fireEvent(final SocketAddress address, final boolean connected) {
                if ( applicationContext != null ) {
                    applicationContext.publishEvent(
                            new SyslogEvent(SinkManagerImpl.this, address.toString(), connected));
                }
            }
        });
    }

    /**
     * Attach the configured sinks.
     */
    private void rebuildLogSinks() {
        List<MessageSink> sinks = new ArrayList<MessageSink>();

        // close old first (so file handle released, etc)
        // this could be removed if we this is not desired behaviour
        // (in which case the old Sinks that are not used will be automatically
        // closed by the DispatchingMessageSink)
        dispatchingSink.setMessageSinks( Collections.<MessageSink>emptySet() );

        // construct new
        Collection<SinkConfiguration> sinkConfigs = loadSinkConfigurations();
        for ( SinkConfiguration sinkConfiguration : sinkConfigs ) {
            if ( sinkConfiguration.isEnabled() ) {

                if ( logger.isLoggable(Level.CONFIG) )
                    logger.log(Level.CONFIG, "Processing log sink configuration ''{0}''.", sinkConfiguration.getName());

                if ( !isValid( sinkConfiguration ) ) {
                    if ( logger.isLoggable(Level.CONFIG) )
                        logger.log(Level.CONFIG, "Ignoring invalid log sink configuration ''{0}''.", sinkConfiguration.getName());
                } else {
                    MessageSinkSupport sink = buildSinkForConfiguration( sinkConfiguration );

                    if ( sink != null ) {
                        if ( logger.isLoggable(Level.CONFIG) )
                            logger.log(Level.CONFIG, "Installing log sink ''{0}''.", sinkConfiguration.getName());

                        sinks.add( sink );
                    }
                }
            } else {
                if ( logger.isLoggable(Level.CONFIG) )
                    logger.log(Level.CONFIG, "Ignoring disabled log sink configuration ''{0}''.", sinkConfiguration.getName());
            }
        }

        stashLogFileSettings( sinks );
        addConsoleSink( sinks );
        processOldTrafficLoggerConfig( sinks );
        updateTrafficLoggingEnabledState( sinks );

        // install new
        dispatchingSink.setMessageSinks( sinks );
    }

    private void stashLogFileSettings( final List<MessageSink> sinks ) {
        File varDir = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        File logConfig = new File( varDir, LogUtils.LOG_SER_FILE );

        List<FileMessageSink> fileSinks = new ArrayList<FileMessageSink>();
        for ( MessageSink sink : sinks ) {
            if ( sink instanceof FileMessageSink ) {
                FileMessageSink fileSink = (FileMessageSink) sink;
                if ( fileSink.isCategoryEnabled( MessageCategory.AUDIT ) ||
                     fileSink.isCategoryEnabled( MessageCategory.LOG ) ) {
                    fileSinks.add( fileSink );
                }
            }
        }

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream( logConfig ));
            out.writeObject(fileSinks);
        } catch ( IOException ioe ) {
            logger.log( Level.WARNING, "Error storing log configuration '" + ExceptionUtils.getMessage( ioe ) + "'.", ExceptionUtils.getDebugException( ioe ) );
        } finally {
            ResourceUtils.closeQuietly(out);
        }
    }

    /**
     * Enable traffic logging if there are any (enabled) sinks
     */
    private void updateTrafficLoggingEnabledState( final List<MessageSink> sinks ) {
        if ( trafficLogger != null ) {
            boolean foundTrafficSink = false;
            for ( MessageSink messageSink : sinks ) {
                if ( messageSink instanceof MessageSinkSupport ) {
                    MessageSinkSupport mss = (MessageSinkSupport) messageSink;
                    if ( mss.isCategoryEnabled( MessageCategory.TRAFFIC ) ) {
                        foundTrafficSink = true;
                        break;
                    }
                }
            }

            if ( logger.isLoggable( Level.CONFIG) ) {
                logger.log( Level.CONFIG, "Updating traffic logging enabled state, new value is " + foundTrafficSink +  ".");
            }
            trafficLogger.setEnabled( foundTrafficSink );
        }
    }

    /**
     * Add a configured console message sink to the sink list.
     *
     * Set a threshold based on the audit detail threshold.
     *
     * This ensures that any audits are logged to the console (if there is any console logging)
     */
    private void addConsoleSink( final List<MessageSink> sinks ) {
        SinkConfiguration.SeverityThreshold threshold = SinkConfiguration.SeverityThreshold.INFO;
        String thresholdConfig = serverConfig.getProperty( ServerConfigParams.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD );
        if ( thresholdConfig != null ) {
            threshold = toSeverityThreshold( thresholdConfig, threshold );
        }

        SinkConfiguration configuration = new SinkConfiguration();
        configuration.setName( "console" );
        configuration.setSeverity( threshold );
        configuration.setCategories( SinkConfiguration.CATEGORY_AUDITS );
        configuration.setType( SinkConfiguration.SinkType.FILE ); // it's fileish ...
        MessageSink sink = new ConsoleMessageSink( configuration );
        sinks.add( sink );
    }

    /**
     * Check if old cluster properties configured traffic logger should be added
     * to the sink list.
     *
     * This will add a sink if there is no traffic logger in the config list and
     * there is one enabled by cluster properties.
     */
    private void processOldTrafficLoggerConfig( final List<MessageSink> sinks ) {
        // check for configured traffic sink
        boolean foundTrafficSink = false;
        for ( MessageSink messageSink : sinks ) {
            if ( messageSink instanceof MessageSinkSupport ) {
                MessageSinkSupport mss = (MessageSinkSupport) messageSink;
                if ( mss.isCategoryEnabled( MessageCategory.TRAFFIC ) ) {
                    foundTrafficSink = true;
                    break;
                }
            }
        }

        if ( !foundTrafficSink ) {
            // create sink for traffic logs
            FileMessageSink sink = buildClusterPropertyConfiguredTrafficSink();
            if ( sink != null ) {
                if ( logger.isLoggable(Level.CONFIG) )
                    logger.log(Level.CONFIG, "Installing traffic log sink for cluster properties settings.");
                sinks.add( sink );
            }
        }
    }

    /**
     * Build a MessageSink for the old traffic logger cluster properties config.
     *
     * This is for backwards compatiblity only. This should not generally be used.
     */
    private FileMessageSink buildClusterPropertyConfiguredTrafficSink() {
        FileMessageSink sink = null;

        boolean enabled = serverConfig.getBooleanProperty( "trafficLoggerEnabled", false );
        String pattern = serverConfig.getProperty( "trafficLoggerPattern" );
        String limit = serverConfig.getProperty( "trafficLoggerLimit" );
        String count = serverConfig.getProperty( "trafficLoggerCount" );

        if ( enabled ) {
            try {
                String name = new File(pattern).getName();
                if ( name.endsWith( DEFAULT_TRAFFIC_LOG_NAME_POSTFIX ) ) {
                    name = name.substring( 0, name.length() - DEFAULT_TRAFFIC_LOG_NAME_POSTFIX.length() );
                }

                // cleanup any remaining wildcards ...
                name = name.replace('%', '_');

                SinkConfiguration configuration = new SinkConfiguration();
                configuration.setName( name );
                configuration.setType( SinkConfiguration.SinkType.FILE );
                configuration.setSeverity( SinkConfiguration.SeverityThreshold.INFO );
                configuration.setCategories( SinkConfiguration.CATEGORY_TRAFFIC_LOGS );
                configuration.setProperty( SinkConfiguration.PROP_FILE_MAX_SIZE, limit );
                configuration.setProperty( SinkConfiguration.PROP_FILE_LOG_COUNT, count );
                configuration.setProperty( SinkConfiguration.PROP_FILE_FORMAT, SinkConfiguration.FILE_FORMAT_RAW );
                sink = new FileMessageSink( serverConfig, configuration );
            } catch (MessageSinkSupport.ConfigurationException ce) {
                logger.log( Level.WARNING, "Error creating traffic log for cluster properties.", ce);
            }
        }

        return sink;
    }

    /**
     * Load sink configurations
     */
    private Collection<SinkConfiguration> loadSinkConfigurations() {
        Collection<SinkConfiguration> sinkConfigurations = Collections.emptyList();

        try {
            sinkConfigurations = this.findAll();
        } catch ( FindException fe ) {
            logger.log(
                    Level.WARNING,
                    "Unexpected error loading messages sink configuration.",
                    fe );
        }

        return sinkConfigurations;
    }

    /**
     * Validate a SinkConfiguration
     */
    private boolean isValid( final SinkConfiguration configuration ) {
        boolean valid;

        // validate base configuration
        String name = configuration.getName();
        SinkConfiguration.SinkType type = configuration.getType();
        SinkConfiguration.SeverityThreshold severity = configuration.getSeverity();

        valid = name != null && type != null && severity != null &&
                ValidationUtils.isValidCharacters(name, ValidationUtils.ALPHA_NUMERIC + "-_");

        // perform type specific validation
        if ( valid ) {
            switch ( type ) {
                case FILE:
                    String format = configuration.getProperty( SinkConfiguration.PROP_FILE_FORMAT );
                    String fileCount = configuration.getProperty( SinkConfiguration.PROP_FILE_LOG_COUNT );
                    String fileLimit = configuration.getProperty( SinkConfiguration.PROP_FILE_MAX_SIZE );

                    valid = format != null && fileCount != null && fileLimit != null &&
                            ValidationUtils.isValidLong( fileCount, false, 1L, 1000L ) &&
                            ValidationUtils.isValidLong( fileLimit, false, 1L, 2L * ONE_GIGABYTE );

                    break;
                case SYSLOG:
                    String host = "syslog.l7tech.com"; // configuration.getProperty( SinkConfiguration.PROP_SYSLOG_HOST );
                    String port = "1999"; //configuration.getProperty( SinkConfiguration.PROP_SYSLOG_PORT );
                    String prot = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_PROTOCOL );
                    String facility = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_FACILITY );

                    valid = host != null && port != null && prot != null && facility != null &&
                            ValidationUtils.isValidDomain( host ) &&
                            ValidationUtils.isValidInteger( port, false, 1, 0xFFFF ) &&
                            ValidationUtils.isValidInteger( facility, false, 0, 124) &&
                            isValidProtocol( prot );

                    if ( valid ) {
                        String charset = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_CHAR_SET );
                        if ("LATIN-1".equals(charset))
                            valid = Charset.isSupported("ISO-8859-1");
                        else
                            valid = Charset.isSupported(charset);
                    }

                    break;
            }
        }

        return valid;
    }

    /**
     * Build a MessageSink for the given configuration
     */
    private MessageSinkSupport buildSinkForConfiguration( final SinkConfiguration configuration ) {
        MessageSinkSupport sink = null;
        SinkConfiguration.SinkType type = configuration.getType();

        try {
            switch ( type ) {
                case FILE:
                    sink = new FileMessageSink( serverConfig, configuration );
                    break;
                case SYSLOG:
                    sink = new SyslogMessageSink( serverConfig, configuration, syslogManager );
                    break;
                default:
                    logger.log(Level.WARNING, "Ignoring unknown type of sink ''{0}''.", type);
                    break;
            }
        } catch (MessageSinkSupport.ConfigurationException ce) {
            logger.log(Level.WARNING, "Error creating log sink '"+configuration.getName()+"'.", ce);
        }

        return sink;
    }

    /**
     * Get the file system space required for the given configuration
     *
     * The configuration should be validated before calling this method.
     */
    private long getReservedSpace( final SinkConfiguration configuration ) {
        long space = 0;

        SinkConfiguration.SinkType type = configuration.getType();
        if ( type != null ) {
            switch ( type ) {
                case FILE:
                    long configCount = Long.parseLong( configuration.getProperty( SinkConfiguration.PROP_FILE_LOG_COUNT ) );
                    long configLimit = Long.parseLong( configuration.getProperty( SinkConfiguration.PROP_FILE_MAX_SIZE ) );
                    space += ( configCount * configLimit * 1024L );
                    break;
            }
        }

        return space;
    }

    /**
     * Check if the given string is a recognised SyslogProtocol value.
     */
    private boolean isValidProtocol( final String protocol ) {
        boolean valid;

        try {
            SyslogProtocol.valueOf( protocol );
            valid = true;
        } catch ( IllegalArgumentException iae ) {
            valid = false;
        }

        return valid;
    }

    /**
     * Convert the given level/severity string to a SeverityThreshold if possible
     */
    private SinkConfiguration.SeverityThreshold toSeverityThreshold( final String value,
                                                                     final SinkConfiguration.SeverityThreshold defaultValue ) {
        SinkConfiguration.SeverityThreshold threshold = defaultValue;

        try {
            threshold = SinkConfiguration.SeverityThreshold.valueOf( value );
        } catch ( IllegalArgumentException iae ) {
            // use the default
        }

        return threshold;
    }

    /**
     * Close the list of resources after a given timeout period.
     *
     * This method should not be used often.
     *
     * @param closeList list of Closeable resources
     * @param time the amount of time (millis) to wait before closing the resources
     */
    private void delayedClose( final Closeable[] closeList, final long time ) {
        Thread cleanup = new Thread( new Runnable(){
            @Override
            public void run() {
                try {
                    Thread.sleep(time);
                    for (Closeable tobeClosed : closeList)
                        ResourceUtils.closeQuietly( tobeClosed );

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "SinkTest-Cleanup" );
        cleanup.setDaemon(true);
        cleanup.start();
    }

    private static final class DelegatingMessageSink implements MessageSink {
        private final MessageSink sink;

        private DelegatingMessageSink( final MessageSink sink ) {
            this.sink = sink;
        }

        @Override
        public void message(final MessageCategory category, final LogRecord record) {
            sink.message(category, record);
        }

        @Override
        public void close() {
        }
    }
}
