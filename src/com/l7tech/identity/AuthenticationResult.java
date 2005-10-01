package com.l7tech.identity;

import java.security.cert.X509Certificate;

public class AuthenticationResult {
    public AuthenticationResult(User user, X509Certificate authenticatedCert, boolean certWasSignedByStaleCA) {
        this.user = user;
        if (user == null) throw new NullPointerException();
        this.certSignedByStaleCA = certWasSignedByStaleCA;
        this.authenticatedCert = authenticatedCert;
        if (certSignedByStaleCA && authenticatedCert == null) throw new IllegalArgumentException("Must include the stale cert if there is one");
    }

    public AuthenticationResult(User user) {
        this(user, null, false);
    }

    public User getUser() {
        return user;
    }

    public boolean isCertSignedByStaleCA() {
        return certSignedByStaleCA;
    }

    public X509Certificate getAuthenticatedCert() {
        return authenticatedCert;
    }

    public void setAuthenticatedCert(X509Certificate authenticatedCert) {
        this.authenticatedCert = authenticatedCert;
    }

    private final User user;
    private final boolean certSignedByStaleCA;
    private X509Certificate authenticatedCert;
}
