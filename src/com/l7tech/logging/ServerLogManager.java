package com.l7tech.logging;

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Level;

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

    // ************************************************
    // PRIVATES
    // ************************************************

    private synchronized void initialize() {
        // create systemLogger
        if (systemLogger == null) {
            systemLogger = java.util.logging.Logger.getLogger(SYSTEM_LOGGER_NAME);
            systemLogMemHandler = new MemHandler();
            systemLogger.addHandler(systemLogMemHandler);
            systemLogger.setLevel(Level.INFO);
            // add other log handlers here as necessary
        }
    }

    private Logger systemLogger = null;
    private static final String SYSTEM_LOGGER_NAME = "SSG System Log";
    private MemHandler systemLogMemHandler = null;
}
