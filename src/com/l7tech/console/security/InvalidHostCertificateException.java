package com.l7tech.console.security;

/**
 * Exception thrown when there is a host certificate problem.
 *
 * @author $Author$
 * @version $Revision$
 */
public class InvalidHostCertificateException extends SecurityException {

    public InvalidHostCertificateException() {
        super();
    }

    public InvalidHostCertificateException(String s) {
        super(s);
    }

    public InvalidHostCertificateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidHostCertificateException(Throwable cause) {
        super(cause);
    }

}
