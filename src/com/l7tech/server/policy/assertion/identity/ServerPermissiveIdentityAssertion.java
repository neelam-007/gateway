/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.identity.PermissiveIdentityAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Server implementation of {@link PermissiveIdentityAssertion}, which authorizes
 * any {@link User} authenticated by the specified {@link com.l7tech.identity.IdentityProvider}.
 * <p>
 * Used by {@link com.l7tech.server.policy.DefaultGatewayPolicies}.
 * <p>
 * <em>Must never be allowed out of the server!</em>
 * @author alex
 * @version $Revision$
 */
public class ServerPermissiveIdentityAssertion extends ServerIdentityAssertion {
    public ServerPermissiveIdentityAssertion(PermissiveIdentityAssertion ass) {
        super(ass);
    }

    /**
     * @return {@link AssertionStatus#NONE} always. Must not be used in a real policy.
     */
    protected AssertionStatus checkUser(User u, PolicyEnforcementContext context) {
        return AssertionStatus.NONE;
    }
}
