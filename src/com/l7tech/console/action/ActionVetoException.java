package com.l7tech.console.action;

import java.awt.event.ActionEvent;

/**
 * Exception used to stop an action from happening.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ActionVetoException extends Exception {
    private ActionEvent event;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param   event the action event
     * @param   message   the detail message.
     */
    public ActionVetoException(ActionEvent event, String message) {
        this(event, message, null);
    }

    /**
     * Constructs a new exception with the specified event, detail
     * message and the cause.
     *
     * @param  event the action event
     * @param  message the detail message
     * @param  cause the cause (A <tt>null</tt> value is permitted,
     *         and indicates that the cause is nonexistent or
     *         unknown.)
     * @since
     */
    public ActionVetoException(ActionEvent event, String message, Throwable cause) {
        super(message, cause);
        this.event = event;
    }

    /**
     * Return the action event
     * @return the action event
     */
    public ActionEvent getEvent() {
        return event;
    }
}
