/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

/**
 * Thrown to indicate that a property sample could not be obtained.
 */
public class PropertySamplingException extends Exception {
    public PropertySamplingException() {
    }

    public PropertySamplingException(String message) {
        super(message);
    }

    public PropertySamplingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertySamplingException(Throwable cause) {
        super(cause);
    }
}
