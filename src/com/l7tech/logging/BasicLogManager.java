package com.l7tech.logging;

import java.util.logging.Logger;
import java.util.logging.LogRecord;

/**
 * Log manager that does nothing special.
 *
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Jul 3, 2003<br/>
 */
public class BasicLogManager extends LogManager {
    public Logger getSystemLogger() {
        return java.util.logging.Logger.global;
    }

    public LogRecord[] getRecorded(int offset, int size) {
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
     *                       Retrieve messages until the end of the message buffer is hit if it equals to -1.
     * @param size  the max. number of messages retrieved
     * @return LogRecord[] the array of log records retrieved
     */
    public LogRecord[] getRecorded(long startMsgNumber, long endMsgNumber, int size) {
        return new LogRecord[0];
    }

    public SSGLogRecord[] getRecorded(String nodeId, long startMsgNumber, long endMsgNumber, int size) {
        return new SSGLogRecord[0];
    }
}
