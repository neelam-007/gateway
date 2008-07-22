package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;

import com.l7tech.gateway.common.log.SinkConfiguration;

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

    ConsoleMessageSink( final SinkConfiguration configuration ) {
        super( configuration );
        handler = getConsoleHandler();
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
    private Handler getConsoleHandler() {
        Handler consoleHandler = null;

        LogManager manager = LogManager.getLogManager();
        Logger rootLogger = manager.getLogger("");
        Handler[] rootHandlers = rootLogger.getHandlers();
        for ( Handler handler : rootHandlers ) {
            if ( handler instanceof ConsoleHandler) {
                consoleHandler = handler;
                break;
            } else if ( handler instanceof StartupHandler ) {
                StartupHandler sh = (StartupHandler) handler;
                if ( sh.isConsole() ) {
                    consoleHandler = handler;
                }
            }
        }

        return consoleHandler;
    }
}
