package com.l7tech.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * A Log4j appender implementation that logs to the JDK logging framework.
 *
 * <p>This appender allows use of 3rd party libraries on the Gateway that use
 * Log4j without requiring multiple log files.</p>
 *
 * <p>Configuration is via the usual Log4j methods or by calling
 * <code>#init()</code>.</p>
 *
 * <p>This implementation aims to work with the Log4j 1.2.x API.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class Log4jJdkLogAppender implements Appender
{
    //- PUBLIC

    /**
     * Initialize logging and configure to listen to underlying updates.
     */
    public static void init() {
        LoggerRepository loggerRepository = LogManager.getLoggerRepository();
        Category rootCategory = loggerRepository.getRootLogger();
        rootCategory.removeAllAppenders();
        rootCategory.addAppender(new Log4jJdkLogAppender());
        reset();
        java.util.logging.LogManager.getLogManager().addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                reset();
            }
        });
    }

    /**
     * Set/Reset the Log4j logging.
     *
     * <p>Note that this does not call LoggerRepository#resetConfiguration() it
     * just resets the root categories priority.</p>
     */
    public static void reset() {
        // Get info
        LoggerRepository loggerRepository = LogManager.getLoggerRepository();
        Category rootCategory = loggerRepository.getRootLogger();
        Logger rootLogger = Logger.getLogger("");
        Level rootLevel = rootLogger.getLevel();

        // Do it
        rootCategory.setLevel(getPriority(rootLevel));
    }

    /**
     *
     */
    public Log4jJdkLogAppender() {
    }

    /**
     *
     */
    public void addFilter(Filter filter) {
    }

    /**
     *
     */
    public Filter getFilter() {
        return null;
    }

    /**
     *
     */
    public void clearFilters() {
    }

    /**
     *
     */
    public void close() {
    }

    /**
     *
     */
    public void doAppend(final LoggingEvent loggingEvent) {
        String name = loggingEvent.getLoggerName()==null ? "" : loggingEvent.getLoggerName();
        Logger logger = Logger.getLogger(name);
        Level level = getLevel(loggingEvent.getLevel()==null ? org.apache.log4j.Level.INFO : loggingEvent.getLevel());
        if(logger.isLoggable(level)) {
            logger.log(getLogRecord(level, loggingEvent));
        }
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public void setErrorHandler(final ErrorHandler errorHandler) {
    }

    /**
     *
     */
    public ErrorHandler getErrorHandler() {
        return null;
    }

    /**
     *
     */
    public void setLayout(final Layout layout) {
    }

    /**
     *
     */
    public Layout getLayout() {
        return null;
    }

    /**
     *
     */
    public void setName(final String string) {
        this.name = string;
    }

    /**
     *
     */
    public boolean requiresLayout() {
        return false;
    }

    //- PRIVATE

    /**
     * The name for this appender.
     */
    private String name;

    /**
     * Translate from a Log4j priority to a JDK level.
     */
    private static Level getLevel(final org.apache.log4j.Level priority) {
        Level level = null;

        switch(priority.toInt()) {
            case org.apache.log4j.Level.DEBUG_INT:
                level = Level.FINE;
                break;
            case org.apache.log4j.Level.INFO_INT:
                level = Level.INFO;
                break;
            case org.apache.log4j.Level.WARN_INT:
                level = Level.WARNING;
                break;
            case org.apache.log4j.Level.ERROR_INT:
                level = Level.SEVERE;
                break;
            default:
                if(priority.toInt()<org.apache.log4j.Level.DEBUG_INT) {
                    level = Level.FINER;
                }
                else if(priority.toInt()>org.apache.log4j.Level.ERROR_INT) {
                    level = Level.SEVERE;
                }
                else {
                    level = Level.INFO;
                }
        }

        return level;
    }

    /**
     * Convert a level to a priority, this is used to set up the Log4j threshold
     * to be similiar to the JDK logging threshold.
     *
     * Not frequently called.
     */
    private static org.apache.log4j.Level getPriority(final Level level) {
        org.apache.log4j.Level priority = org.apache.log4j.Level.WARN;

        if(Level.ALL.equals(level)) {
            priority = org.apache.log4j.Level.ALL;
        }
        else if(Level.FINEST.equals(level)
             || Level.FINER.equals(level)
             || Level.FINE.equals(level)) {
            priority = org.apache.log4j.Level.DEBUG;
        }
        else if(Level.INFO.equals(level)
             || Level.CONFIG.equals(level)) {
            priority = org.apache.log4j.Level.INFO;
        }
        else if(Level.WARNING.equals(level)) {
            priority = org.apache.log4j.Level.WARN;
        }
        else if(Level.SEVERE.equals(level)) {
            priority = org.apache.log4j.Level.ERROR;
        }
        else if(Level.OFF.equals(level)) {
            priority = org.apache.log4j.Level.OFF;
        }

        return priority;
    }

    /**
     * Convert the given event to a log record
     */
    private static LogRecord getLogRecord(final Level level, final LoggingEvent loggingEvent) {
        LogRecord logRecord = new LogRecord(level, loggingEvent.getRenderedMessage());
        logRecord.setLoggerName(loggingEvent.getLoggerName());
        LocationInfo locationInfo = loggingEvent.getLocationInformation();
        if(locationInfo!=null) {
            if(locationInfo.getClassName()!=null) logRecord.setSourceClassName(locationInfo.getClassName());
            if(locationInfo.getMethodName()!=null) logRecord.setSourceMethodName(locationInfo.getMethodName());
        }
        ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
        if(throwableInformation!=null) {
            logRecord.setThrown(throwableInformation.getThrowable());
        }
        return logRecord;
    }
}
