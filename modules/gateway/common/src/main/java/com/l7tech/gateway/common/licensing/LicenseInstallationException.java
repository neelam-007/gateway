package com.l7tech.gateway.common.licensing;

/**
 * Exception thrown on error attempting to install a new LicenseDocument.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class LicenseInstallationException extends Exception {
    public LicenseInstallationException() {
        super();
    }

    public LicenseInstallationException(String message) {
        super(message);
    }

    public LicenseInstallationException(String message, Throwable cause) {
        super(message, cause);
    }

    public LicenseInstallationException(Throwable cause) {
        super(cause);
    }
}
