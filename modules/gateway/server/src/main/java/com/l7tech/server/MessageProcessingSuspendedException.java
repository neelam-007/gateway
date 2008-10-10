/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server;

/*
 * Thrown when message processing has been suspended (e.g. audit logs cannot be saved).
 *
 * @author jbufu
 */
public class MessageProcessingSuspendedException extends Exception {

    public MessageProcessingSuspendedException() {
        super();
    }

    public MessageProcessingSuspendedException(String message) {
        super(message);
    }

    public MessageProcessingSuspendedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageProcessingSuspendedException(Throwable cause) {
        super(cause);
    }
}

