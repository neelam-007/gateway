/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.identity;

/**
 * Authenticates the credentials against a specified provider, but does not authorize any particular
 * user or group.  Otherwise known as "Wildcard Identity Assertion."
 * @author alex
 */
public class AuthenticationAssertion extends IdentityAssertion {
    private String loggingIdentity;

    public AuthenticationAssertion() {
    }

    public AuthenticationAssertion(long providerOid) {
        super(providerOid);
        updateLoggingIdentity();
    }


    public void setIdentityProviderOid(long provider) {
        super.setIdentityProviderOid(provider);
        updateLoggingIdentity();
    }

    private void updateLoggingIdentity() {
        long poid = getIdentityProviderOid();
        if (-1 == poid)
            loggingIdentity = "default identity provider";
        else
            loggingIdentity = "identity provider ID " + poid;
    }

    public String loggingIdentity() {
        return loggingIdentity;
    }
}
