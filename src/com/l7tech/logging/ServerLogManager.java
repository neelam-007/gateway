package com.l7tech.logging;

import javax.naming.NamingException;
import java.util.logging.*;

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
        try {
            javax.naming.Context cntx = new javax.naming.InitialContext();
            String level = (String)(cntx.lookup("java:comp/env/" + levelEnvEntryName));
            if (level != null && level.length() > 0)
                return Level.parse(level);

        } catch (NamingException e) {
            // note, should not user logger here
            e.printStackTrace(System.err);
        }
        return Level.INFO;
    }

    private Logger systemLogger = null;
    private static final String SYSTEM_LOGGER_NAME = "SSG System Log";
    private MemHandler systemLogMemHandler = null;
    private static final String levelEnvEntryName = "SsgLogLevel";
}
