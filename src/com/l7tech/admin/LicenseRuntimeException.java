package com.l7tech.admin;

import com.l7tech.common.LicenseException;

/**
 * The exception handles the failure of checking license.
 *
 * @Author: ghuang
 */
public class LicenseRuntimeException extends RuntimeAdminException {

    public LicenseRuntimeException() {
    }

    public LicenseRuntimeException(String string) {
        super(string);
    }

    // New exception to conceal original stack trace from LicenseManager
    public LicenseRuntimeException(LicenseException le) {
        super(le.getMessage(), new LicenseException(le.getMessage()));
    }

    public LicenseRuntimeException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
