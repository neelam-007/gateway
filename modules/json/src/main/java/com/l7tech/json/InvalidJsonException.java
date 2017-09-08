package com.l7tech.json;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Thrown when JSON data is not well formed
 *
 * @author darmstrong
 */
public class InvalidJsonException extends Exception {

    public InvalidJsonException(final String message) {
        super(message);
    }

    public InvalidJsonException(final Throwable cause) {
        super(cause);
    }
}
