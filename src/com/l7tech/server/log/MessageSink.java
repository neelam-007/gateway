package com.l7tech.server.log;

import java.util.logging.LogRecord;

/**
 * Interface for message consumers.
 *
 * @author Steve Jones
 */
public interface MessageSink {

    /**
     * Pass a message to a consumer.
     *
     * @param category The message category
     * @param record The message data
     */
    void message(MessageCategory category, LogRecord record);
}
