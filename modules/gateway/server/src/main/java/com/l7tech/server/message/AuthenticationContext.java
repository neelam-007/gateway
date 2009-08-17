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

    /**
     * Identity tag for use when authenticating without an identity tag.
     *
     * <p>Results should be tagged with this in the case that there is
     * no specified identity tag to prevent re-tagging.</p>
     */
    public static final String TAG_NONE = "";


    /**
     * Is there an authenticated user?
     *
     * @return True if an identity was authenticated (but not necessarily authorized)
     */
    public boolean isAuthenticated() {
        return lastAuthenticationResult != null;
    }

    /**
     * Add an authentication result to the context.
     *
     * @param authResult The result to add.
     */
    public void addAuthenticationResult( final AuthenticationResult authResult ) {
        if (authResult == null) throw new NullPointerException("authResult");
        Pair<AuthenticationResult,String> taggedResult = new Pair<AuthenticationResult,String>(authResult, null);
        if (!authenticationResults.contains(taggedResult)) {
            authenticationResults.add(taggedResult);
        }
        this.lastAuthenticationResult = authResult;
    }

    /**
     * Tag an authentication result in the context.
     *
     * <p>An authentication result can only be tagged once.</p>
     *
     * @param authResult The result to add.
     * @param identityTag The identity tag to associate with the authentication (must not be null)
     * @return true if an authentication result was tagged.
     */
    public boolean tagAuthenticationResult( final AuthenticationResult authResult,
                                            final String identityTag ) {
        boolean tagged = false;
        if (identityTag == null) throw new IllegalArgumentException("identityTag is required");

        if (authResult != null ) {
            Pair<AuthenticationResult,String> untaggedResult = new Pair<AuthenticationResult,String>(authResult, null);
            int index = authenticationResults.indexOf( untaggedResult );
            if ( index > -1 ) {
                authenticationResults.remove( index );
                authenticationResults.add( index, new Pair<AuthenticationResult,String>(authResult, identityTag) );
                tagged = true;
            }
        }

        return tagged;
    }

    /**
     * Get a list of unique authentication results.
     *
     * @return The list (may be empty but never null)
     */
    public List<AuthenticationResult> getAllAuthenticationResults() {
        List<AuthenticationResult> authResults = new ArrayList<AuthenticationResult>();
        for ( Pair<AuthenticationResult,String> taggedResult : authenticationResults ) {
            if (!authResults.contains(taggedResult.left)) {
                authResults.add(taggedResult.left);
            }
        }
        return authResults;
    }

    /**
     * Get a list of authentication results with no identity tag.
     *
     * @return The list (may be empty but never null)
     */
    public List<AuthenticationResult> getUntaggedAuthenticationResults() {
        List<AuthenticationResult> authResults = new ArrayList<AuthenticationResult>();
        for ( Pair<AuthenticationResult,String> taggedResult : authenticationResults ) {
            if ( normalizeTag(taggedResult.right)==null && !authResults.contains(taggedResult.left) ) {
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
                if ( identityTag.equalsIgnoreCase( normalizeTag(taggedResult.right) ) ) {
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
                        resultTag = normalizeTag(authResultAndTag.right);
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

    /**
     * Normalize an identity tag by replacing the empty tag with null.
     */
    private String normalizeTag( final String identityTag ) {
        String resultTag = identityTag;

        if ( resultTag!=null && resultTag.isEmpty() ) {
            resultTag = null;
        }

        return resultTag;
    }
}
