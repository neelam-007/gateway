package com.l7tech.logging;

import java.util.logging.Logger;
import java.util.logging.LogRecord;

/**
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 9:42:52 AM
 *
 */
public abstract class LogManager {

    /**
     * Provides access to the actual log manager
     */
    public static LogManager getInstance() {
        return SingletonHolder.singleton;
    }
    public abstract Logger getSystemLogger();
    public abstract LogRecord[] getRecorded(int offset, int size);


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
    public abstract LogRecord[] getRecorded(long startMsgNumber, long endMsgNumber, int size);

    // ************************************************
    // PRIVATES
    // ************************************************
    private static class SingletonHolder {
        private static LogManager singleton = null;
        static {
            try {
                // dont call the locator because it itself logs shit!
                singleton = new ServerLogManager();
                //todo: consider shrinking this catch. Catching 'Throwable' will
                // catch things such as ThreadDeath and all the java.lang.Error
                // flavors. em21102003
            } catch (Throwable e) {
                // this cannot fail !
                System.err.println("Critical error Locating LogManager");
                e.printStackTrace(System.err);
            } finally {
                // we cannot leave without instantiating the logger
                if (singleton == null) singleton = new BasicLogManager();
            }
        }
    }
}
