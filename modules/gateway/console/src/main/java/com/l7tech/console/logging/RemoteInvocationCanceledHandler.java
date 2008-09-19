package com.l7tech.console.logging;

import com.l7tech.console.util.RemoteInvocationCanceledException;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Error handler for RemoteInvocationCanceledException.
 */
class RemoteInvocationCanceledHandler implements ErrorHandler {
    private static final Logger logger = Logger.getLogger(RemoteInvocationCanceledHandler.class.getName());

    public void handle(ErrorEvent e) {
        final RemoteInvocationCanceledException rice = ExceptionUtils.getCauseIfCausedBy(e.getThrowable(), RemoteInvocationCanceledException.class);
        if (rice != null) {
            logger.log(Level.INFO, "Canceled: " + ExceptionUtils.getMessage(rice), rice);
        } else {
            e.handle();
        }
    }
}
