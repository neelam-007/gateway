package com.l7tech.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.*;

/**
 * SSG log manager that sets the right handlers to the root logger and
 * prepares the hibernate dumper when ready. This must be instantiated when
 * the server boots and once hibernate is ready, a call to suscribeDBHandler
 * must be made.
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
public class ServerLogManager {

    public static ServerLogManager getInstance() {
        return SingletonHolder.singleton;
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

    private ServerLogManager() {
        initialize();
    }

    private synchronized void initialize() {
        if (rootLogger == null) {
            rootLogger = Logger.getLogger(ROOT_LOGGER_NAME);

            try {
                rootLogger.setLevel(getLevel());
            } catch (RuntimeException e) {
                System.err.println("can't read property " + e.getMessage());
                // continue without those special log handlers
                return;
            }

            setLogHandlers(rootLogger);
        }
    }

    private void setLogHandlers(Logger logger) {

        try {
            // add a file handler
            String pattern = getLogFilesPath();
            if (pattern.charAt(pattern.length()-1) != File.separatorChar) {
                pattern += File.separator;
            }
            pattern += "ssg_%g_%u.log";
            FileHandler fileHandler = new FileHandler(pattern, getLogFilesSizeLimit(), getLogFileNr(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            // add a suscriber handler
            logger.addHandler(suscriberHandler);
        } catch (Throwable e) {
            System.err.println("can't set special handlers " + e.getMessage());
        }


    }

    /**
     * this should only be called once hibernate is initialized
     */
    public void suscribeDBHandler() {
        if (dbHandler != null) return;
        dbHandler = new ServerLogHandler();
        dbHandler.initialize();
        suscriberHandler.suscribe(dbHandler);
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

    private Logger rootLogger = null;
    private static final String ROOT_LOGGER_NAME = "com.l7tech";
    private ServerLogHandler dbHandler = null;
    private Properties props = null;

    private class SuscriberHandler extends Handler {
        public void publish(LogRecord record) {
            for (Iterator i = suckers.iterator(); i.hasNext(); ) {
                ((Handler)i.next()).publish(record);
            }
        }

        public void flush() {
            for (Iterator i = suckers.iterator(); i.hasNext(); ) {
                ((Handler)i.next()).flush();
            }
        }

        public void close() throws SecurityException {
            for (Iterator i = suckers.iterator(); i.hasNext(); ) {
                ((Handler)i.next()).close();
            }
        }

        public synchronized void suscribe(Handler sucker) {
            if (!suckers.contains(sucker)) {
                suckers.add(sucker);
            }
        }
        private final ArrayList suckers = new ArrayList();
    }
    private final SuscriberHandler suscriberHandler = new SuscriberHandler();

    private static class SingletonHolder {
        private static ServerLogManager singleton = new ServerLogManager();
    }
}
