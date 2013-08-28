package com.l7tech.server.log;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.gateway.common.cluster.ClusterContext;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.log.*;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterContextFactory;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.event.EntityClassEvent;
import com.l7tech.server.event.system.SyslogEvent;
import com.l7tech.server.log.syslog.SyslogConnectionListener;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.TestingSyslogManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.Subject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import static com.l7tech.objectmodel.EntityType.CLUSTER_INFO;
import static com.l7tech.objectmodel.EntityType.LOG_SINK;
import static com.l7tech.util.Functions.grepFirst;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.optional;
import static java.util.Collections.singletonList;

/**
 *
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SinkManagerImpl
        extends HibernateEntityManager<SinkConfiguration, EntityHeader>
        implements SinkManager, PropertyChangeListener {

    //- PUBLIC

    public SinkManagerImpl( final ServerConfig serverConfig,
                            final SyslogManager syslogManager,
                            final TrafficLogger trafficLogger,
                            final ApplicationEventProxy eventProxy,
                            final ClusterInfoManager clusterInfoManager,
                            final ClusterContextFactory clusterContextFactory,
                            final RoleManager roleManager ) {
        if ( serverConfig == null ) throw new IllegalArgumentException("serverConfig must not be null");
        if ( syslogManager == null ) throw new IllegalArgumentException("syslogManager must not be null");

        this.serverConfig = serverConfig;
        this.syslogManager = syslogManager;
        this.trafficLogger = trafficLogger;
        this.clusterInfoManager = clusterInfoManager;
        this.clusterContextFactory = clusterContextFactory;
        this.roleManager = roleManager;

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

        String value = serverConfig.getProperty(SCPROP_FILE_LIMIT);
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
        long reservedSpace = 0L;

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
                        Level testLevel = sinkConfiguration.getSeverity().toLoggingLevel();
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
    public void createRoles(SinkConfiguration entity) throws SaveException {
        for ( final Role role : createRolesForSink( entity ) ) {
            logger.info("Creating new Role: " + role.getName());
            roleManager.save(role);
        }
    }

    public static Collection<Role> createRolesForSink( final SinkConfiguration entity ) {
        String nameForRole = TextUtils.truncStringMiddle( entity.getName(), 50 );
        String name = MessageFormat.format(ROLE_READ_NAME_PATTERN, nameForRole, entity.getGoid());

        Role role = new Role();
        role.setName(name);

        role.addEntityPermission(READ, LOG_SINK, entity.getId());
        role.addEntityPermission(READ, CLUSTER_INFO, null);

        role.addEntityOtherPermission(LOG_SINK, entity.getId(), OtherOperationName.LOG_VIEWER.getOperationName());

        // Set role as entity-specific
        role.setEntityType(LOG_SINK);
        role.setEntityGoid(entity.getGoid());
        role.setDescription("Users assigned to the {0} role have the ability to read the log sink and any associated log files.");

        return singletonList(role);
    }

    @Override
    public void updateRoles( final SinkConfiguration entity ) throws UpdateException {
        //sinks cannot currently be renamed, no update necessary
    }

    @Override
    public Goid save(SinkConfiguration entity) throws SaveException {
        Goid goid =  super.save(entity);
        entity.setGoid(goid);
        createRoles(entity);
        return goid;
    }

    @Override
    public Collection<LogFileInfo> findAllFilesForSinkByNode( @NotNull final String nodeId, final Goid sinkId) throws FindException {
        final Collection<LogFileInfo> files = new ArrayList<LogFileInfo>();
        if(isThisNodeMe(nodeId)){
            final SinkConfiguration config = findByPrimaryKey(sinkId);
            if( config != null && config.getType().equals(SinkConfiguration.SinkType.FILE) ){
                try {
                    String pattern = getSinkFilePattern(config);
                    String folder = pattern.substring(0,pattern.lastIndexOf("/"));
                    String filePattern = pattern.substring(pattern.lastIndexOf("/")+1);
                    if(config.isRollingEnabled()){
                        filePattern = config.getName() + "." + config.getRollingInterval().getPattern().replaceAll("\\w{2,4}", "\\\\d{2,4}") + ".log";
                    }
                    else {
                        filePattern = filePattern.replace("%u","[0-9]");
                        filePattern = filePattern.replace("%g","[0-9]");
                        filePattern = filePattern.replace("%%","%");
                    }
                    final Pattern p = Pattern.compile(filePattern);
                    File f = new File(folder);
                    if( f.isDirectory() ){
                        final File[] fileList = f.listFiles(new FilenameFilter(){
                            @Override
                            public boolean accept(File dir, String name) {
                                Matcher result = p.matcher(name);
                                return result.matches();
                            }
                        });
                        files.addAll( map( Arrays.asList(fileList), new Unary<LogFileInfo,File>(){
                            @Override
                            public LogFileInfo call( final File file ) {
                                return new LogFileInfo( file );
                            }
                        } ) );
                    }
                } catch (IOException e) {
                    logger.warning("Log sink files not found for sink:"+config.getName());
                }
            }
            return files;

        } else {
            final Option<Collection<LogFileInfo>> result = doWithLogAccessAdmin( nodeId, new UnaryThrows<Collection<LogFileInfo>,LogAccessAdmin,FindException>(){
                @Override
                public Collection<LogFileInfo> call( final LogAccessAdmin logAccessAdmin ) throws FindException {
                    return logAccessAdmin.findAllFilesForSinkByNode( nodeId, sinkId );
                }
            } );
            if ( result.isSome() ) files.addAll( result.some() );
        }

        return files;
    }

    @Override
    public LogSinkData getSinkLogs( final String nodeId,
                                    final Goid sinkId,
                                    final String file,
                                    final LogSinkQuery query) throws FindException {
        LogSinkData data = null;

        if( isThisNodeMe(nodeId) )
        {
            InputStream fin = null;
            TruncatingInputStream in = null;
            PoolByteArrayOutputStream bs = null;
            GZIPOutputStream out = null;
            try {
                final SinkConfiguration sinkConfig = findByPrimaryKey(sinkId);
                if( sinkConfig != null && SinkConfiguration.SinkType.FILE.equals(sinkConfig.getType()) ){
                    final File logFileWithPattern = new File(getSinkFilePattern(sinkConfig));
                    final String fileDirectory = logFileWithPattern.getParent();
                    String fileRegex = logFileWithPattern.getName()
                            .replace( "%u", "[0-9]" )
                            .replace( "%g", "[0-9]" );
                    if(sinkConfig.isRollingEnabled()){
                        fileRegex = sinkConfig.getName() + "." + sinkConfig.getRollingInterval().getPattern().replaceAll("\\w{2,4}", "\\\\d{2,4}") + ".log";
                    }
                    if ( file.matches( fileRegex ) ) {
                        final File logFile = new File(fileDirectory, file);
                        boolean isRotated = isRotated(query.getLastRead(), file,fileDirectory,logFileWithPattern.getName());
                        long startPoint =  isRotated ? 0 : query.getStartPosition();
                        if( query.isFromEnd() ){
                            startPoint = Math.max(0L,logFile.length() - startPoint - 16384L);
                        }
                        String cat = sinkConfig.getCategories();
                        boolean skip = false;
                        if(cat != null && cat.equals(SinkConfiguration.CATEGORY_SSPC_LOGS)){
                            long toRead = query.isFromEnd() ? logFile.length() - startPoint : (TruncatingInputStream.DEFAULT_SIZE_LIMIT / 4L);
                            final String[] cmd = new String[]{"sudo", "/opt/SecureSpan/Appliance/libexec/ssg-viewlog.sh",
                                    logFile.getAbsolutePath(),
                                    String.valueOf(startPoint),
                                    String.valueOf(toRead)};
                            final ProcResult result = ProcUtils.exec(cmd);
                            fin = new ByteArrayInputStream(result.getOutput());
                            skip = true;
                        }
                        else {
                            fin = new FileInputStream(logFile);
                        }
                        long skipped = 0;
                        if(!skip){
                            skipped = fin.skip( startPoint );
                        }
                        if (!skip && skipped < startPoint ) {
                            data = new LogSinkData(new byte[0],-1L,false,0,0);
                        } else {
                            in = new TruncatingInputStream(fin);
                            bs = new PoolByteArrayOutputStream((int)(TruncatingInputStream.DEFAULT_SIZE_LIMIT / 4L));
                            out = new GZIPOutputStream(bs, 16384){{
                                def = new Deflater(Deflater.BEST_SPEED, true);
                            }};

                            IOUtils.copyStream(in,out);
                            long readAt = System.currentTimeMillis();

                            long lastLocation = in.getPosition();
                            if ( lastLocation >= 0L ) {
                                lastLocation += skipped;
                            }
                            out.finish();

                            data = new LogSinkData(bs.toByteArray(),lastLocation,isRotated,readAt,logFile.length());
                        }
                    } else {
                        logger.warning( "Attempt to read log file " + file + ", not matching sink file pattern " + fileRegex );
                    }
                }
            } catch ( final FileNotFoundException e ) {
                logger.info("Log file not found: "+ e.getMessage());
            } catch ( final IOException e ) {
                logger.log( Level.WARNING, "Error reading from log file: "+ e.getMessage(), ExceptionUtils.getDebugException( e ) );
            }finally {
                ResourceUtils.closeQuietly(fin, in, bs, out);
            }
        } else {
            data = doWithLogAccessAdmin( nodeId, new UnaryThrows<LogSinkData,LogAccessAdmin,FindException>(){
                @Override
                public LogSinkData call( final LogAccessAdmin logAccessAdmin ) throws FindException {
                    return logAccessAdmin.getSinkLogs( nodeId, sinkId, file, query);
                }
            } ).toNull();
        }

        return data;
    }

    private boolean isRotated(long lastRead, String fileName, String fileDirectory, String filePattern){
        if(lastRead == 0) return false;
        try{
            int start  = filePattern.indexOf("%g");
            if(start < 0) return false;
            Pattern pattern = Pattern.compile("[0-9]");
            Matcher matcher = pattern.matcher(fileName);
            if(matcher.find(start))
            {
                String indexStr = matcher.group();
                int index = Integer.parseInt(indexStr);
                String nextFileName = fileName.replaceFirst(indexStr,Integer.toString(index+1));
                final File logFile = new File(fileDirectory, nextFileName);
                if(logFile!=null){
                    return logFile.lastModified()>lastRead;
                }
            }
        }catch (NumberFormatException e){
            return false;
        }


        return false;
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

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(SinkManager.class.getName());

    private static final String SCPROP_LOG_LEVELS = "logLevels";
    private static final String SCPROP_FILE_LIMIT = "logFileSpaceMax";
    private static final String DEFAULT_TRAFFIC_LOG_NAME_POSTFIX = "_%g_%u.log";
    private static final long ONE_GIGABYTE = 1024L * 1024L * 1024L;
    private static final long DEFAULT_FILE_SPACE_LIMIT = ONE_GIGABYTE * 5L; //5GB

    private static final String ROLE_NAME_TYPE_SUFFIX = LogSinkAdmin.ROLE_NAME_TYPE_SUFFIX;
    private static final String ROLE_READ_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX_READ + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    private final DispatchingMessageSink dispatchingSink = new DispatchingMessageSink();
    private final MessageSink publishingSink = new DelegatingMessageSink(dispatchingSink);
    private final ServerConfig serverConfig;
    private final SyslogManager syslogManager;
    private final TrafficLogger trafficLogger;
    private final ClusterInfoManager clusterInfoManager;
    private final ClusterContextFactory clusterContextFactory;
    private final RoleManager roleManager;
    @SuppressWarnings({ "MismatchedQueryAndUpdateOfCollection" })
    private final Collection<Logger> configuredLoggers = new ArrayList<Logger>(); // hold a reference to prevent GC


    /**
     * Handle application event
     */
    private void handleEvent(final ApplicationEvent event) {
        if ( event instanceof EntityClassEvent) {
            EntityClassEvent evt = (EntityClassEvent) event;

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

    private int getClusterPort() {
        return ConfigFactory.getIntProperty(ServerConfigParams.PARAM_CLUSTER_PORT, 2124);
    }

    private boolean isThisNodeMe(final String nodeId) {
        return clusterInfoManager == null || nodeId.equals(clusterInfoManager.thisNodeId());
    }

    /**
     * Update the log levels with any customized properties.
     *
     * We detect if a log level has been removed so we can set it to it's parent value.
     */
    private void updateLogLevels(final String oldValue, final String newValue) {
        synchronized ( configuredLoggers ) {
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
                configuredLoggers.clear();
                for ( Map.Entry<Object,Object> property : levelProps.entrySet() ) {
                    String key = (String) property.getKey();
                    String value = (String) property.getValue();

                    if ( key.endsWith(".level") && properties.getProperty(key)==null) {
                        String name = key.substring(0, key.length()-6);
                        try {
                            Level configLevel = Level.parse(value);
                            Logger configLogger = Logger.getLogger(name);
                            configLogger.setLevel(configLevel);
                            configuredLoggers.add( configLogger ); // Hold a reference to prevent GC
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
                sinks.add(sink);
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

        boolean enabled = serverConfig.getBooleanProperty("trafficLoggerEnabled", false);
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
                    List<String> hostList = configuration.syslogHostList();
                    String prot = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_PROTOCOL );
                    String facility = configuration.getProperty( SinkConfiguration.PROP_SYSLOG_FACILITY );

                    valid = prot != null && facility != null &&
                            ValidationUtils.isValidInteger( facility, false, 0, 124) &&
                            isValidProtocol( prot ) &&
                            isValidHostList( hostList );

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
                    String cat = configuration.getCategories();
                    if(cat != null && !cat.equals(SinkConfiguration.CATEGORY_SSPC_LOGS)){
                        sink = new FileMessageSink( serverConfig, configuration );
                    }
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
        long space = 0L;

        SinkConfiguration.SinkType type = configuration.getType();
        if ( type != null ) {
            switch ( type ) {
                case FILE:
                    long configCount = Long.parseLong(configuration.getProperty(SinkConfiguration.PROP_FILE_LOG_COUNT));
                    long configLimit = Long.parseLong(configuration.getProperty(SinkConfiguration.PROP_FILE_MAX_SIZE));
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
     * Check if the given string is a valid list of host/port pairs.
     */
    private boolean isValidHostList( final List<String> hostList ) {
        boolean valid = true;

        if ( hostList == null || hostList.isEmpty() ) {
            valid = false;
        } else {
            for ( final String hostPort : hostList ) {
                final Pair<String,String> hostAndPort = InetAddressUtil.getHostAndPort( hostPort, null );

                if ( !ValidationUtils.isValidDomain( InetAddressUtil.stripIpv6Brackets( hostAndPort.left ) ) ||
                        !ValidationUtils.isValidInteger( hostAndPort.right, false, 1, 0xFFFF ) ) {
                    valid = false;
                    break;
                }
            }
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
            threshold = SinkConfiguration.SeverityThreshold.valueOf(value);
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

    private String getSinkFilePattern( final SinkConfiguration sinkConfig ) throws IOException {
        String filePattern;
        String cat = sinkConfig.getCategories();
        if(sinkConfig.isEnabled() && !cat.equals(SinkConfiguration.CATEGORY_SSPC_LOGS)){
            MessageSinkSupport sinkSupport = dispatchingSink.getMessageSink(sinkConfig);
            FileMessageSink fileSink = (FileMessageSink)sinkSupport;
            filePattern = fileSink.getFilePattern();
        } else {
            String filepath = sinkConfig.getProperty( "file.logPath" );
            if(cat != null && cat.equals(SinkConfiguration.CATEGORY_SSPC_LOGS)){
                filepath = ConfigFactory.getProperty("com.l7tech.server.base") + "/Controller/var/logs";
            }
            filePattern = LogUtils.getLogFilePattern(serverConfig, sinkConfig.getName(), filepath, false);
        }
        return filePattern;
    }

    private <R> Option<R> doWithLogAccessAdmin( final String nodeId,
                                                final UnaryThrows<R,LogAccessAdmin,FindException> callback ) throws FindException {
        Option<R> result = none();

        final long startTime = System.currentTimeMillis();
        final ClusterNodeInfo clusterNodeInfo = clusterInfoManager != null ? grepFirst(clusterInfoManager.retrieveClusterStatus(), new Unary<Boolean,ClusterNodeInfo>(){
            @Override
            public Boolean call( final ClusterNodeInfo clusterNodeInfo ) {
                return nodeId.equals( clusterNodeInfo.getNodeIdentifier() );
            }
        }) : null;
        if ( clusterContextFactory != null && clusterNodeInfo != null ) {
            try {
                result = optional( Subject.doAs( null, new PrivilegedExceptionAction<R>() {
                    @Override
                    public R run() throws Exception {
                        ClusterContext context = clusterContextFactory.buildClusterContext( clusterNodeInfo.getAddress(), getClusterPort() );
                        LogAccessAdmin laa = context.getLogAccessAdmin();
                        return callback.call( laa );
                    }
                } ) );
            }
            catch(PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if(cause instanceof FindException) {
                    throw (FindException) cause;
                }
                if(ExceptionUtils.causedBy(cause, ConnectException.class)) {
                    logger.log(Level.INFO, "Unable to connect to remote node '"+nodeId+"', to list/read logs.");
                } else {
                    logger.log(Level.WARNING, "Error during log list/read from remote node '"+nodeId+"'", cause);
                }
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected error during log list/read from remote node '"+nodeId+"':" + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        if(logger.isLoggable(Level.FINEST)) {
            logger.finest("Getting sink configurations from NODE took "
                    + (System.currentTimeMillis()-startTime) + "ms.");
        }

        return result;
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
