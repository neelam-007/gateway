/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

/**
 * Exception thrown by WssProcessor or ProcessorResultUtil if a fatal error is encountered during processing.
 */
public class ProcessorException extends Exception {
    public ProcessorException() {
    }

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(Throwable cause) {
        super(cause);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
