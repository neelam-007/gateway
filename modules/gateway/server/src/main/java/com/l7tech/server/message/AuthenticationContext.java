package com.l7tech.server.message;

import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.ServerConfig;
import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.User;
import com.l7tech.message.CredentialContext;
import com.l7tech.util.Pair;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.xml.processor.X509BinarySecurityTokenImpl;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.util.*;
import java.security.cert.X509Certificate;

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
        addAuthenticationResult( authResult, null, null );
    }

    /**
     * Add an authentication result to the context.
     *
     * @param authResult The result to add.
     * @param loginCredentials The credentials used to authenticate (may be null)
     */
    public void addAuthenticationResult( final AuthenticationResult authResult,
                                         final LoginCredentials loginCredentials ) {
        addAuthenticationResult( authResult, loginCredentials, null );
    }

    /**
     * Add an authentication result to the context.
     *
     * @param authResult The result to add.
     * @param loginCredentials The credentials used to authenticate (may be null)
     * @param identityTag The identity tag to associate with the authentication (may be null)
     */
    public void addAuthenticationResult( final AuthenticationResult authResult,
                                         final LoginCredentials loginCredentials,
                                         final String identityTag ) {
        if (authResult == null) throw new NullPointerException("authResult");
        Pair<AuthenticationResult,String> taggedResult = new Pair<AuthenticationResult,String>(authResult, identityTag);
        if (!authenticationResults.contains(taggedResult)) {
            if ( loginCredentials != null && !isLoginCredentialConsumed(loginCredentials)) {
                consumedCredentials.add( loginCredentials );
            }
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
     * TODO [steve] support for lookup by other signing token types (kerberos, etc)
     *
     * @param token The signing authentication token
     * @param identityTag The identity tag
     * @return The authentication result or null if not found.
     */
    public AuthenticationResult getAuthenticationResultForSigningSecurityToken( final SigningSecurityToken token,
                                                                                final String identityTag ) {
        AuthenticationResult result = null;

        X509Certificate certificate = null;
        if ( token instanceof X509BinarySecurityTokenImpl) {
            X509BinarySecurityTokenImpl x509Token = (X509BinarySecurityTokenImpl) token;
            certificate = x509Token.getMessageSigningCertificate();
        }

        if ( certificate != null ) {
            for ( Pair<AuthenticationResult,String> authResultAndTag : authenticationResults ) {
                if ( authResultAndTag.left.getAuthenticatedCert() != null &&
                     CertUtils.certsAreEqual( authResultAndTag.left.getAuthenticatedCert(), certificate ) &&
                     ( (identityTag == null && authResultAndTag.right == null) || (identityTag!=null && identityTag.equalsIgnoreCase(authResultAndTag.right) ) ) ) {
                    if ( result == null ) {
                        result = authResultAndTag.left;
                    } else {
                        throw new IllegalStateException("Multiple authentication results for certificate '"+certificate.getSubjectDN().getName()+"'.");
                    }
                }
            }
        }
        
        return result;
    }

    public User getLastAuthenticatedUser() {
        return lastAuthenticationResult == null ? null : lastAuthenticationResult.getUser();
    }

    /**
     * Has the given loginCredential been used to authenticate?
     *
     * @param loginCredentials The credentials to check
     * @return True if the credentials have already been used
     */
    public boolean isLoginCredentialConsumed( final LoginCredentials loginCredentials ) {
        boolean consumed = false;

        for ( LoginCredentials credentials : consumedCredentials ) {
            if ( credentials == loginCredentials ) {  // test for same object
                consumed = true;
                break;
            }
        }

        return consumed;
    }

    public int getAuthSuccessCacheTime() {
        return ServerConfig.getInstance().getIntPropertyCached(ServerConfig.PARAM_AUTH_CACHE_MAX_SUCCESS_TIME, 1000, 27000L);
    }

    public int getAuthFailureCacheTime() {
        return ServerConfig.getInstance().getIntPropertyCached(ServerConfig.PARAM_AUTH_CACHE_MAX_FAILURE_TIME, 1000, 27000L);
    }

    //- PRIVATE

    private List<LoginCredentials> consumedCredentials = new ArrayList<LoginCredentials>();
    private AuthenticationResult lastAuthenticationResult = null;
    private List<Pair<AuthenticationResult,String>> authenticationResults = new ArrayList<Pair<AuthenticationResult,String>>();
}
