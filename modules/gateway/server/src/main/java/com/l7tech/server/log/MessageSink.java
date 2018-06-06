package com.l7tech.server.log;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.io.Closeable;

/**
 * Interface for message consumers.
 *
 * @author Steve Jones
 */
public interface MessageSink extends Closeable {

    /**
     * Pass a message to a consumer.
     *
     * @param category The message category
     * @param record The message data
     */
    void message(MessageCategory category, LogRecord record);
    
    /**
     * Gets list of handlers used
     * @return Immutable list of handlers used.
     */
    List<Handler> getHandlers();

}
