package com.l7tech.portal.metrics;

import org.apache.log4j.Logger;

/**
 * Utility class for logging.
 *
 * @author alee
 */
public final class LogUtil {

    /**
     * If debug is enabled and the error was caused by an exception, logs error message and exception at debug level.
     * <p/>
     * Otherwise logs error message (no exception) at warning level.
     *
     * @param logger       the Logger with which to log.
     * @param errorMessage the error message to log.
     * @param exception    the exception which caused the error. Can be null.
     */
    public static void logError(final Logger logger, final String errorMessage, final Throwable exception) {
        if (logger.isDebugEnabled() && exception != null) {
            logger.debug(errorMessage, exception);
        } else {
            logger.warn(errorMessage);
        }
    }

    /**
     * If debug is enabled and the error was caused by an exception, logs error message and exception at debug level and then exits.
     * <p/>
     * Otherwise logs error message (no exception) at warning level and then exits.
     *
     * @param logger       the Logger with which to log.
     * @param errorMessage the error message to log.
     * @param exception    the exception which caused the error. Can be null.
     */
    public static void logErrorAndExit(final Logger logger, final String errorMessage, final Throwable exception) {
        logError(logger, errorMessage, exception);
        System.exit(0);
    }
}
