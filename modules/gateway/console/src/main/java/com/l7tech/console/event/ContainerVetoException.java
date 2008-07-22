package com.l7tech.console.event;

import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;

/**
 * Exception used to stop an container action from happening.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ContainerVetoException extends Exception {
    private ContainerEvent event;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param   event the action event
     * @param   message   the detail message.
     */
    public ContainerVetoException(ContainerEvent event, String message) {
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
    public ContainerVetoException(ContainerEvent event, String message, Throwable cause) {
        super(message, cause);
        this.event = event;
    }

    /**
     * Return the container event
     * @return the action event
     */
    public ContainerEvent getEvent() {
        return event;
    }
}
