package com.l7tech.logging;

import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.*;
import java.sql.SQLException;

/**
 * Instance of the log manager that is meant to be used on the server-side.<br/><br/>
 *
 * Reads properties from ssglog.properties file. Tries to get this file from
 * /ssg/etc/conf/ssglog.properties. If not present, gets is from
 * webapps/ROOT/WEB-INF/classes/ssglog.properties
 * Creates log rotation files, in a path provided in properties file. If this path
 * is invalid, use home dir instead.
 *
 * NOTE: Please avoid calling any external class that uses logging itself.
 *
 * NOTE: Unusual exception handling because of the fact that logging subsystem
 * is not initialized yet.
 * 
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Jul 3, 2003<br/>
 * Time: 11:42:08 AM<br/><br/>
 */
public class ServerLogManager extends LogManager {

    public ServerLogManager() {
        initialize();
    }

    public Logger getSystemLogger() {
        return systemLogger;
    }

    /**
     * retrieve recent log entries.
     * this is reset when the system restarts
     */
    public LogRecord[] getRecorded(int offset, int size) {
        // todo, remove this
        return new LogRecord[0];
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
        Collection res = dbHandler.getLogRecords(null, startMsgNumber, endMsgNumber, size);
        LogRecord[] output = new LogRecord[res.size()];
        int cnt = 0;
        for (Iterator i = res.iterator(); i.hasNext(); cnt++) {
            output[cnt] = (LogRecord)i.next();
        }
        return output;
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
     * @param nodeId         the node id for which to retrieve server logs on. if left null, retreives
     *                       log records for this node.
     * @param size  the max. number of messages retrieved
     * @return LogRecord[] the array of log records retrieved
     */
    public SSGLogRecord[] getRecorded(String nodeId, long startMsgNumber, long endMsgNumber, int size) {
        Collection res = dbHandler.getLogRecords(nodeId, startMsgNumber, endMsgNumber, size);
        SSGLogRecord[] output = new SSGLogRecord[res.size()];
        int cnt = 0;
        for (Iterator i = res.iterator(); i.hasNext(); cnt++) {
            output[cnt] = (SSGLogRecord)i.next();
        }
        return output;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private synchronized void initialize() {
        if (systemLogger == null) {
            systemLogger = Logger.getLogger(SYSTEM_LOGGER_NAME);

            try {
                systemLogger.setLevel(getLevel());
            } catch (RuntimeException e) {
                System.err.println("can't read property " + e.getMessage());
                // continue without those special log handlers
                return;
            }

            setLogHandlers(systemLogger);
        }
    }

    private void setLogHandlers(Logger logger) {
        /*// add custom memory handler
        if (systemLogMemHandler == null) {
            systemLogMemHandler = new MemHandler();
        }
        logger.addHandler(systemLogMemHandler);*/

        // add a file handler
        try {
            String pattern = getLogFilesPath();
            if (pattern.charAt(pattern.length()-1) != File.separatorChar) {
                pattern += File.separator;
            }
            pattern += "ssg_%g_%u.log";
            FileHandler fileHandler = new FileHandler(pattern, getLogFilesSizeLimit(), getLogFileNr());
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            if (startDbHandler()) {
                pluginServerLogHandler(logger);
            }
        } catch (Throwable e) {
            System.err.println("can't set special handlers " + e.getMessage());
        }
    }

    private void pluginServerLogHandler(Logger logger) {
        final Logger targetLogger = logger;
        // let the initialization of the logger finish
        Thread delayedOperation = new Thread() {
            public void run() {
                if (dbHandler != null) return;
                dbHandler = new ServerLogHandler();
                boolean initialized = false;
                int i = 0;
                while (!initialized) {
                    try {
                        sleep(3000);
                    } catch (InterruptedException e) {
                        System.err.println("sleep interrupted " + e.getMessage());
                    }
                    try {
                        HibernatePersistenceContext context = null;
                        try {
                            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
                        } catch (SQLException e) {
                            System.err.println("cannot get persistence context " + e.getMessage());
                            initialized = false;
                            continue;
                        }
                        context.beginTransaction();
                        dbHandler.initialize();
                        context.commitTransaction();
                        context.close();
                        initialized = true;
                        break;
                    } catch (IllegalStateException e) {
                        System.err.println("error initializing trying again: " + e.getMessage());
                        initialized = false;
                    } catch (TransactionException e) {
                        System.err.println("transaction exception: " + e.getMessage() +
                                           " server log handler cannot be initialized");
                        return;
                    }
                    ++i;
                    if (i > 3) {
                        // give up after a while
                        return;
                    }
                }
                targetLogger.addHandler(dbHandler);
            }
        };
        delayedOperation.start();
    }

    private Level getLevel() {
        try {
            String strval = getProps().getProperty(LEVEL_PROP_NAME);
            return Level.parse(strval);
        } catch (Throwable e) {
            System.err.println("can't read props " + e.getMessage());
            // if cant' read from props file, default to all
            return Level.ALL;
        }
    }

    private String getLogFilesPath() {
        String path = null;
        try {
            path = getProps().getProperty(FILEPATH_PROP_NAME);
        } catch (Throwable e) {
            System.err.println("can't read props " + e.getMessage());
            // if cant' read from props file, default to home dir
            path = null;
        }
        // check that the path is valid
        File f = new File(path);
        if (!f.exists()) {
            path = System.getProperties().getProperty("user.home");
            System.err.println("Path provided for log files does not exist, using " + path +
                               " instead");
        }
        return path;
    }

    private boolean startDbHandler() {
        String val = getProps().getProperty(START_DBHANDLER_PROP);
        if (val == null) return false;
        return Boolean.valueOf(val).booleanValue();
    }

    private int getLogFilesSizeLimit() {
        try {
            return Integer.parseInt(getProps().getProperty(SIZELIMIT_PROP_NAME));
        } catch (Throwable e) {
            System.err.println("can't read props " + e.getMessage());
            // if cant' read from props file, default to 500000
            return 500000;
        }
    }

    private int getLogFileNr() {
        try {
            return Integer.parseInt(getProps().getProperty(NRFILES_PROP_NAME));
        } catch (Throwable e) {
            System.err.println("can't read props " + e.getMessage());
            // if cant' read from props file, default to 4
            return 4;
        }
    }

    private synchronized Properties getProps() throws RuntimeException {
        if (props == null) {
            try {
                InputStream inputStream = null;

                String path = System.getProperty(PROP_LOGPROPERTIES);
                if (path == null || path.length() == 0) path = DEFAULT_LOGPROPERTIES_PATH;
                File f = new File(path);
                if (f.exists()) {
                    try {
                        inputStream = new FileInputStream(f);
                    } catch (IOException e) {
                        // inputStream stays null
                        System.err.println("Can't open prop file " + f.getName() + " " + e.getMessage());
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
    public static final String START_DBHANDLER_PROP = "com.l7tech.server.log.HibHandler";
    public static final String DEFAULT_LOGPROPERTIES_PATH  = "/ssg/etc/conf/ssglog.properties";

    private Logger systemLogger = null;
    private static final String SYSTEM_LOGGER_NAME = "com.l7tech.server.log";
    private ServerLogHandler dbHandler = null;
    private Properties props = null;
}
