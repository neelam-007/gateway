package com.l7tech.policy.assertion.xmlsec;

import java.io.Serializable;

/**
 * The <code>SamlAuthenticationStatementAssertion</code> assertion describes
 * the SAML Authentication Statement constraints.
 */
public class SamlAuthenticationStatement implements Serializable {
    private String[] authenticationMethods = new String[] {};

    public SamlAuthenticationStatement() {
    }

    public String[] getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(String[] authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }
}
