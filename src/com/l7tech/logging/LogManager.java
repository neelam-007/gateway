package com.l7tech.logging;

import com.l7tech.common.util.Locator;

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
        if (singleton == null) {
            try {
                singleton = (LogManager)Locator.getDefault().lookup(LogManager.class);
            } catch (Throwable e) {
                // this cannot fail !
                System.err.println("Critical error Locating LogManager");
                e.printStackTrace(System.err);
            } finally {
                // we cannot leave without instantiating the logger
                if (singleton == null) singleton = new BasicLogManager();
            }
        }
        return singleton;
    }
    public abstract Logger getSystemLogger();
    public abstract LogRecord[] getRecorded(int offset, int size);

    // ************************************************
    // PRIVATES
    // ************************************************
    private static LogManager singleton = null;
}
