package com.l7tech.common.io;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

/**
 * An X509 Trust manager that trusts everything.
 * <p/>
 * <b>THIS DOES NOT CHECK THE SERVER CERTIFICATE.
 * DO NOT USE THIS IN PRODUCTION TLS CODE UNLESS YOU ARE DOING YOUR OWN CERT OR PIN CHECKING!
 * If you don't know whether you are doing your own cert or pin checking, you probably are not!
 * </b>
 * See "The most dangerous code in the world: validating SSL certificates in non-browser software"
 * http://crypto.stanford.edu/~dabo/pubs/abstracts/ssl-client-bugs.html
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
