/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Exception used to report an unacceptable password format (too short, too easy, too similar to previous password, etc)
 * in situations where this matters.
 */
public class BadPasswordFormatException extends Exception {
    public BadPasswordFormatException() {
    }

    public BadPasswordFormatException(String message) {
        super(message);
    }

    public BadPasswordFormatException(Throwable cause) {
        super(cause);
    }

    public BadPasswordFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
