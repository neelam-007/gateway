/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Thrown if a problem is encountered during policy processing, but it should now be safe to
 * retry processing the policy from the start.
 * User: mike
 * Date: Aug 13, 2003
 * Time: 9:56:47 AM
 */
public class PolicyRetryableException extends Exception {
    private static final String DEFAULT_MESSAGE = "Retry policy operation.";

    public PolicyRetryableException() {
        super(DEFAULT_MESSAGE);
    }

    public PolicyRetryableException(String message) {
        super(message != null ? message : DEFAULT_MESSAGE);
    }

    public PolicyRetryableException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

    public PolicyRetryableException(String message, Throwable cause) {
        super(message != null ? message : DEFAULT_MESSAGE, cause);
    }
}
