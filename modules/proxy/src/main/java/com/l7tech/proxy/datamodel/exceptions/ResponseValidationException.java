/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Exception thrown when a signed response could not be verified.
 *
 * User: mike
 * Date: Sep 17, 2003
 * Time: 4:53:11 PM
 */
public class ResponseValidationException extends Exception {
    public ResponseValidationException() {
    }

    public ResponseValidationException(String message) {
        super(message);
    }

    public ResponseValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResponseValidationException(Throwable cause) {
        super(cause);
    }
}
