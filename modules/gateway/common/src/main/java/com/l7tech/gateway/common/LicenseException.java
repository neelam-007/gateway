/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common;

import com.l7tech.util.ExceptionUtils;

/**
 * Exception thrown if an operation is not permitted by any currently-installed license.
 * Note the subtle difference between this and {@link LicenseException} -- LicenseException is thrown when an
 * operation is not permitted by an installed license, while {@link InvalidLicenseException} means the license
 * couldn't be installed in the first place.
 */
public class LicenseException extends Exception {
    private static final String MSG = "Operation not enabled by any currently-installed license";

    public LicenseException() {
        super(MSG);
    }

    public LicenseException(String message) {
        super(message == null ? MSG : message);
    }

    public LicenseException(String message, Throwable cause) {
        super(message == null ? ExceptionUtils.getMessage(cause, MSG) : message, cause);
    }

    public LicenseException(Throwable cause) {
        super(ExceptionUtils.getMessage(cause, MSG), cause);
    }
}
