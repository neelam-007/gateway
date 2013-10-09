package com.l7tech.server.log;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;

/**
 * Configuration class for JDK logging on the Gateway.
 *
 * @author Steve Jones
 */
public class JdkLogConfig {

    //- PUBLIC

    /**
     * System property to enable logging to console for "startup" messages.
     */
    public static final String PARAM_LOG_TO_CONSOLE = "com.l7tech.server.log.console";

    /**
     * System properties that can be used to customize the log settings. Note that these are only
     * applicable the first time a new installation is run so there should be no reason to use.
     */
    public static final String PARAM_LOG_DEFAULT_NAME = "com.l7tech.server.log.default.name";
    public static final String PARAM_LOG_DEFAULT_LIMIT = "com.l7tech.server.log.default.limit";
    public static final String PARAM_LOG_DEFAULT_COUNT = "com.l7tech.server.log.default.count";
    public static final String PARAM_LOG_DEFAULT_APPEND = "com.l7tech.server.log.default.append";

    /**
     * Invoke logging configuration.
     *
     * <p>This constructor is suitable for use with java.util.logging.</p>
     *
     * <pre>
     *   java.util.logging.config.class = com.l7tech.server.log.JdkLogConfig
     * </pre>
     *
     * <p>When used in this way the JDK out/err streams are redirected to
     * logging.</p>
     *
     * <p>You can output startup logs to the console by setting:</p>
     *
     * <pre>
     *   com.l7tech.server.log.console = true
     * </pre>
     *
     * <p>WARNING: Do not use this at the same time as a console handler!</p>
     */
    public JdkLogConfig() {
        try {
            removeConsoleHandlers();  // ensure no console handlers before we capture std out/err
            captureSystemStreams();

            // This ensures logging of any error when loading server config
            JdkLoggerConfigurator.configure(null, "com/l7tech/server/resources/logging.properties", null);

            // Init logging based config
            initLogging(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configured logging from classpath / file.
     *
     * <p>This will start the daemon process that checks for log file changes.</p>
     */
    public static void configureLogging() {
        synchronized ( loggingLock ) {
            if ( !loggingConfigured ) {
                loggingConfigured = true;
                initLogging(false);
            }
        }
    }

    //- PRIVATE

    private static final String DEFAULT_NAME = "ssg";
    private static final int DEFAULT_LIMIT = 20480000;
    private static final int DEFAULT_COUNT = 10;
    private static final boolean DEFAULT_APPEND = true;

    private static final Object loggingLock = new Object();
    private static boolean loggingConfigured = false;

    private static void initLogging(final boolean isJdkInit) {
        final Config config = ConfigFactory.getCachedConfig();
        final String logConfigurationPath = config.getProperty( "configDirectory" ) + File.separator + "ssglog.properties";

        final Runnable configCallback = new Runnable(){
            @Override
            public void run() {
                boolean logToConsole = SyspropUtil.getBoolean( PARAM_LOG_TO_CONSOLE, false );
                if ( logToConsole ) {
                    addSystemErrorHandler();
                    //Remove the GatewayRootLoggingHandler from the root loggers. Leaving it here will result in double logging.
                    Logger rootLogger = Logger.getLogger("");
                    for ( Handler handler : rootLogger.getHandlers() ) {
                        if ( handler instanceof GatewayRootLoggingHandler) {
                            rootLogger.removeHandler( handler );
                        }
                    }
                }

                loadSerializedConfig();
            }
        };

        final String logClassname = !isJdkInit ? "com.l7tech.logging" : null;

        if ( new File(logConfigurationPath).exists() ) {
            JdkLoggerConfigurator.configure(logClassname, "com/l7tech/server/resources/logging.properties", logConfigurationPath, !isJdkInit, configCallback);
        } else {
            // specify "ssglog.properties" twice since the non-default one can be overridden by
            // a system property.
            JdkLoggerConfigurator.configure(logClassname, "com/l7tech/server/resources/logging.properties", "com/l7tech/server/resources/logging.properties", !isJdkInit, configCallback);
        }
    }

    /**
     * Attemt to start the last known set of appenders from the sink configuration.
     *
     * If there is no last known configuration then install the shipped default.
     */
    private static void loadSerializedConfig() {
        List<LogFileConfiguration> logFileConfigurations = new ArrayList<LogFileConfiguration>();

        ServerConfig serverConfig = ServerConfig.getInstance();
        File varDir = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        int level = LogUtils.readLoggingThreshold( "sink.level" );
        File logConfig = new File( varDir, LogUtils.LOG_SER_FILE );
        if ( logConfig.exists() ) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream( logConfig ));
                Object object = in.readObject();
                if ( object instanceof List ) {
                    for ( Object item : (List) object ) {
                        if ( item instanceof LogFileConfiguration ) {
                            LogFileConfiguration config = (LogFileConfiguration) item;
                            if ( level != 0 ) {
                                config = new LogFileConfiguration( config, level );
                            }
                            logFileConfigurations.add( config );
                        }
                    }
                }
            } catch ( IOException ioe ) {
                // Ok
            } catch ( ClassNotFoundException cnfe ) {
                // Ok
            } finally {
                ResourceUtils.closeQuietly(in);
            }
        } else {
            // fallback to default configuration
            try {
                logFileConfigurations.add( new LogFileConfiguration(
                        LogUtils.getLogFilePattern(serverConfig, SyspropUtil.getString(PARAM_LOG_DEFAULT_NAME, DEFAULT_NAME), null, true),
                        SyspropUtil.getInteger(PARAM_LOG_DEFAULT_LIMIT, DEFAULT_LIMIT),
                        SyspropUtil.getInteger(PARAM_LOG_DEFAULT_COUNT, DEFAULT_COUNT),
                        SyspropUtil.getBoolean(PARAM_LOG_DEFAULT_APPEND, DEFAULT_APPEND),
                        level != 0 ? level : Level.INFO.intValue(),
                        LogUtils.DEFAULT_LOG_FORMAT_STANDARD,
                        null,
                        false,
                        null)
                        );
            } catch ( IOException ioe ) {
                // don't log while initializing logging, could use system.err?
            }
        }

        if ( !logFileConfigurations.isEmpty() ) {
            Logger rootLogger = Logger.getLogger("");
            //Save the root logger that was used.
            initialRootLogger.set(rootLogger);
            //save the handlers used so that they can be used by the GatewayRootLoggingHandler
            final List<Handler> handlers = new ArrayList<>();
            for ( LogFileConfiguration logFileConfiguration : logFileConfigurations ) {
                try {
                    final Handler handler = logFileConfiguration.buildFileHandler();
                    handlers.add(handler);
                    rootLogger.addHandler(handler);
                } catch ( IOException ioe ) {
                    // ok
                }
            }
            initialRootHandlers.set(handlers);
        }
    }

