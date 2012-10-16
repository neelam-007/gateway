package com.l7tech.console.logging;

import com.l7tech.console.security.LogonListener;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles multiple related error events.
 *
 * <p>This handler ensures that only one error is processed at a time.</p>
 *
 * <p>If any errors occur during a short window after disconnection from the
 * gateway then they are suppressed (these errors are assumed to be related
 * to the disconnection)</p>
 *
 * <p>If there are many errors in a short amount of time then it is assumed
 * that something is wrong and the manager will exit (perhaps something is
 * critically wrong with the error dialog).</p>
 *
 * <p>If errors occur during error handling (reentrancy) then this is logged
 * at severe. No further processing occurs for such errors since it seems
 * likely to fail.</p>
 * 
 * @author Steve Jones
 */
public class CascadingErrorHandler implements ErrorHandler, LogonListener {

    //- PUBLIC

    /**
     * Handle the given error event.
     *
     * <p>If the error is ignorable it will be logged but not shown
     * to the user.</p>
     *
     * <p>Only one error at a time will be processed.</p>
     *
     * @param e The error event
     */
    @Override
    public void handle( final ErrorEvent e ) {
        if (e != null) {
            final Runnable handler = new Runnable(){
                @Override
                public void run() {
                    errorTimes[errorTimeIndex++] = e.getTime();
                    errorTimeIndex %= ERROR_COUNT;

                    if ( processingError ) {
                        // reentrant, so error during error handling
                        // this can only occur if someone calls ErrorManager.notify inappropriately
                        Logger logger = e.getLogger();
                        if ( logger != null ) {
                            log(e);
                            logger.log(Level.SEVERE, "Error during error handling.");
                        }
                    } else {
                        processingError = true;
                        try {
                            long timeNow = System.currentTimeMillis();

                            // check for error after disconnection (so probably caused by it)
                            long disconnectTime = getDisconnectTime();
                            if ( (disconnectTime+DISCONNECT_SUPPRESS_PERIOD) > timeNow ) {
                                Logger logger = e.getLogger();
                                if ( logger != null ) {
                                    log(e);
                                    logger.log(Level.INFO, "Suppressing error due to disconnection from gateway.");
                                }
                            } else {
                                // check for excessive errors in short amount of time
                                long oldErrorTime = errorTimes[errorTimeIndex];
                                if ( (oldErrorTime+ERROR_PERIOD) > timeNow ) {
                                    Logger logger = e.getLogger();
                                    if ( logger != null ) {
                                        log(e);
                                    }
                                    performShutdown(logger);
                                } else {
                                    boolean loggedOn = getLogonStatus();
                                    e.handle();
                                    if ( loggedOn && !getLogonStatus() ) {
                                        stampDisconnectTime(); // allow for this time after error dialog is closed
                                    }
                                }
                            }
                        } finally {
                            processingError = false;
                        }
                    }
                }
            };

            Utilities.invokeOnSwingThreadAndWait( handler );
        }
    }

    /**
     * Notification of log off.
     *
     * @param e The event
     */
    @Override
    public void onLogoff(final LogonEvent e) {
        stampDisconnectTime();
        setLogonStatus(false);
    }

    /**
     * Notification of log on.
     *
     * @param e The event
     */
    @Override
    public void onLogon(final LogonEvent e) {
        setLogonStatus(true);
    }

    public static void setShutdownHandler(Runnable handler) {
        if (handler == null)
            throw new IllegalArgumentException("handler must not be null");

        synchronized(shutdownLock) {
            shutdownHandler = handler;
        }
    }

    //- PRIVATE

    /**
     * The number of errors to keep track of
     */
    private static final int ERROR_COUNT = 5;

    /**
     * The length of the error window (millis)
     */
    private static final long ERROR_PERIOD = 500L;

    /**
     * The amount of time to suppress errors after a disconnection (millis)
     */
    private static final long DISCONNECT_SUPPRESS_PERIOD = 2000L;


    /**
     * Handler for shutting down
     */
    private static final Object shutdownLock = new Object();
    private static Runnable shutdownHandler = new Runnable() {
        @Override
        public void run(){System.exit(-1);}};

    private boolean processingError = false;
    private long[] errorTimes = new long[ERROR_COUNT];
    private int errorTimeIndex = 0;

    private final Object disconnectTimeLock = new Object();
    private long disconnectTime;
    private boolean loggedOn = false;

    /**
     *
     */
    private void stampDisconnectTime() {
        synchronized( disconnectTimeLock ) {
            disconnectTime = System.currentTimeMillis();
        }
    }

    /**
     *
     */
    private long getDisconnectTime() {
        synchronized (disconnectTimeLock) {
            return disconnectTime;
        }
    }

    /**
     *
     */
    private void setLogonStatus(boolean status) {
        synchronized( disconnectTimeLock ) {
            loggedOn = status;
        }
    }

    /**
     *
     */
    private boolean getLogonStatus() {
        synchronized( disconnectTimeLock ) {
            return loggedOn;
        }
    }

    /**
     *
     */
    private void performShutdown(Logger logger) {
        if (logger!=null)
            logger.log(Level.SEVERE, "Exiting due to excessive errors.");
        try {
            Runnable shutdownTask;
            synchronized(shutdownLock) {
                shutdownTask = shutdownHandler;
            }
            shutdownTask.run();
        } catch (Throwable throwable) {
            if ( logger!=null && logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE,
                        "Error shutting down.", 
                        ExceptionUtils.getDebugException(throwable));
            }
        }
    }

    /**
     * Log the given event if a logger and level are set. 
     */
    private void log(ErrorEvent e) {
        Level level = e.getLevel();
        Logger logger = e.getLogger();

        if (level != null && logger != null) {            
            logger.log(level, e.getMessage(), e.getThrowable());
        }
    }
}
