package com.l7tech.server.message;

import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.ServerConfig;
import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.User;
import com.l7tech.message.CredentialContext;

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

    public void addAuthenticationResult(final AuthenticationResult authResult) {
        if (authResult == null) throw new NullPointerException("authResult");
        if (!authenticationResults.contains(authResult)) {
            authenticationResults.add(authResult);
        }
        this.lastAuthenticationResult = authResult;
    }

    public List<AuthenticationResult> getAllAuthenticationResults() {
        return authenticationResults;
    }

    public AuthenticationResult getLastAuthenticationResult() {
        return lastAuthenticationResult;
    }

    public AuthenticationResult getAuthenticationResultForX509Certificate( final X509Certificate certificate ) {
        AuthenticationResult result = null;

        for ( AuthenticationResult authenticationResult : authenticationResults ) {
            if ( authenticationResult.getAuthenticatedCert() != null &&
                 CertUtils.certsAreEqual( authenticationResult.getAuthenticatedCert(), certificate )) {
                if ( result == null ) {
                    result = authenticationResult;
                } else {
                    throw new IllegalStateException("Multiple authentication results for certificate '"+certificate.getSubjectDN().getName()+"'.");
                }
            }
        }

        return result;
    }

    public User getLastAuthenticatedUser() {
        return lastAuthenticationResult == null ? null : lastAuthenticationResult.getUser();
    }

    public int getAuthSuccessCacheTime() {
        return ServerConfig.getInstance().getIntPropertyCached(ServerConfig.PARAM_AUTH_CACHE_MAX_SUCCESS_TIME, 1000, 27000L);
    }

    public int getAuthFailureCacheTime() {
        return ServerConfig.getInstance().getIntPropertyCached(ServerConfig.PARAM_AUTH_CACHE_MAX_FAILURE_TIME, 1000, 27000L);
    }

    //- PRIVATE

    private AuthenticationResult lastAuthenticationResult = null;
    private List<AuthenticationResult> authenticationResults = new ArrayList<AuthenticationResult>();
}
