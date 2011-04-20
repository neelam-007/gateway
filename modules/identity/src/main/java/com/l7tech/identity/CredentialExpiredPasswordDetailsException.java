package com.l7tech.identity;

import javax.security.auth.login.FailedLoginException;

/**
 * User: wlui
 */

public class CredentialExpiredPasswordDetailsException extends FailedLoginException {
    private String passwordPolicyDescription = null;

    public CredentialExpiredPasswordDetailsException() {
        super();
    }

    public CredentialExpiredPasswordDetailsException(String msg) {
        super(msg);
    }

    public CredentialExpiredPasswordDetailsException(String msg, String passwordPolicyDescription) {
        super(msg);
        this.passwordPolicyDescription = passwordPolicyDescription;
    }

    public String getPasswordPolicyDescription() {
        return passwordPolicyDescription;
    }

}
