package com.l7tech.logging;

import com.l7tech.server.ServerConfig;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Layer 7 technologies, inc.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 11:42:08 AM
 *
 * instance of the log manager that is meant to be used on the server-side
 */
public class ServerLogManager extends LogManager {
    public Logger getSystemLogger() {
        if (systemLogger == null) initialize();
        return systemLogger;
    }

    /**
     * retrieve recent log entries.
     * this is reset when the system restarts
     */
    public LogRecord[] getRecorded(int offset, int size) {
        if (systemLogMemHandler == null) return new LogRecord[0];
        return systemLogMemHandler.getRecords(offset, size);
    }

    /**
     * Retrieve the system logs in between the startMsgNumber and endMsgNumber specified
     * up to the specified size.
     * NOTE: the log messages whose message number equals to startMsgNumber and endMsgNumber
     * are not returned.
     *
     * @param startMsgNumber the message number to locate the start point.
     *                       Start from beginning of the message buffer if it equals to -1.
     * @param endMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the message buffer is hit if it equals to -1.
     * @param size  the max. number of messages retrieved
     * @return LogRecord[] the array of log records retrieved
     */
    public LogRecord[] getRecorded(long startMsgNumber, long endMsgNumber, int size) {
        if (systemLogMemHandler == null) return new LogRecord[0];
        return systemLogMemHandler.getRecords(startMsgNumber, endMsgNumber,  size);
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private synchronized void initialize() {
        // create systemLogger
        if (systemLogger == null) {
            systemLogger = java.util.logging.Logger.getLogger(SYSTEM_LOGGER_NAME);
            systemLogMemHandler = new MemHandler();
            systemLogger.addHandler(systemLogMemHandler);
            /* add our own logging file (instead of catalina.out
            try {
                FileHandler fileHandler = new FileHandler("ssglog");
                fileHandler.setFormatter(new SimpleFormatter());
                systemLogger.addHandler(fileHandler);
            } catch (Exception e) {
                // dont use normal logger here
                e.printStackTrace(System.err);
                throw new RuntimeException(e);
            }*/
            Level level = getLogLevel();
            systemLogger.setLevel(level);
            // add other log handlers here as necessary
        }
    }

    private Level getLogLevel() {
        // default level is INFO but is customizable through web.xml
        String level = ServerConfig.getInstance().getLogLevel();
        if (level != null && level.length() > 0)
            return Level.parse(level);

        return Level.INFO;
    }

    private Logger systemLogger = null;
    private static final String SYSTEM_LOGGER_NAME = "SSG System Log";
    private MemHandler systemLogMemHandler = null;
}
