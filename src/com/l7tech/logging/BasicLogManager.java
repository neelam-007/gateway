package com.l7tech.logging;

import java.util.logging.Logger;
import java.util.logging.LogRecord;

/**
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 4:37:41 PM
 *
 * Log manager that basically does nothing special.
 */
public class BasicLogManager extends LogManager {
    public Logger getSystemLogger() {
        return java.util.logging.Logger.global;
    }

    public LogRecord[] getRecorded(int offset, int size) {
        return new LogRecord[0];
    }
}
