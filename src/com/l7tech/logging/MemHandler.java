package com.l7tech.logging;

import java.util.logging.LogRecord;
import java.util.SortedSet;
import java.util.Collections;
import java.util.TreeSet;
import java.util.Comparator;

/**
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 9:06:31 AM
 *
 * Logging handler that keeps n log records in memory.
 * It differs from the java.util.logging.MemoryHandler in that you can ask for those
 * records back. It will be used by a WS when the console wants to consult recent log
 * entries from a ssg.
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
        public boolean equals(Object obj) {
            return obj.equals(this);
        }
    }

    private SortedSet buffer = null;
}
