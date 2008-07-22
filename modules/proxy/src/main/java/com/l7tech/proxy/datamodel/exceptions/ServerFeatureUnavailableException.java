/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Thrown when the Gateway can't do something due to a licensing issue.
 */
public class ServerFeatureUnavailableException extends Exception {
    public ServerFeatureUnavailableException() {
        super();
    }

    public ServerFeatureUnavailableException(String message) {
        super(message);
    }

    public ServerFeatureUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerFeatureUnavailableException(Throwable cause) {
        super(cause);
    }
}
