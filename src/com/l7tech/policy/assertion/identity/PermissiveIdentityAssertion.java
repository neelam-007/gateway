/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

/**
 * Authorizes any {@link com.l7tech.identity.User} authenticated by the specified
 * {@link com.l7tech.identity.IdentityProvider}.
 * <p>
 * Used by {@link com.l7tech.server.policy.DefaultGatewayPolicies}.
 * <p>
 * <em>Must never be allowed out of the server!</em>
 * @author alex
 * @version $Revision$
 */
public class PermissiveIdentityAssertion extends IdentityAssertion {
    public PermissiveIdentityAssertion(long providerOid) {
        super(providerOid);
    }

    public String getForbidden() {
        oops();
        return null;
    }

    public void setForbidden(String s) {
        oops();
    }

    private void oops() {
        throw new RuntimeException(getClass().getName() + " must never be saved or transmitted");
    }
}
