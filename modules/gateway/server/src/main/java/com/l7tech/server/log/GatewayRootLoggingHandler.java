package com.l7tech.server.log;

import java.io.IOException;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * This is a logging handler that will use the root handlers from the JdkLogConfig.
 * This is used during gateway startup to log message before the sink manager is initialized.
 *
 * @author Victor Kazakov
 */
public class GatewayRootLoggingHandler extends Handler implements StartupAwareHandler {

    public GatewayRootLoggingHandler() throws IOException {
        super();
    }

    @Override
    public void publish(LogRecord record) {
        final List<Handler> handlers = JdkLogConfig.getInitialRootHandlers();
        for(Handler handler : handlers){
            handler.publish(record);
        }
    }

    @Override
    public void flush() {
        final List<Handler> handlers = JdkLogConfig.getInitialRootHandlers();
        for(Handler handler : handlers){
            handler.flush();
        }
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() throws SecurityException {
        //we should not close the root handlers here, they may be used elsewhere
    }
}