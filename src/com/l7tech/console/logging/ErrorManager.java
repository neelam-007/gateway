package com.l7tech.console.logging;

import com.l7tech.common.util.Locator;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class ErrorManager is the console error handler. It handles user notifying
 * anbd error logging.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public abstract class ErrorManager {
    private static final Logger log = Logger.getLogger(ErrorManager.class.getName());

    private static ErrorManager instance;
    private static final ErrorManager EMPTY = new NullErrorManager();

    public static ErrorManager getDefault() {
        if (instance != null) {
            return instance;
        }
        instance = (ErrorManager)Locator.getDefault().lookup(ErrorManager.class);
        if (instance == null) {
            instance = EMPTY;
        }
        return instance;
    }

    /**
     * Log and notify the user about the problem or error
     *
     * @param level the log level
     * @param t the throwable with the
     * @param message the message
     */
    public void notify(Level level, Throwable t, String message) {
        notify(level, t, message, log);
    }

    /**
     * Log and notify the user about the problem or error and log the message
     * to the specified log
     *
     * @param level the log level
     * @param t the throwable with the
     * @param message the message
     * @param log where the message should be logged
     */
    public abstract void notify(Level level, Throwable t, String message, Logger log);

    /** null error manager */
    private static class NullErrorManager extends ErrorManager {
        /**
         * Log and notify the user about the problem or error
         *
         * @param level the log level
         * @param t the throwable with the
         * @param message the message
         */
        public void notify(Level level, Throwable t, String message, Logger log) {
            log.log(level, message, t);
        }

    }
}
