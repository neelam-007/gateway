package com.l7tech.message;

import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.util.*;
import java.util.logging.Logger;

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
     * TODO [steve] allow multiple credentials for some types (optionally). 
     */
    public void addCredentials(LoginCredentials credentials) {
        for (LoginCredentials l : this.credentials) {
            if (l.getCredentialSourceAssertion() == null && credentials.getCredentialSourceAssertion() == null) {
                logger.warning("A credential of type null was already added in this context");
                return;
            } else if (l.getCredentialSourceAssertion().equals(credentials.getCredentialSourceAssertion().getClass())) {
                logger.warning("A credential of type " + l.getCredentialSourceAssertion().getName() +
                               " was already added in this context");
                return;
            }
        }
        this.credentials.add(credentials);
        lastCredentials = credentials;
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

    private static final Logger logger = Logger.getLogger(CredentialContext.class.getName());

    private final List<LoginCredentials> credentials = new ArrayList<LoginCredentials>();
    private LoginCredentials lastCredentials;
    private boolean isAuthenticationMissing = false;
}
