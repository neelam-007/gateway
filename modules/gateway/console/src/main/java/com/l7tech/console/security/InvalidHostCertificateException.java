package com.l7tech.console.security;

import java.security.cert.X509Certificate;

/**
 * Exception thrown when there is a host certificate problem.
 *
 * @author $Author$
 * @version $Revision$
 */
public class InvalidHostCertificateException extends SecurityException {

    //- PUBLIC

    public InvalidHostCertificateException( final X509Certificate certificate ) {
        super();
        this.certificate = certificate;
    }

    public InvalidHostCertificateException( final X509Certificate certificate, final String s ) {
        super(s);
        this.certificate = certificate;
    }

    public InvalidHostCertificateException( final X509Certificate certificate, final String message, final Throwable cause ) {
        super(message, cause);
        this.certificate = certificate;
    }

    public InvalidHostCertificateException( final X509Certificate certificate, final Throwable cause ) {
        super(cause);
        this.certificate = certificate;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    //- PRIVATE

    private final X509Certificate certificate;
}
