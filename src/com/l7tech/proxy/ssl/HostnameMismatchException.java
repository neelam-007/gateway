/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

/**
 * Exception thrown during our HandshakeCompletedListener if the SSG hostname we connected to doesn't match up with
 * the one in their certificate.
 *
 * User: mike
 * Date: Sep 15, 2003
 * Time: 3:55:55 PM
 */
public class HostnameMismatchException extends RuntimeException {
    public HostnameMismatchException() {
    }

    public HostnameMismatchException(String message) {
        super(message);
    }

    public HostnameMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public HostnameMismatchException(Throwable cause) {
        super(cause);
    }
}
