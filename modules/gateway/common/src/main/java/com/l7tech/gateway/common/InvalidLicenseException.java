/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common;

/**
 * Exception thrown in a license file is not valid.
 * Note the subtle difference between this and {@link LicenseException} -- LicenseException is thrown when an
 * operation is not permitted by an installed license, while {@link InvalidLicenseException} means the license
 * couldn't be installed in the first place.
 */
public class InvalidLicenseException extends Exception {
    public InvalidLicenseException() {
        super();
    }

    public InvalidLicenseException(String message) {
        super(message);
    }

    public InvalidLicenseException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLicenseException(Throwable cause) {
        super(cause);
    }
}
