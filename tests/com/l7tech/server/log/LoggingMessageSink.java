package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * MessageSink for logging messages when testing.
 *
 * @author Steve Jones
 */
public class LoggingMessageSink implements MessageSink {

    private static final Logger logger = Logger.getLogger(LoggingMessageSink.class.getName());

    public void message(MessageCategory category, LogRecord record) {
        logger.log( record );
    }

    public void close() throws IOException {
    }
}
