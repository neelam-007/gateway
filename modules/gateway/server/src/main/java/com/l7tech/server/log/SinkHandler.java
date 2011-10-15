package com.l7tech.server.log;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.log.HybridDiagnosticContextKeys;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Logging handler that passes records to a MessageSink.
 *
 * @author Steve Jones
 */
public class SinkHandler extends Handler {

    //- PUBLIC

    /**
     * Create a handler with the given sink and category
     *
     * @param sink the MessageSink to use
     * @param category the MessageCategory to use
     */
    public SinkHandler(final MessageSink sink, final MessageCategory category) {
        this.sink = sink;
        this.category = category;
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() {
    }

    /**
     * Does nothing.
     */
    @Override
    public void flush() {
    }

    /**
     * Publish the record to the sink with this handlers category
     *
     * @param record The record to publish
     */
    @Override
    public void publish(final LogRecord record) {
        HybridDiagnosticContext.put( HybridDiagnosticContextKeys.LOGGER_NAME, record.getLoggerName() );
        sink.message(category, record);
    }

    //- PRIVATE

    private final MessageSink sink;
    private final MessageCategory category;
}
