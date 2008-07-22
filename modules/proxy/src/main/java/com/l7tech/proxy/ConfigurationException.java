/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

/**
 * Thrown if an operation could not continue due to a problem with the client or server configuration,
 * and retrying the operation is unlikely to succeed until the configuration is changed.
 */
public class ConfigurationException extends Exception {
    public ConfigurationException() {
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
