/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.identity;

import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import org.springframework.context.ApplicationContext;

/**
 * SSG implementation of {@link IdentityAssertion}.  Authenticates the request's credentials against
 * a particular identity provider, but does not authorize that the authenticated user matches any particular
 * {@link com.l7tech.identity.User} or {@link com.l7tech.identity.Group}.
 * @author alex
 */
public class ServerAuthenticationAssertion extends ServerIdentityAssertion {
    public ServerAuthenticationAssertion(AuthenticationAssertion data, ApplicationContext spring) {
        super(data, spring);
    }
}
