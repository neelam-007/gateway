/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

/** Thrown when a document cannot be processed in hardware for some reason but should be retried in software. */
public class SoftwareFallbackException extends Exception {
    public SoftwareFallbackException() {
    }

    public SoftwareFallbackException(String message) {
        super(message);
    }

    public SoftwareFallbackException(Throwable cause) {
        super(cause);
    }

    public SoftwareFallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
