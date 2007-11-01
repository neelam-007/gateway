package com.l7tech.console.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The error event. Hooks into <code>ExceptionHandler</code> chain via
 * <code>PolicyChange.proceed()</code> method.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ErrorEvent {
    private final long time;
    private final Level level;
    private final Throwable throwable;
    private final String message;
    private final Logger logger;

    /**
     * Create the error event
     * 
     * @param level   the log level
     * @param t       the throwable with the
     * @param message the message
     * @param log     where the message should be logged
     */
    public ErrorEvent(Level level, Throwable t, String message, Logger log) {
        this.time = System.currentTimeMillis();
        this.level = level;
        this.throwable = t;
        this.message = message;
        this.logger = log;
    }

    public long getTime() {
        return time;
    }

    public Level getLevel() {
        return level;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getMessage() {
        return message;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Invokes next handler chain.
     */
    public void handle() {
        throw new UnsupportedOperationException();
    }
}
