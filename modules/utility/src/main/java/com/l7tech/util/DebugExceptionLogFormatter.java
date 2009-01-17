package com.l7tech.util;

import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.Arrays;


/**
 * LogFormatter support class for suppression of exception stack trace for named loggers.
 *
 * <p>This class allows formatter to support our <code>com.l7tech.logging.debugExceptionLoggers</code>
 * logging property.</p>
 */
public abstract class DebugExceptionLogFormatter extends Formatter {

    //- PUBLIC

    public DebugExceptionLogFormatter(){
        configured = false;
        debugLoggers = Collections.emptySet();
    }

    //- PROTECTED

    /**
     * Exceptions are loggable unless the logger is listed as a debug exception
     * logger and debug logging is not enabled.
     *
     * @param loggerName The logger to check.
     * @return true if exceptions should be logged for the given logger
     */
    protected boolean isLoggableException( final String loggerName ) {
        initConfig();        
        return JdkLoggerConfigurator.debugState() || !debugLoggers.contains(loggerName);
    }

    //- PRIVATE

    private static final String LOGGING_PROP_DEBUG_EX_LOGGERS = "com.l7tech.logging.debugExceptionLoggers";

    private boolean configured;
    private Set<String> debugLoggers;

    /**
     * Read the configuration for the format
     */
    private void initConfig() {
        if (!configured) {
            configured = true;
            LogManager manager = LogManager.getLogManager();
            String debugLoggerText = manager.getProperty(LOGGING_PROP_DEBUG_EX_LOGGERS);  
            if ( debugLoggerText != null ) {
                debugLoggers = new HashSet<String>( Arrays.asList( debugLoggerText.split("[\\s]{1,128}") ) );
            }
        }
    }
}
