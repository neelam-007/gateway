package com.l7tech.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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

    /**
     * Retrieve the system logs in between the startMsgNumber and endMsgNumber specified
     * up to the specified size.
     * NOTE: the log messages whose message number equals to startMsgNumber and endMsgNumber
     * are not returned.
     *
     * @param startMsgNumber the message number to locate the start point.
     *                       Start from beginning of the message buffer if it equals to -1.
     * @param endMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the message buffer is hit
     *                       if it equals to -1.
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
        if (systemLogger == null) {
            systemLogger = Logger.getLogger(SYSTEM_LOGGER_NAME);
            // create system Logger
            try {
                systemLogger.setLevel(getLevel());
            } catch (RuntimeException e) {
                System.err.println("can't read property " + e.getMessage());
                // continue without those special log handlers
                return;
            }
            // add custom memory handler
            systemLogMemHandler = new MemHandler();
            systemLogger.addHandler(systemLogMemHandler);
            // add a file handler
            try {
                String pattern = getLogFilesPath() + File.separator + "ssg_%g_%u.log";
                FileHandler fileHandler = new FileHandler(pattern, getLogFilesSizeLimit(), getLogFileNr());
                fileHandler.setFormatter(new SimpleFormatter());
                systemLogger.addHandler(fileHandler);
            } catch (IOException e) {
                // dont use normal logger here
                throw new RuntimeException(e);
            }
        }
    }

    private Level getLevel() {
        try {
            String strval = getProps().getProperty(LEVEL_PROP_NAME);
            return Level.parse(strval);
        } catch (Throwable e) {
            // if cant' read from props file, default to all
            return Level.ALL;
        }
    }

    private String getLogFilesPath() {
        try {
            return getProps().getProperty(FILEPATH_PROP_NAME);
        } catch (Throwable e) {
            System.err.println("can't read props " + e);
            // if cant' read from props file, default to home dir
            return System.getProperties().getProperty("user.home");
        }
    }

    private int getLogFilesSizeLimit() {
        try {
            return Integer.parseInt(getProps().getProperty(SIZELIMIT_PROP_NAME));
        } catch (Throwable e) {
            System.err.println("can't read props " + e);
            // if cant' read from props file, default to 500000
            return 500000;
        }
    }

    private int getLogFileNr() {
        try {
            return Integer.parseInt(getProps().getProperty(NRFILES_PROP_NAME));
        } catch (Throwable e) {
            System.err.println("can't read props " + e);
            // if cant' read from props file, default to 4
            return 4;
        }
    }

    private synchronized Properties getProps() throws RuntimeException {
        if (props == null) {
            try {
                InputStream inputStream = null;

                String path = System.getProperty( PROP_LOGPROPERTIES );
                if ( path == null || path.length() == 0 ) path = DEFAULT_LOGPROPERTIES_PATH;
                File f = new File( path );
                if ( f.exists() ) {
                    try {
                        inputStream = new FileInputStream( f );
                    } catch ( IOException ioe ) {
                        // inputStream stays null
                    }
                }

                if ( inputStream == null ) {
                    inputStream = getClass().getResourceAsStream(PROPS_PATH);
                }

                props = new Properties();
                if (props == null) throw new RuntimeException("can't read properties");
                props.load(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return props;
    }

    public static final String PROPS_PATH = "/ssglog.properties";
    public static final String LEVEL_PROP_NAME = "com.l7tech.server.log.level";
    public static final String FILEPATH_PROP_NAME = "com.l7tech.server.log.FileHandler.path";
    public static final String SIZELIMIT_PROP_NAME = "com.l7tech.server.log.FileHandler.limit";
    public static final String NRFILES_PROP_NAME = "com.l7tech.server.log.FileHandler.count";
    public static final String PROP_LOGPROPERTIES = "com.l7tech.server.logPropertiesPath";
    public static final String DEFAULT_LOGPROPERTIES_PATH  = "/ssg/etc/conf/ssglog.properties";

    private Logger systemLogger = null;
    private static final String SYSTEM_LOGGER_NAME = "com.l7tech.server.log";
    private MemHandler systemLogMemHandler = null;
    private Properties props = null;
}
