package com.l7tech.policy.assertion.xmlsec;

import java.io.Serializable;

/**
 * The <code>SamlAuthenticationStatementAssertion</code> assertion describes
 * the SAML Authentication Statement constraints.
 */
public class SamlAuthenticationStatement implements Cloneable, Serializable {
    private String[] authenticationMethods = new String[] {};
    private String customAuthenticationMethods = "";
    private boolean includeAuthenticationContextDeclaration = true;

    public SamlAuthenticationStatement() {
    }

    public String[] getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(String[] authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public String getCustomAuthenticationMethods() {
        return customAuthenticationMethods;
    }

    public void setCustomAuthenticationMethods(String customAuthenticationMethods) {
        this.customAuthenticationMethods = customAuthenticationMethods;
    }

    public boolean isIncludeAuthenticationContextDeclaration() {
        return includeAuthenticationContextDeclaration;
    }

    public void setIncludeAuthenticationContextDeclaration( final boolean includeAuthenticationContextDeclaration ) {
        this.includeAuthenticationContextDeclaration = includeAuthenticationContextDeclaration;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        }
        catch(CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone error");
        }
    }
}
