package com.l7tech.server.log;

import com.l7tech.util.Config;

import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.text.MessageFormat;

/**
 * Component that maintains a buffer of logging events.
 *
 * <p>The default buffer size is 100 records.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class LogRecordRingBuffer implements PropertyChangeListener {

    //- PUBLIC

    /**
     *
     */
    public LogRecordRingBuffer() {
        bufferFull = false;
        nextRecordIndex = 0;
        bufferSize = 100;
        loggerName = "";
        messageSizeLimit = new AtomicInteger(4096);
        paramSizeLimit = new AtomicInteger(4096);
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
            @Override
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
     * Get the configuration in use.
     *
     * @return The configuration property.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Set the configuration to use.
     *
     * @param config The configuration to use
     * @throws IllegalStateException if initialized.
     */
    public void setConfig( final Config config ) {
        if ( this.config != null ) throw new IllegalStateException("Cannot set config after initialization.");
        this.config = config;
        readConfig();
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

    @Override
    public void propertyChange( final PropertyChangeEvent propertyChangeEvent ) {
        readConfig();
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

            records[nextRecordIndex] = process(record);
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
    private Config config;
    private LogRecord[] records;
    private final AtomicInteger messageSizeLimit;
    private final AtomicInteger paramSizeLimit;

    private static Set<String> doNotLog = new HashSet<String>();
    static {
        doNotLog.add(SinkManagerImpl.TRAFFIC_LOGGER_NAME);
    }

    private void readConfig() {
        Config config = this.config;
        if ( config != null ) {
            messageSizeLimit.set( range( config.getIntProperty("logBuffer.messageSize", 4096), 128, Integer.MAX_VALUE ) );
            paramSizeLimit.set( range( config.getIntProperty("logBuffer.paramSize", 4096), 128, Integer.MAX_VALUE ) );
        }
    }

    private int range( final int value, final int min, final int max  ) {
        int ranged = value;

        if ( value < min ) {
            ranged = min;
        } else if ( value > max ) {
            ranged = max;
        }

        return ranged;
    }

    private LogRecord process( final LogRecord logRecord ) {
        final int messageSizeLimit = this.messageSizeLimit.get();
        final int paramSizeLimit = this.paramSizeLimit.get();

        final Object[] params = logRecord.getParameters();
        LogRecord processedLogRecord;
        String logMessage = logRecord.getMessage();
        if ( logMessage != null && logMessage.length() > messageSizeLimit ) {
            String message = logMessage;
            if( message != null && params!=null && params.length>0) {
                try {
                    logMessage = MessageFormat.format(message, params);
                }
                catch(IllegalArgumentException iae) {
                    // then display the unformatted message
                }
            }

            logMessage = logMessage.substring( 0, messageSizeLimit );
            processedLogRecord = new LogRecord( logRecord.getLevel(), logMessage );
        } else {
            processedLogRecord = new LogRecord( logRecord.getLevel(), logMessage );

            // (pseudo)serialize record parameters; gets rid of potentially huge size params
            if ( params != null ) {
                Object[] truncParams = new Object[ params.length ];
                for (int i = 0; i < params.length; i++) {
                    if (params[i] != null) {
                        String paramValue = params[i].toString();
                        if ( paramValue != null && paramValue.length() > paramSizeLimit ) {
                            paramValue = paramValue.substring( 0, paramSizeLimit );
                        }
                        truncParams[i] = paramValue;
                    }
                }
                processedLogRecord.setParameters( truncParams );
            }
        }

        processedLogRecord.setLoggerName( logRecord.getLoggerName() );
        processedLogRecord.setMillis( logRecord.getMillis() );
        processedLogRecord.setSequenceNumber( logRecord.getSequenceNumber() );
        processedLogRecord.setThreadID( logRecord.getThreadID() );
        
        return processedLogRecord;
    }
}
