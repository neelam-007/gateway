/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

/** Thrown when the specified Apache policy can't be mapped to a Layer 7 policy. */
public class PolicyConversionException extends Exception {
    public PolicyConversionException() {
    }

    public PolicyConversionException(String message) {
        super(message);
    }

    public PolicyConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PolicyConversionException(Throwable cause) {
        super(cause);
    }
}