    // Starting with jdk 1.7_u25 the are 2 different loggerContexts created one when the server is starting up and
    // one is created when the application context is refreshed. Because of this there will be two root loggers, one in
    // each context. Once the SinkManager is initialized it will need to remove any StartupAwareHandler from the root
    // loggers and so will need to access the initial root logger to remove this.
    private static final AtomicReference<Logger> initialRootLogger = new AtomicReference<>();

    /**
     * Returns the root logger used when the server starts up. (Before the application context is created)
     * @return The root logger used when the server starts up
     */
    public static Logger getInitialRootLogger(){
        return initialRootLogger.get();
    }

    /**
     * This allows the root logging handlers used to be accessed from the GatewayRootLoggingHandler
     */
    private static final AtomicReference<List<Handler>> initialRootHandlers = new AtomicReference<>(Collections.<Handler>emptyList());
    protected static List<Handler> getInitialRootHandlers(){
        return initialRootHandlers.get();
    }

    /**
     * Capture System.X streams and send to logging.
     */
    private static void captureSystemStreams() {
        //noinspection IOResourceOpenedButNotSafelyClosed
        System.setOut(new LoggingPrintStream(Logger.getLogger("STDOUT"), Level.INFO));
        //noinspection IOResourceOpenedButNotSafelyClosed
        System.setErr(new LoggingPrintStream(Logger.getLogger("STDERR"), Level.WARNING));
    }

    /**
     *
     */
    private static void addSystemErrorHandler() {
        try {
            Handler handler = new SystemErrorStreamHandler();
            handler.setLevel(Level.CONFIG);
            Logger.getLogger("").addHandler( handler );
        } catch ( Exception e ) {
            // don't think we want to log here
        }
    }

    /**
     *
     */
    private static void removeConsoleHandlers() {
        Logger rootLogger = Logger.getLogger("");
        for ( Handler handler : rootLogger.getHandlers() ) {
            if ( handler instanceof ConsoleHandler ) {
                rootLogger.removeHandler( handler );
            }
        }
    }

    /**
     * Stream handler that logs to the err file descriptor (so work when System.err is redirected)
     * and removes itself from the logging configuration when the server is started up.
     */
    private static class SystemErrorStreamHandler extends StreamHandler implements StartupAwareHandler {
        private static final OutputStream out = new FileOutputStream(FileDescriptor.err);

        public SystemErrorStreamHandler() {
            super();
            setOutputStream( out );
        }

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        @Override
        public synchronized void close() throws SecurityException {
            flush();
        }

    }
}
