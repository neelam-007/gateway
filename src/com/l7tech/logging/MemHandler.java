package com.l7tech.logging;

import java.util.logging.LogRecord;
import java.util.SortedSet;
import java.util.Collections;
import java.util.TreeSet;
import java.util.Comparator;

/**
 * Logging handler that keeps n log records in memory.
 *
 * It differs from the java.util.logging.MemoryHandler in that you can ask for those
 * records back. It will be used by a WS when the console wants to consult recent log
 * entries from a ssg.

 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell
 * Date: Jul 3, 2003
 * 
 */
public class MemHandler extends java.util.logging.Handler {
    /**
     * The maximum number of log records kept in memory
     */
    public static final long MAX_BUFFER_SIZE = 4096;

    public void publish(LogRecord record) {
        if (buffer == null) initialize();
        record(record);
    }

    public void flush() {
    }

    public void close() throws SecurityException {
    }

    public synchronized void initialize() {
        buffer = Collections.synchronizedSortedSet(new TreeSet(new LogRecordComparator()));
    }

    /**
     * May return array of smaller size if data is not available.
     * Will not return null.
     */
    public LogRecord[] getRecords(int offset, int size) {
        if (buffer == null) return new LogRecord[0];
        Object[] allrecords = buffer.toArray();
        int potentialMaxSize = allrecords.length - offset;
        LogRecord[] output = null;
        if (potentialMaxSize < 0) return new LogRecord[0];
        else if (potentialMaxSize < size) output = new LogRecord[potentialMaxSize];
        else output = new LogRecord[size];
        for (int i = 0; i < output.length; i++) {
            output[i] = (LogRecord)allrecords[i+offset];
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
     *                       Retrieve messages until the end of the message buffer is hit if it equals to -1.
     * @param size  the max. number of messages retrieved
     * @return LogRecord[] the array of log records retrieved
     */
    public LogRecord[] getRecords(long startMsgNumber, long endMsgNumber, int size) {
        if (buffer == null) return new LogRecord[0];
        Object[] allrecords = buffer.toArray();

        if(allrecords.length == 0) return new LogRecord[0];

        boolean startMsgFound = false;
        boolean endMsgFound = false;
        int startIndex = 0;
        int endIndex = 0;

         // find the starting point
        if(startMsgNumber >= 0 ) {
            for(int i=0; i < allrecords.length; i++){
                if(((LogRecord)allrecords[i]).getSequenceNumber() == startMsgNumber){
                    startMsgFound = true;

                    // excluding the start msg itself
                    startIndex = i + 1;
                    break;
                }
            }
        }

        if(endMsgNumber >= 0 ) {
            // find the ending point
            for(int i=0; i < allrecords.length; i++){
                if(((LogRecord)allrecords[i]).getSequenceNumber() == endMsgNumber){
                    endMsgFound = true;
                    endIndex = i;
                    break;
                }
            }
        }

        LogRecord[] output = null;
        int potentialMaxSize = 0;

         if(startMsgFound && endMsgFound){
            potentialMaxSize = endIndex - startIndex;
        } else if (!startMsgFound && endMsgFound) {
            // try to retrieve all messages up to the message whose sequence number equals to the start sequence number
            potentialMaxSize = endIndex;
        }
        else if (startMsgFound && !endMsgFound){
            potentialMaxSize = allrecords.length - startIndex;
        } else {
            potentialMaxSize = allrecords.length;
        }

        if (potentialMaxSize <= 0)
            return new LogRecord[0];
        else if (potentialMaxSize < size)
            output = new LogRecord[potentialMaxSize];
        else
            output = new LogRecord[size];

        for (int i = 0; i < output.length; i++) {
            output[i] = (LogRecord) allrecords[i + startIndex];
        }

        return output;

    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private void record(LogRecord record) {
        // keep buffer under control
        while (buffer.size() >= MAX_BUFFER_SIZE) {
            buffer.remove(buffer.last());
        }
        buffer.add(record);
    }

    class LogRecordComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            LogRecord log1 = (LogRecord)o1;
            LogRecord log2 = (LogRecord)o2;
            // reverse order on purpose so that most recent are at top
            return (int)(log2.getMillis() - log1.getMillis());
        }
    }

    private SortedSet buffer = null;
}
