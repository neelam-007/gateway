package com.l7tech.server.log;

import java.io.PrintStream;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l7tech.common.util.JdkLoggerConfigurator;
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
     * <p>WARNING: Do not use this at the same time as a console handler!</p>
     */
    public JdkLogConfig() {
        try {
            System.setProperty(StartupHandler.SYSPROP_LOG_TO_CONSOLE, "false");
            captureSystemStreams();

            // This ensures logging of any error when loading server config
            JdkLoggerConfigurator.configure(null, "ssglog.properties", null, false, false);

            // Init logging based on partion config
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

    private static final Object loggingLock = new Object();
    private static boolean loggingConfigured = false;

    private static void initLogging(final boolean isJdkInit) {
        ServerConfig serverConfig = ServerConfig.getInstance();
        String logConfigurationPath = serverConfig.getPropertyCached("configDirectory") + File.separator + "ssglog.properties";

        if ( new File(logConfigurationPath).exists() ) {
            JdkLoggerConfigurator.configure("com.l7tech.logging", "ssglog.properties", logConfigurationPath, !isJdkInit, !isJdkInit);
        } else {
            // specify "ssglog.properties" twice since the non-default one can be overridden by
            // a system property.
            JdkLoggerConfigurator.configure("com.l7tech.logging", "ssglog.properties", "ssglog.properties", !isJdkInit, !isJdkInit);
        }

        // set level to config to enable logging during startup (before any cluster property is read)
        Logger.getLogger("com.l7tech").setLevel(Level.CONFIG);
    }

    /**
     * Capture System.X streams and send to logging.
     */
    private static void captureSystemStreams() {
        //noinspection IOResourceOpenedButNotSafelyClosed
        System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("STDOUT"), Level.INFO)));
        //noinspection IOResourceOpenedButNotSafelyClosed
        System.setErr(new PrintStream(new LoggingOutputStream(Logger.getLogger("STDERR"), Level.WARNING)));
    }
}
