package com.l7tech.common.io;

/**
 * Exception thrown when the CertificateGenerator is unable to generate a certificate.
 */
public class CertificateGeneratorException extends Exception {
    public CertificateGeneratorException() {
    }

    public CertificateGeneratorException(String message) {
        super(message);
    }

    public CertificateGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificateGeneratorException(Throwable cause) {
        super(cause);
    }
}
