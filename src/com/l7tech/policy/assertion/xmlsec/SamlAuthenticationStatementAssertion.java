package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * The <code>SamlAuthenticationStatementAssertion</code> assertion describes
 * the SAML Authentication Statement constraints.
 */
public class SamlAuthenticationStatementAssertion extends SamlStatementAssertion {
    private String[] authenticationMethods = new String[] {};

    public SamlAuthenticationStatementAssertion() {
    }

    public String[] getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(String[] authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }
}
