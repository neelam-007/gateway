package com.l7tech.console.logging;



/**
 * The <code>ErrorHandler</code> represents an error handler element.
 * It is designed as a <i>Chain of Responibility</i> pattern for
 * Exceptions handling.<br>
 * When the handle() of a ErrorHandler is called, it should
 * either handle the Error event by itself, or invoke next handler by
 * ErrorEvent#handle().
 * <p>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public interface ErrorHandler {
    /**
     * handle the error event
     *
     * @param e the error event
     */
    void handle(ErrorEvent e);
}
