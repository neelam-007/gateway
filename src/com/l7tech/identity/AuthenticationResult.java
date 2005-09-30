package com.l7tech.identity;

public class AuthenticationResult {
    public AuthenticationResult(User user, boolean certWasSignedByStaleCA) {
        this.user = user;
        if (user == null) throw new NullPointerException();
        this.certSignedByStaleCA = certWasSignedByStaleCA;
    }

    public AuthenticationResult(User user) {
        this(user, false);
    }

    public User getUser() {
        return user;
    }

    public boolean isCertSignedByStaleCA() {
        return certSignedByStaleCA;
    }

    private User user;
    private boolean certSignedByStaleCA;
}
