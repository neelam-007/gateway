package com.l7tech.logging;

import com.l7tech.util.Locator;

import java.util.logging.Logger;
import java.util.logging.LogRecord;

/**
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 9:42:52 AM
 *
 * Get access to the
 */
public abstract class LogManager {
    public static LogManager getInstance() {
        if (singleton == null) {
            singleton = (LogManager)Locator.getDefault().lookup(LogManager.class);
            // todo, use the locator to load appropriate version
            // singleton = new ServerLogManager();
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
