/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Exception thrown if the operation is canceled by the user.
 * User: mike
 * Date: Aug 13, 2003
 * Time: 9:50:42 AM
 */
public class OperationCanceledException extends Exception {
    public OperationCanceledException() {
    }

    public OperationCanceledException(String message) {
        super(message);
    }

    public OperationCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationCanceledException(Throwable cause) {
        super(cause);
    }
}
