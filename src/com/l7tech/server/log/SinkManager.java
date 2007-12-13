package com.l7tech.server.log;

import com.l7tech.common.log.SinkConfiguration;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.ValidationUtils;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.SyslogEvent;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.log.syslog.SyslogConnectionListener;
import com.l7tech.server.log.syslog.SyslogProtocol;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.StringReader;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * Provides the ability to do CRUD operations on SinkConfiguration rows in the database.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SinkManager
        extends HibernateEntityManager<SinkConfiguration, EntityHeader>
        implements ApplicationContextAware, ApplicationListener, PropertyChangeListener {
    //- PUBLIC

    public SinkManager( final ServerConfig serverConfig,
                        final SyslogManager syslogManager,
                        final TrafficLogger trafficLogger ) {
        if ( serverConfig == null ) throw new IllegalArgumentException("serverConfig must not be null");
        if ( syslogManager == null ) throw new IllegalArgumentException("syslogManager must not be null");

        this.serverConfig = serverConfig;
        this.syslogManager = syslogManager;
        this.trafficLogger = trafficLogger;
    }

    /**
     * Get the file storage allocation for logs.
     *
     * @return The size in bytes.
     */
    public long getMaximumFileStorageSpace() {
        long storage = DEFAULT_FILE_SPACE_LIMIT;

        String value = serverConfig.getPropertyCached( SCPROP_FILE_LIMIT, 30000L );
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

    public Class<SinkConfiguration> getImpClass() {
        return SinkConfiguration.class;
    }

    public Class<SinkConfiguration> getInterfaceClass() {
        return SinkConfiguration.class;
    }

    public String getTableName() {
        return "sink_config";
    }

    /**
     * Test the given sinkConfiguration (useful for syslog)
     *
     * @param sinkConfiguration The configuration to test.
     * @param testMessage The message to send.
     * @return True if the message may have been sent
     */
    public boolean test(final SinkConfiguration sinkConfiguration, final String testMessage) {
        boolean success = false;

        if ( sinkConfiguration != null ) {
            MessageSinkSupport sink = buildSinkForConfiguration( sinkConfiguration );
            if ( sink != null ) {
                try {
                    for ( MessageCategory category : MessageCategory.values() ) {
                        if ( sink.isCategoryEnabled(category) ) {
                            LogRecord testRecord = new LogRecord( Level.INFO,
                                    testMessage==null ? "Test message." : testMessage );

                            // up level if required for message to be sent
                            Level testLevel = Level.parse(sinkConfiguration.getSeverity().toString());
                            if ( testLevel.intValue() > Level.INFO.intValue() ) {
                                testRecord.setLevel(testLevel);
                            }

                            sink.message(category, testRecord);

                            break;
                        }
                    }
                } finally {
                    // we don't know how long it will take to send a message so leave open for 60 secs
                    delayedClose(sink, 60000);                    
                }
            }
        }

        return success;
    }

    public void setApplicationContext(final ApplicationContext applicationContext) {
        if ( this.applicationContext == null )
            this.applicationContext = applicationContext;
    }

    public void onApplicationEvent(final ApplicationEvent event) {
        if ( event instanceof EntityInvalidationEvent ) {
            EntityInvalidationEvent evt = (EntityInvalidationEvent) event;

            if ( SinkConfiguration.class.isAssignableFrom(evt.getEntityClass()) ) {
                rebuildLogSinks();
            }
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        updateLogLevels( (String)evt.getOldValue(), (String)evt.getNewValue() );    
    }

    public MessageSink getPublishingSink() {
        return publishingSink;
    }

    //- PROTECTED

    @Override
    protected void initDao() {
        installHandlers();
        updateLogLevels(null, null);
        installLogConfigurationListener();
        installConnectionListener();
        rebuildLogSinks();

        // Log a message to both startup and regular logs
        logger.info("Redirecting logging to configured log sinks.");
        StartupHandler.notifyStarted();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SinkManager.class.getName());

    private static final String SCPROP_LOG_LEVELS = "logLevels";
    private static final String SCPROP_FILE_LIMIT = "logFileSpaceMax";
    private static final String DEFAULT_TRAFFIC_LOG_NAME_POSTFIX = "_%g_%u.log";
    private static final String TRAFFIC_LOGGER_NAME = "com.l7tech.traffic";
    private static final long ONE_GIGABYTE = 1024L * 1024L * 1024L;
    private static final long DEFAULT_FILE_SPACE_LIMIT = ONE_GIGABYTE * 5L; //5GB

    private final DispatchingMessageSink dispatchingSink = new DispatchingMessageSink();
    private final MessageSink publishingSink = new DelegatingMessageSink(dispatchingSink);
    private final ServerConfig serverConfig;
    private final SyslogManager syslogManager;
    private final TrafficLogger trafficLogger;
    private ApplicationContext applicationContext;

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
     * Update the log levels with any customized properties.
     *
     * We detect if a log level has been removed so we can set it to it's parent value.
     */
    private void updateLogLevels(final String oldValue, final String newValue) {
        String loggingLevels = newValue;
        if ( loggingLevels == null )
            loggingLevels = serverConfig.getPropertyCached(SCPROP_LOG_LEVELS);

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
        syslogManager.setConnectionListener(new SyslogConnectionListener(){
            public void notifyConnected(final SocketAddress address) {
                fireEvent(address, true);
            }

            public void notifyDisconnected(final SocketAddress address) {
                fireEvent(address, false);
            }

            private void fireEvent(final SocketAddress address, final boolean connected) {
                if ( applicationContext != null ) {
                    applicationContext.publishEvent(
                            new SyslogEvent(SinkManager.this, address.toString(), connected));
                }
            }
        });
    }

    /**
     * Attach the configured sinks.
     */
    private void rebuildLogSinks() {
        List<MessageSink> sinks = new ArrayList();

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

        addConsoleSink( sinks );
        processOldTrafficLoggerConfig( sinks );
        updateTrafficLoggingEnabledState( sinks );

        // install new
        dispatchingSink.setMessageSinks( sinks );
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
     */
    private void addConsoleSink( final List<MessageSink> sinks ) {
        SinkConfiguration.SeverityThreshold threshold = SinkConfiguration.SeverityThreshold.INFO;
        String thresholdConfig = serverConfig.getPropertyCached( ServerConfig.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD );
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

        boolean enabled = serverConfig.getBooleanPropertyCached("trafficLoggerEnabled", false, 30000L);
        String pattern = serverConfig.getPropertyCached("trafficLoggerPattern");
        String limit = serverConfig.getPropertyCached("trafficLoggerLimit");
        String count = serverConfig.getPropertyCached("trafficLoggerCount");

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
        Collection<SinkConfiguration> sinkConfigurations = Collections.EMPTY_LIST;

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
        boolean valid = true;

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
                    String host = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_HOST );
                    String port = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_PORT );
                    String prot = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_PROTOCOL );
                    String facility = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_FACILITY );

                    valid = host != null && port != null && prot != null && facility != null &&
                            ValidationUtils.isValidDomain( host ) &&
                            ValidationUtils.isValidInteger( port, false, 1, 0xFFFF ) &&
                            ValidationUtils.isValidInteger( facility, false, 0, 124) &&
                            isValidProtocol( prot );

                    if ( valid ) {
                        String charset = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_CHAR_SET );
                        valid = Charset.isSupported( charset );
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
        boolean valid = false;

        try {
            SyslogProtocol.valueOf( protocol );
            valid = true;
        } catch ( IllegalArgumentException iae ) {
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
     * Close the given sink after a timeout.
     *
     * This method should not be used often. 
     */
    private void delayedClose( final MessageSink sink, final long time ) {
        Thread cleanup = new Thread( new Runnable(){
            public void run() {
                try {
                    Thread.sleep(time);
                    ResourceUtils.closeQuietly( sink );
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

        public void message(final MessageCategory category, final LogRecord record) {
            sink.message(category, record);
        }

        public void close() {            
        }
    }
}
