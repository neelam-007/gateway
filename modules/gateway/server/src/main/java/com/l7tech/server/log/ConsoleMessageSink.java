package com.l7tech.server.log;

import com.l7tech.gateway.common.log.SinkConfiguration;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.*;

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
            if ( handler instanceof L7ConsoleHandler ) {
                consoleHandler = handler;
                break;
            }
        }

        return consoleHandler;
    }

    public static class L7ConsoleHandler extends StreamHandler {
        private static final OutputStream out = new FileOutputStream(FileDescriptor.err);

        public L7ConsoleHandler() {
            super();
            setOutputStream( out );
        }

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        @Override
        public synchronized void close() throws SecurityException {
            flush();
        }
    }
}
