/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

/**
 * Exception thrown when an asked-for MIME multipart part does not exist.
 */
public class NoSuchPartException extends Exception {
    public NoSuchPartException() {
    }

    public NoSuchPartException(String message) {
        super(message);
    }

    public NoSuchPartException(Throwable cause) {
        super(cause);
    }

    public NoSuchPartException(String message, Throwable cause) {
        super(message, cause);
    }
}
