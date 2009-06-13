package com.l7tech.server.message;

import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.ServerConfig;
import com.l7tech.identity.User;
import com.l7tech.message.CredentialContext;
import com.l7tech.util.Pair;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.SecurityToken;

import java.util.*;

/**
 * CredentialContext plus authentication details.
 */
public class AuthenticationContext extends CredentialContext {

    //- PUBLIC

    public boolean isAuthenticated() {
        return lastAuthenticationResult != null;
    }

    /**
     * Add an authentication result to the context.
     *
     * @param authResult The result to add.
     */
    public void addAuthenticationResult( final AuthenticationResult authResult ) {
        addAuthenticationResult( authResult, null );
    }

    /**
     * Add an authentication result to the context.
     *
     * @param authResult The result to add.
     * @param identityTag The identity tag to associate with the authentication (may be null)
     */
    public void addAuthenticationResult( final AuthenticationResult authResult,
                                         final String identityTag ) {
        if (authResult == null) throw new NullPointerException("authResult");
        Pair<AuthenticationResult,String> taggedResult = new Pair<AuthenticationResult,String>(authResult, identityTag);
        if (!authenticationResults.contains(taggedResult)) {
            authenticationResults.add(taggedResult);
        }
        this.lastAuthenticationResult = authResult;
    }

    public List<AuthenticationResult> getAllAuthenticationResults() {
        List<AuthenticationResult> authResults = new ArrayList<AuthenticationResult>();
        for ( Pair<AuthenticationResult,String> taggedResult : authenticationResults ) {
            if (!authResults.contains(taggedResult.left)) {
                authResults.add(taggedResult.left);
            }
        }
        return authResults;
    }

    public List<AuthenticationResult> getUntaggedAuthenticationResults() {
        List<AuthenticationResult> authResults = new ArrayList<AuthenticationResult>();
        for ( Pair<AuthenticationResult,String> taggedResult : authenticationResults ) {
            if ( taggedResult.right==null && !authResults.contains(taggedResult.left) ) {
                authResults.add(taggedResult.left);
            }
        }
        return authResults;
    }

    /**
     * Get the authentication result for the given identity tag.
     *
     * @param identityTag The identity tag.
     * @return The associated authentication result, or null
     */
    public AuthenticationResult getAuthenticationResultForTag( final String identityTag ) {
        AuthenticationResult result = null;

        if ( identityTag != null ) {
            for ( Pair<AuthenticationResult,String> taggedResult : authenticationResults ) {
                if ( identityTag.equalsIgnoreCase( taggedResult.right ) ) {
                    result = taggedResult.left;
                    break;
                }
            }
        }

        return result;

    }

    /**
     * Get the last authentication result.
     *
     * @return The latest authentication result or null if there is none
     */
    public AuthenticationResult getLastAuthenticationResult() {
        return lastAuthenticationResult;
    }

    /**
     * Get the authentication result for the given token and tag.
     *
     * @param token The signing authentication token
     * @param identityTag The identity tag
     * @return The authentication result or null if not found.
     */
    public AuthenticationResult getAuthenticationResultForSigningSecurityToken( final SigningSecurityToken token,
                                                                                final String identityTag ) {
        // It is important that this method finds the AR that matched the given
        // token (not certificate) else signature combination attacks can succeed
        AuthenticationResult result = null;
        String resultTag = null;

        if ( token != null ) {
            for ( Pair<AuthenticationResult,String> authResultAndTag : authenticationResults ) {
                if ( authResultAndTag.left.matchesSecurityToken( token ) ) {
                    if ( result == null ) {
                        result = authResultAndTag.left;
                        resultTag = authResultAndTag.right;
                    } else {
                        throw new IllegalStateException("Multiple authentication results for token '"+token.getClass()+"' of type '"+token.getType()+"'.");
                    }
                }
            }
        }

        if ( result != null &&
             (identityTag == null && resultTag != null) || (identityTag!=null && !identityTag.equalsIgnoreCase(resultTag) ) ) {
            result = null; // tags don't match
        }
        
        return result;
    }

    public User getLastAuthenticatedUser() {
        return lastAuthenticationResult == null ? null : lastAuthenticationResult.getUser();
    }

    /**
     * Has the given security token been used to authenticate?
     *
     * @param securityToken The token to check
     * @return True if the security token has already been used
     */
    public boolean isSecurityTokenUsed( final SecurityToken securityToken ) {
        boolean used = false;

        for ( Pair<AuthenticationResult,String> taggedResult : authenticationResults ) {
            if ( taggedResult.left.matchesSecurityToken(securityToken) ) {
                used = true;
                break;
            }
        }

        return used;
    }

    public int getAuthSuccessCacheTime() {
        return ServerConfig.getInstance().getIntPropertyCached(ServerConfig.PARAM_AUTH_CACHE_MAX_SUCCESS_TIME, 1000, 27000L);
    }

    public int getAuthFailureCacheTime() {
        return ServerConfig.getInstance().getIntPropertyCached(ServerConfig.PARAM_AUTH_CACHE_MAX_FAILURE_TIME, 1000, 27000L);
    }

    //- PRIVATE

    private AuthenticationResult lastAuthenticationResult = null;
    private List<Pair<AuthenticationResult,String>> authenticationResults = new ArrayList<Pair<AuthenticationResult,String>>();
}
