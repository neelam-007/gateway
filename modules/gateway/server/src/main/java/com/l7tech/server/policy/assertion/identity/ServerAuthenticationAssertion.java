package com.l7tech.server.policy.assertion.identity;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;

/**
 * SSG implementation of {@link IdentityAssertion}.  Authenticates the request's credentials against
 * a particular identity provider, but does not authorize that the authenticated user matches any particular
 * {@link com.l7tech.identity.User} or {@link com.l7tech.identity.Group}.
 * @author alex
 */
public class ServerAuthenticationAssertion extends ServerIdentityAssertion<AuthenticationAssertion> {
    
    public ServerAuthenticationAssertion(AuthenticationAssertion data, ApplicationContext spring) {
        super(data, spring);
    }

    @Override
    protected AssertionStatus checkUser(AuthenticationResult authResult) {       
        User requestingUser = authResult.getUser();
        Goid requestProvider = requestingUser.getProviderId();

        // provider must always match
        if (!requestProvider.equals(assertion.getIdentityProviderOid())) {
            logAndAudit(AssertionMessages.SPECIFICUSER_PROVIDER_MISMATCH,
                    Goid.toString(requestProvider), Goid.toString(assertion.getIdentityProviderOid()));
            return AssertionStatus.AUTH_FAILED;
        }

        // Doesn't care about the precise identity of the user
        return AssertionStatus.NONE;
    }
}
