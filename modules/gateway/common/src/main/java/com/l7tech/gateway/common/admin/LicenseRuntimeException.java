package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.LicenseException;

/**
 * The exception handles the failure of checking license.
 *
 * @Author: ghuang
 */
public class LicenseRuntimeException extends RuntimeAdminException {

    // New exception to conceal original stack trace from LicenseManager
    public LicenseRuntimeException(LicenseException le) {
        super(le.getMessage(), new LicenseException(le.getMessage()));
    }
}
