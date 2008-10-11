package com.l7tech.common.io;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

/**
 * An X509 Trust manager that trusts everything.
 */
public class PermissiveX509TrustManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        // permissive
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        // permissive
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
