package com.l7tech.server.log;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Logging Handler that adds events to a buffer.
 *
 * @author $Author$
 * @version $Revision$
 */
class LogRecordHandler extends Handler {

    //- PUBLIC

    /**
     * Publish a record to the queue.
     *
     * @param record the record to publish
     */
    public void publish(LogRecord record) {
        if(record!=null) {
            logRecordRingBuffer.publish(record);
        }
    }

    /**
     * Does nothing.
     */
    public void flush() {
    }

    /**
     * Does nothing.
     */
    public void close() {
    }

    //- PACKAGE

    LogRecordHandler(LogRecordRingBuffer buffer) {
        logRecordRingBuffer = buffer;
    }

    //- PRIVATE

    private final LogRecordRingBuffer logRecordRingBuffer;
}
