package com.l7tech.server.identity;

import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SessionSecurityToken;

/**
 * Authenticator for use with session based identity tokens.
 */
public class SessionAuthenticator {

    //- PUBLIC

    public SessionAuthenticator( final Goid providerId ) {
        this.providerId = providerId;
    }

    /**
     * Authenticate the given session credentials by verifying the provider id.
     *
     * <p>This method also verifies that the user identifier matches for the login
     * in case the user details have changed since the session was established.</p>
     *
     * @param pc The credentials to authenticate.
     * @param user The expected user.
     * @return null if the user does not authenticate or the AuthenticationResult on success
     * @throws BadCredentialsException If an error occurs
     */
    public AuthenticationResult authenticateSessionCredentials( final LoginCredentials pc,
                                                                final User user ) throws BadCredentialsException {
        final SecurityToken securityToken = pc.getSecurityToken();
        if ( !(securityToken instanceof SessionSecurityToken) ) {
            throw new BadCredentialsException("Unexpected token");
        }

        final SessionSecurityToken token = (SessionSecurityToken) securityToken;
        if ( token.getProviderId().equals(providerId )) {
            return null;
        } else if ( (user.getId()==null&&token.getUserId()!=null) ||
                    (user.getId()!=null&&!user.getId().equals( token.getUserId() )) ) {
            throw new BadCredentialsException( "Session token login does not match identity." );
        }

        return new AuthenticationResult(user, pc.getSecurityTokens());
    }

    /**
     * Get the user identifier from the given credentials.
     *
     * <p>This will verify the provider identifier matches.</p>
     *
     * @param pc The credentials
     * @return The user identifier if present
     */
    public String getUserId( final LoginCredentials pc ) {
        String userId = null;

        final SecurityToken securityToken = pc.getSecurityToken();
        if ( securityToken instanceof SessionSecurityToken ) {
            final SessionSecurityToken token = (SessionSecurityToken) securityToken;
            if ( token.getProviderId().equals(providerId ) ){
                userId = token.getUserId();
            }
        }

        return userId;
    }

    //- PRIVATE

    private final Goid providerId;
}
