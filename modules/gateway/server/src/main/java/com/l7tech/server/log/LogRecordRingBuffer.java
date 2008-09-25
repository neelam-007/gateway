package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Component that maintains a buffer of logging events.
 *
 * <p>The default buffer size is 100 records.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class LogRecordRingBuffer {

    //- PUBLIC

    /**
     *
     */
    public LogRecordRingBuffer() {
        bufferFull = false;
        nextRecordIndex = 0;
        bufferSize = 100;
        loggerName = "";
    }

    /**
     * Initialize this LogRecordRingBuffer.
     *
     * <p>This will:</p>
     *
     * <ol>
     *   <li>Create the queue (buffer)</li>
     *   <li>Register for logging reconfiguration notices</li>
     *   <li>Register a handler to start receiving events</li>
     * </ol>
     */
    public void init() {
        // create buffer
        records = new LogRecord[bufferSize];

        // create record handler
        final Handler queueHandler = new LogRecordHandler(this);

        // register for reconfig notification
        LogManager logManager = LogManager.getLogManager();

        PropertyChangeListener pcl = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // add handler
                LogManager logManager = LogManager.getLogManager();
                Logger rootLogger = logManager.getLogger(loggerName);
                if(rootLogger!=null) rootLogger.addHandler(queueHandler);
            }
        };

        logManager.addPropertyChangeListener(pcl);
        pcl.propertyChange(null); // do initial registration.
    }

    /**
     *
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the queue size.
     *
     * <p>After initialization this cannot be change.</p>
     *
     * @param bufferSize the queue size
     * @throws IllegalStateException if initialized.
     */
    public void setBufferSize(int bufferSize) throws IllegalStateException {
        if(records!=null) throw new IllegalStateException("Cannot set queue size after initialization.");
        this.bufferSize = bufferSize;
    }

    /**
     * Get the name of the logger that the handler should be attached to.
     *
     * @return the logger name
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * Set the name of the logger that the handler should be attached to.
     *
     * @param loggerName the handler attachment point.
     * @throws IllegalStateException if initialized.
     */
    public void setLoggerName(String loggerName) throws IllegalStateException {
        if(records!=null) throw new IllegalStateException("Cannot set logger name after initialization.");
        this.loggerName = loggerName;
    }

    /**
     * Get all currently buffered log records.
     *
     * @return the currently buffered log records (oldest first).
     */
    synchronized  public LogRecord[] getLogRecords() {
        LogRecord[] currentRecords;

        if (bufferFull) {
            currentRecords = new LogRecord[records.length];
            System.arraycopy(records, nextRecordIndex, currentRecords, 0, records.length - nextRecordIndex);
            System.arraycopy(records, 0, currentRecords, records.length - nextRecordIndex, nextRecordIndex);
        } else {
            currentRecords = new LogRecord[nextRecordIndex];
            System.arraycopy(records, 0, currentRecords, 0, nextRecordIndex);
        }

        return currentRecords;
    }

    /**
     * Get all the records more recent than X (inclusive).
     *
     * @return the new records
     */
    public LogRecord[] getLogRecords(long sequenceNumber) {
        LogRecord[] recordSnapshot = getLogRecords();

        int startOfNewRecords = recordSnapshot.length;
        for (int i = 0; i < recordSnapshot.length; i++) {
            LogRecord logRecord = recordSnapshot[i];
            if(logRecord.getSequenceNumber() >= sequenceNumber) {
                startOfNewRecords = i;
                break;
            }
        }

        LogRecord[] newRecords = new LogRecord[recordSnapshot.length-startOfNewRecords];
        System.arraycopy(recordSnapshot, startOfNewRecords, newRecords, 0, newRecords.length);
        return newRecords;
    }

    //- PACKAGE

    /**
     * Add a record to the buffer. If full then wrap around and note as full.
     *
     * <p>NOTE: probably best not to do any logging from within this method ...</p>
     *
     * @param record the record to publish
     */
    synchronized void publish(LogRecord record) {
        if ( records != null && records.length > 0 &&
             record != null && ! doNotLog.contains(record.getLoggerName())) {

            // (pseudo)serialize record parameters; gets rid of potentially huge size params
            Object[] params = record.getParameters();
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] != null) {
                        params[i] = params[i].toString();
                    }
                }
            }

            records[nextRecordIndex] = record;
            nextRecordIndex++;
            if (nextRecordIndex >= records.length) {
                nextRecordIndex = 0;
                bufferFull = true;
            }
        }
    }

    //- PRIVATE

    private String loggerName;
    private int bufferSize;
    private boolean bufferFull;
    private int nextRecordIndex;
    private LogRecord[] records;

    private static Set<String> doNotLog = new HashSet<String>();
    static {
        doNotLog.add(SinkManagerImpl.TRAFFIC_LOGGER_NAME);
    }
}
