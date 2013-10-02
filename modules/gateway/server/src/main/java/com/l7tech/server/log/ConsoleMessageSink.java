package com.l7tech.server.log;

import com.l7tech.gateway.common.log.SinkConfiguration;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * MessageSink for logging to the console.
 *
 * @author Steve Jones
 */
public class ConsoleMessageSink extends MessageSinkSupport {

    //- PUBLIC

    /**
     * Does nothing
     */
    public void close() {
    }

    //- PACKAGE

    ConsoleMessageSink( final SinkConfiguration configuration, Handler handler ) {
        super( configuration );
        this.handler = handler;
    }

    /**
     * Logs the given record to the console.
     *
     * @param category The message category
     * @param record The record to log
     */
    void processMessage(final MessageCategory category, final LogRecord record) {
        if ( handler != null ) {
            handler.publish(record);
        }
    }

    //- PRIVATE

    private final Handler handler;

    /**
     * Get the root console handler if one exists.
     */
    static Handler getRootConsoleHandler() {
        Handler consoleHandler = null;

        Logger rootLogger = Logger.getLogger("");
        Handler[] rootHandlers = rootLogger.getHandlers();
        for ( Handler handler : rootHandlers ) {
            if ( handler instanceof ConsoleHandler ) {
                consoleHandler = handler;
                break;
            }
        }

        return consoleHandler;
    }
}
