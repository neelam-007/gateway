/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import java.io.IOException;

/**
 * Thrown when the message response sent by the backend service generated an IOException.
 *
 * @author jbufu
 */
public class MessageResponseIOException extends IOException {

    public MessageResponseIOException() {
        super();
    }

    public MessageResponseIOException(String message) {
        super(message);
    }

    public MessageResponseIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageResponseIOException(Throwable cause) {
        super(cause);
    }
}


