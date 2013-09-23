package com.l7tech.server.identity.external;

import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.external.VirtualPolicyUser;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;

/**
 * User manager for policy backed ID provider.
 */
public interface PolicyBackedUserManager extends UserManager<VirtualPolicyUser> {

    void configure(PolicyBackedIdentityProvider provider);

    AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc) throws BadCredentialsException;

}
