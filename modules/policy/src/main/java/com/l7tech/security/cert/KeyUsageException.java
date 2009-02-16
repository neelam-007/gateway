package com.l7tech.security.cert;

import com.l7tech.security.cert.KeyUsageActivity;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Exception thrown if an activity is not permitted by a certificate's Key Usage or Extended Key Usage extensions.
 */
public class KeyUsageException extends CertificateException {
    private KeyUsageActivity activity;
    private X509Certificate cert;

    public KeyUsageException() {
    }

    public KeyUsageException(String msg) {
        super(msg);
    }

    public KeyUsageException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyUsageException(Throwable cause) {
        super(cause);
    }

    public KeyUsageException(KeyUsageActivity activity, X509Certificate cert) {
        super(cert == null
                ? "Key usage policy forbids activity when corresponding certificate is unknown: " + activity
                : "Certificate key usage or extended key usage disallowed by key usage enforcement policy for activity: " + activity);
        this.activity = activity;
        this.cert = cert;
    }

    public KeyUsageActivity getActivity() {
        return activity;
    }

    public String getActivityName() {
        return activity == null ? "(unknown)" : activity.name();
    }

    public X509Certificate getCertificate() {
        return cert;
    }

    public void setActivity(KeyUsageActivity activity) {
        this.activity = activity;
    }

    public void setCertificate(X509Certificate cert) {
        this.cert = cert;
    }
}
