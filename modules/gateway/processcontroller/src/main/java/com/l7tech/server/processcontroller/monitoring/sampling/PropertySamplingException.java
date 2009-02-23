/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

/**
 * Thrown to indicate that a property sample could not be obtained.
 */
public class PropertySamplingException extends Exception {
    private final boolean temporary;

    /**
     * @param temporary <code>true</code> if this is expected to be a temporary failure
     */
    public PropertySamplingException(String message, boolean temporary) {
        super(message);
        this.temporary = temporary;
    }

    /**
     * @param temporary <code>true</code> if this is expected to be a temporary failure
     */
    public PropertySamplingException(String message, Throwable cause, boolean temporary) {
        super(message, cause);
        this.temporary = temporary;
    }

    /**
     * @param temporary <code>true</code> if this is expected to be a temporary failure
     */
    public PropertySamplingException(Throwable cause, boolean temporary) {
        super(cause);
        this.temporary = temporary;
    }

    /**
      * @return <code>true</code> if this is expected to be a temporary failure; <code>false</code> otherwise.
      */
    public boolean isTemporary() {
        return temporary;
    }
}
