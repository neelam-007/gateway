package com.l7tech.console.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class DefaultErrorEvent extends ErrorEvent {
    protected ErrorHandler[] handlers = new ErrorHandler[0];
    protected int adviceIndex = 0;

    public DefaultErrorEvent(ErrorHandler[] eh, Level level, Throwable t, String message, Logger log) {
        super(level, t, message, log);
        handlers = eh;
    }

    /**
     * Invokes next handler chain.
     */
    public void handle() {
        this.handlers[this.adviceIndex++].handle(this);
    }
}
