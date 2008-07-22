package com.l7tech.server.util;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An Thread.UncaughtExceptionHandler that logs exceptions.
 *
 * @author Steve Jones
 */
public final class UncaughtExceptionLogger implements Thread.UncaughtExceptionHandler {

    //- PUBLIC

    /**
     * Install this handler as the default.
     */
    public void install() {
        doInstall(true);
    }

    /**
     * Install this handler as the default if there is no current handler.
     */
    public void installIfNoHandler() {
        doInstall(false);
    }

    /**
     * Handle an uncaught exception.
     *
     * @param thread The thread in which the exception was thrown
     * @param throwable The exception
     */
    public void uncaughtException(Thread thread, Throwable throwable) {
        logger.log(Level.SEVERE, "Uncaught exception in thread '" +thread.getName()+ "' [Id:" +thread.getId()+ "].", throwable);
    }

    /**
     * Create and install a default handler.
     *
     * @return the installed handler.
     */
    public static UncaughtExceptionLogger createAndInstall() {
        UncaughtExceptionLogger logger = new UncaughtExceptionLogger();
        logger.doInstall(true);
        return logger;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(UncaughtExceptionLogger.class.getName());

    /**
     * Install this handler
     *
     * @param override Install even if a handler is set.
     */
    private void doInstall(boolean override) {
        if (override || Thread.getDefaultUncaughtExceptionHandler()==null) {
            Thread.setDefaultUncaughtExceptionHandler(this);
        }
    }
}
