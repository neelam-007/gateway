package com.l7tech.policy.assertion.xmlsec;

/**
 * The <code>SamlAuthenticationStatementAssertion</code> assertion describes
 * the SAML Authentication Statement constraints.
 */
public class SamlAuthenticationStatement extends SamlStatementAssertion {
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
