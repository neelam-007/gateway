package com.l7tech.gateway.common.licensing;

/**
 * Exception thrown on error attempting to uninstall an existing LicenseDocument.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class LicenseRemovalException extends Exception {
    public LicenseRemovalException() {
        super();
    }

    public LicenseRemovalException(String message) {
        super(message);
    }

    public LicenseRemovalException(String message, Throwable cause) {
        super(message, cause);
    }

    public LicenseRemovalException(Throwable cause) {
        super(cause);
    }
}
