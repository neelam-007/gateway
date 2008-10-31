package com.l7tech.server;

/** Exception thrown if a presented EMS cert does not match the previously-registered cert for this EMS. */
public class CertificateMismatchException extends Exception {
    public CertificateMismatchException() {
    }

    public CertificateMismatchException(String message) {
        super(message);
    }

    public CertificateMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificateMismatchException(Throwable cause) {
        super(cause);
    }
}
