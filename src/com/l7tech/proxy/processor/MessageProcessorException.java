/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.processor;

/**
 * Superclass for exceptions that may be thrown during Client Proxy message processing.
 * User: mike
 * Date: Aug 13, 2003
 * Time: 10:08:48 AM
 */
public class MessageProcessorException extends Exception {
    public MessageProcessorException() {
    }

    public MessageProcessorException(String message) {
        super(message);
    }

    public MessageProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageProcessorException(Throwable cause) {
        super(cause);
    }
}
