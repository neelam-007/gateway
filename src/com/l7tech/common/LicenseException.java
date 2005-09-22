/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common;

/**
 * Exception thrown if an operation is not permitted by any currently-installed license.
 */
public class LicenseException extends Exception {
    private static final String MSG = "Operation not enabled by any currently-installed license";

    public LicenseException() {
        super(MSG);
    }

    public LicenseException(String message) {
        super(message == null ? MSG : MSG + ": " + message);
    }

    public LicenseException(String message, Throwable cause) {
        super(message == null ? MSG : MSG + ": " + message, cause);
    }

    public LicenseException(Throwable cause) {
        super(MSG, cause);
    }
}
