package com.l7tech.server.log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.StreamHandler;
import java.util.logging.LogRecord;
import java.util.List;
import java.util.ArrayList;

import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.server.ServerConfig;

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
            JdkLoggerConfigurator.configure(null, "com/l7tech/server/resources/logging.properties", null, false, false);

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
        ServerConfig serverConfig = ServerConfig.getInstance();
        String logConfigurationPath = serverConfig.getPropertyCached("configDirectory") + File.separator + "ssglog.properties";

        if ( new File(logConfigurationPath).exists() ) {
            JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/server/resources/logging.properties", logConfigurationPath, !isJdkInit, !isJdkInit);
        } else {
            // specify "ssglog.properties" twice since the non-default one can be overridden by
            // a system property.
            JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/server/resources/logging.properties", "com/l7tech/server/resources/logging.properties", !isJdkInit, !isJdkInit);
        }

        boolean logToConsole = SyspropUtil.getBoolean(PARAM_LOG_TO_CONSOLE);
        if ( logToConsole ) {
            addSystemErrorHandler();
        }

        loadSerializedConfig();
    }

    /**
     * Attemt to start the last known set of appenders from the sink configuration.
     *
     * If there is no last known configuration then install the shipped default.
     */
    private static void loadSerializedConfig() {
        List<LogFileConfiguration> logFileConfigurations = new ArrayList<LogFileConfiguration>();

        ServerConfig serverConfig = ServerConfig.getInstance();
        File varDir = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_VAR_DIRECTORY, true);
        File logConfig = new File( varDir, LogUtils.LOG_SER_FILE );
        if ( logConfig.exists() ) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream( logConfig ));
                Object object = in.readObject();
                if ( object instanceof List ) {
                    for ( Object item : (List) object ) {
                        if ( item instanceof LogFileConfiguration ) {
                            logFileConfigurations.add((LogFileConfiguration) item);
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
                        LogUtils.getLogFilePattern(serverConfig, SyspropUtil.getString(PARAM_LOG_DEFAULT_NAME, DEFAULT_NAME), null),
                        SyspropUtil.getInteger(PARAM_LOG_DEFAULT_LIMIT, DEFAULT_LIMIT),
                        SyspropUtil.getInteger(PARAM_LOG_DEFAULT_COUNT, DEFAULT_COUNT),
                        SyspropUtil.getBoolean(PARAM_LOG_DEFAULT_APPEND, DEFAULT_APPEND),
                        Level.INFO.intValue(),
                        LogUtils.DEFAULT_LOG_FORMAT_STANDARD) );
            } catch ( IOException ioe ) {
                // don't log while initializing logging, could use system.err?
            }
        }

        if ( !logFileConfigurations.isEmpty() ) {
            Logger rootLogger = Logger.getLogger("");
            for ( LogFileConfiguration logFileConfiguration : logFileConfigurations ) {
                try {
                    rootLogger.addHandler( logFileConfiguration.buildFileHandler() );
                } catch ( IOException ioe ) {
                    // ok
                }
            }
        }
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
