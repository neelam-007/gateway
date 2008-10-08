package com.l7tech.server.log;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.ConsoleHandler;

import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.server.ServerConfig;

/**
 * Configuration class for JDK logging on the Gateway.
 *
 * @author Steve Jones
 */
public class JdkLogConfig {

    //- PUBLIC

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
            boolean logToConsole = Boolean.getBoolean("com.l7tech.server.log.console");
            System.setProperty(StartupHandler.SYSPROP_LOG_TO_CONSOLE, "false");

            if ( !logToConsole ) {
                captureSystemStreams();
            }

            // This ensures logging of any error when loading server config
            JdkLoggerConfigurator.configure(null, "com/l7tech/server/resources/logging.properties", null, false, false);
            if ( logToConsole ) {
                if (!hasConsoleHandler())
                    addConsoleHandler();
            }

            // Init logging based config
            initLogging(true);

            if ( logToConsole ) {
                if (!hasConsoleHandler())
                    addConsoleHandler();
                captureSystemStreamsOnStarted();
            }
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
    private static void captureSystemStreamsOnStarted() {
        Logger.getLogger("").addHandler( new StreamCaptureHandler() );
    }

    /**
     *
     */
    private static void addConsoleHandler() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.CONFIG);
        Logger.getLogger("").addHandler( new ConsoleHandler() );            
    }

    /**
     *
     */
    private static boolean hasConsoleHandler() {
        boolean hasHandler = false;

        Logger rootLogger = Logger.getLogger("");
        for ( Handler handler : rootLogger.getHandlers() ) {
            if ( handler instanceof ConsoleHandler ) {
                hasHandler = true;
            }
        }

        return hasHandler;
    }

    /**
     *
     */
    private static void removeConsoleHandler() {
        Logger rootLogger = Logger.getLogger("");
        for ( Handler handler : rootLogger.getHandlers() ) {
            if ( handler instanceof ConsoleHandler ) {
                rootLogger.removeHandler( handler );
            }
        }
    }

    /**
     * Dummy handler that only captures
     */
    private static class StreamCaptureHandler extends Handler implements StartupAwareHandler {
        public void publish( final LogRecord record ) {}
        public void flush() { }
        public void close() throws SecurityException { }

        public void notifyStarted() {
            removeConsoleHandler();
            captureSystemStreams();
        }
    }
}
