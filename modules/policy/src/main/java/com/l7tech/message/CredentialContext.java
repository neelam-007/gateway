package com.l7tech.message;

import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.util.*;

/**
 * Context for credential information.
 */
public class CredentialContext {

    //- PUBLIC

    /**
     * Returns only one set of credentials.
     *
     * @return null if there are no credentials present, a LoginCredentials if there is only one present
     */
    public LoginCredentials getLastCredentials() {
        return lastCredentials;
    }

    public List<LoginCredentials> getCredentials() {
        return credentials;
    }

    /**
     *
     */
    public void addCredentials( final LoginCredentials credential ) {
        credentials.add( credential );
        lastCredentials = credential;
    }

    /**
     * Check if some authentication credentials that were expected in the request were not found.
     * (For PolicyEnforcementContext, this implies PolicyViolated, as well.)
     *
     * @return true if the policy expected credentials, but they weren't present
     */
    public boolean isAuthenticationMissing() {
        return isAuthenticationMissing;
    }

    /**
     * Report that some authentication credentials that were expected in the request were not found.
     * This implies requestPolicyViolated, as well.
     */
    public void setAuthenticationMissing() {
        isAuthenticationMissing = true;
    }

    //- PRIVATE

    private final List<LoginCredentials> credentials = new ArrayList<LoginCredentials>();
    private LoginCredentials lastCredentials;
    private boolean isAuthenticationMissing = false;
}
