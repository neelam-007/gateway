/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * @author mike
 */
public class PolicyLockedException extends Exception {
    private static final String DEFAULT_MESSAGE = "Unable to replace a locked-in policy.";

    public PolicyLockedException() {
        super(DEFAULT_MESSAGE);
    }

    public PolicyLockedException(String message) {
        super(message == null ? DEFAULT_MESSAGE : message);
    }

    public PolicyLockedException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

    public PolicyLockedException(String message, Throwable cause) {
        super(message == null ? DEFAULT_MESSAGE : message, cause);
    }
}
