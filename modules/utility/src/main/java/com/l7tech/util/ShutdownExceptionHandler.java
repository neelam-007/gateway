package com.l7tech.util;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Timer;
import java.util.TimerTask;

/**
 * UncaughtExceptionHandler that suppresses exceptions during system shutdown.
 *
 * @author Steve Jones
 */
public class ShutdownExceptionHandler implements Thread.UncaughtExceptionHandler {

    //- PUBLIC

    public static ShutdownExceptionHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Add an exception handler to the given Timers thread.
     *
     * @param timer The timer to use
     */
    public static void addShutdownHandler(final Timer timer) {
        if (timer != null) {
            timer.schedule(new TimerTask(){
                public void run() {
                    Thread.currentThread().setUncaughtExceptionHandler(getInstance());
                }
            }, 0);
        }
    }

    public void shutdownNotify() {
        logger.info("Received shutdown notification (from application).");
        synchronized(lock) {
            shuttingDown = true;
        }
    }

    public void uncaughtException(final Thread thread, Throwable exception) {
        String threadName = thread!=null ? thread.getName() : "<NULL>";

        boolean shuttingDown;
        synchronized(lock) {
            shuttingDown = this.shuttingDown;
        }

        if (shuttingDown) {
            logger.log(Level.FINER, "Exception during shutdown in thread '"+threadName+"'.", exception);
        } else {
            logger.log(Level.WARNING, "Unexpected exception in thread '"+threadName+"'.", exception);    
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ShutdownExceptionHandler.class.getName());

    private final Object lock = new Object();
    private boolean shuttingDown = false;

    private ShutdownExceptionHandler() {
        logger.info("Registering for shutdown notification.");
        final Thread hook = new Thread(new ShutdownHook());
        hook.setName("ShutdownExceptionHandlerHook");
        hook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(hook);
    }

    private static final class InstanceHolder {
        private static ShutdownExceptionHandler INSTANCE = new ShutdownExceptionHandler();
    }

    private final class ShutdownHook implements Runnable {
        public void run() {
            logger.info("Received shutdown notification.");
            synchronized(lock) {
                shuttingDown = true;            
            }
        }
    }
}
