/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Exception thrown if a keystore is determined to be corrupt.
 * @author mike
 * @version 1.0
 */
public class KeyStoreCorruptException extends Exception {
    public KeyStoreCorruptException() {
    }

    public KeyStoreCorruptException(String message) {
        super(message);
    }

    public KeyStoreCorruptException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyStoreCorruptException(Throwable cause) {
        super(cause);
    }
}
