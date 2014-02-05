package com.l7tech.server.policy.assertion.identity;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.server.identity.AuthCache;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;

/**
 * Subclasses of ServerIdentityAssertion are responsible for verifying that the entity
 * making a <code>Request</code> (as previously found using a CredentialSourceAssertion)
 * is authorized to do so.
 */
public abstract class ServerIdentityAssertion<AT extends IdentityAssertion> extends AbstractMessageTargetableServerAssertion<AT> {
    private final IdentityProviderFactory identityProviderFactory;

    public ServerIdentityAssertion(AT data, ApplicationContext ctx) {
        super(data);
        if (ctx == null) {
            throw new IllegalArgumentException("Application Context is required");
        }
        this.identityProviderFactory = ctx.getBean("identityProviderFactory", IdentityProviderFactory.class);
    }

    /**
     * Attempts to authenticate the request using the <code>LoginCredentials</code>
     * previously found with a <code>ServerCredentialSourceAssertion</code>, and if
     * successful calls checkUser() to verify that the authenticated user is
     * authorized to make the request.
     */
    @Override
    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) {
        final List<LoginCredentials> pCredentials = authContext.getCredentials();

        if (pCredentials.size() < 1 && authContext.getLastAuthenticatedUser() == null) {
            // No credentials have been found yet
            if (authContext.isAuthenticated()) {
                logAndAudit(AssertionMessages.IDENTITY_AUTHENTICATED_NO_CREDS, messageDescription);
                throw new IllegalStateException("Request is authenticated but request has no LoginCredentials!");
            }

            // Authentication is required for any IdentityAssertion
            // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
            if ( isRequest() )
                context.setAuthenticationMissing();
            logAndAudit(AssertionMessages.IDENTITY_NO_CREDS);
            return AssertionStatus.AUTH_REQUIRED;
        }

        if (Goid.isDefault(assertion.getIdentityProviderOid())) {
            logAndAudit(AssertionMessages.IDENTITY_PROVIDER_NOT_SET);
            throw new IllegalStateException("Can't call checkRequest() when no valid identityProviderGoid has been set!");
        }

        AssertionStatus lastStatus = AssertionStatus.UNDEFINED;
        final IdentityProvider provider;
        try {
            provider = getIdentityProvider();
        } catch (ObjectNotFoundException e) {
            logAndAudit(AssertionMessages.IDENTITY_PROVIDER_NOT_EXIST, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.AUTH_FAILED;
        } catch (FindException e) {
            logAndAudit(AssertionMessages.IDENTITY_PROVIDER_NOT_FOUND, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            // fla fix, allow the policy to continue in case the credentials be valid for
            // another id assertion down the road (fix for bug 374)
            return AssertionStatus.AUTH_FAILED;
        }

        // Check if already authenticated
        if ( assertion.getIdentityTag() == null ) {
            for ( AuthenticationResult authResult : authContext.getUntaggedAuthenticationResults() ) {
                lastStatus = checkUser(authResult);
                if ( isSuccess( lastStatus ) ) {
                    authContext.tagAuthenticationResult( authResult, AuthenticationContext.TAG_NONE );

                    // successful output point
                    return lastStatus;
                }
            }
        } else {
            AuthenticationResult authResult = authContext.getAuthenticationResultForTag( assertion.getIdentityTag() );
            if ( authResult != null ) {
                lastStatus = checkUser(authResult);
                if ( isSuccess( lastStatus ) ) {
                    // successful output point
                    return lastStatus;
                }
            }

            // tag not found or identity did not match, so check if the identity was
            // authenticated as a side-effect of a previous auth assertion
            for ( AuthenticationResult untaggedAuthResult : authContext.getUntaggedAuthenticationResults() ) {
                lastStatus = checkUser(untaggedAuthResult);
                if ( isSuccess( lastStatus ) ) {
                    // Only successful if we manage to add our identity tag
                    if ( authContext.tagAuthenticationResult( untaggedAuthResult, assertion.getIdentityTag() ) ) {
                        // successful output point
                        return lastStatus;
                    }
                }
            }
        }

        // Try available credentials
        for (LoginCredentials pc : pCredentials) {
            try {
                if ( authContext.isSecurityTokenUsed(pc.getSecurityToken()) ) {
                    continue; // don't attempt to authenticate twice with the same token
                }
                lastStatus = validateCredentials(provider, pc, context, authContext);
                if ( isSuccess( lastStatus ) ) {
                    // successful output point
                    return lastStatus;
                }
            } catch (InvalidClientCertificateException icce) {
                logAndAudit(AssertionMessages.IDENTITY_INVALID_CERT, pc.getLogin());
                if ( isRequest() ) {
                    // set some response header so that the CP is made aware of this situation
                    HeadersKnob headersKnob = context.getResponse().getKnob(HeadersKnob.class);
                    if(headersKnob != null) {
                        headersKnob.addHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS,
                                                   SecureSpanConstants.CERT_INVALID);
                    }
                }
                lastStatus = authFailed(pc, icce);
            } catch (MissingCredentialsException mce) {
                if ( isRequest() )
                    context.setAuthenticationMissing();
                lastStatus = authFailed(pc, mce);
            } catch (AuthenticationException ae) {
                logAndAudit(AssertionMessages.IDENTITY_CREDENTIAL_FAILED, pc.getLogin(), ExceptionUtils.getMessage(ae));
                lastStatus = authFailed(pc, ae);
            }
        }
        logAndAudit(AssertionMessages.IDENTITY_AUTHENTICATION_FAILED, assertion.loggingIdentity());
        return lastStatus;
    }

    /**
     * Authenticates and calls {@link #checkUser}.  Override at will.
     */
    protected AssertionStatus validateCredentials( final IdentityProvider provider,
                                                   final LoginCredentials pc,
                                                   final PolicyEnforcementContext context,
                                                   final AuthenticationContext authContext )
        throws AuthenticationException {
        AuthenticationResult authResult = AuthCache.getInstance().getCachedAuthResult(
            pc,
            provider,
            authContext.getAuthSuccessCacheTime(),
            authContext.getAuthFailureCacheTime()
        );
        if (authResult == null) {
            logAndAudit(AssertionMessages.IDENTITY_CREDENTIAL_FAILED, pc.getLogin(), "User not found for credentials");
            return authFailed(pc, null);
        }

        if ( authResult.isCertSignedByStaleCA() && isRequest() ) {
            HeadersKnob hrk = context.getResponse().getKnob(HeadersKnob.class);
            hrk.setHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS, SecureSpanConstants.CERT_STALE);
        }

        User user = authResult.getUser();

        String name = user.getLogin();
        if (name == null) name = user.getName();
        if (name == null) name = user.getSubjectDn();
        if (name == null) name = user.getId();

        // Authentication success
        authContext.addAuthenticationResult(authResult);
        logAndAudit(AssertionMessages.IDENTITY_AUTHENTICATED, name);

        // Make sure this guy matches our criteria
        AssertionStatus status = checkUser(authResult);
        if ( isSuccess( status ) ) {
            authContext.tagAuthenticationResult(
                    authResult,
                    assertion.getIdentityTag()==null ? AuthenticationContext.TAG_NONE : assertion.getIdentityTag() );
        }
        return status;
    }


    private boolean isSuccess( final AssertionStatus status ) {
        return AssertionStatus.NONE.equals( status );
    }

    private AssertionStatus authFailed(LoginCredentials pc, Exception e) {
        // we were losing the details of this authentication failure. important for debugging saml stuff
        logger.log(Level.FINE, "ServerIdentityAssertion failed", e);
        String name = pc.getLogin();
        if (name == null || name.length() == 0) {
            X509Certificate cert = pc.getClientCert();
            if (cert != null) name = cert.getSubjectDN().getName();
        }

        String logid = assertion.loggingIdentity();

        // Preserve old logging behavior until there's a compelling reason to change it
        if (assertion instanceof MemberOfGroup)
            logger.info("could not verify membership of group " + logid + " with credentials from " + name);
        else if (assertion instanceof SpecificUser)
            logger.info("could not verify identity " + logid + " with credentials from " + name);
        else
            logger.info("could not verify " + logid + " with credentials from " + name);

        return AssertionStatus.AUTH_FAILED;
    }

    /**
     * Loads the {@link IdentityProvider} object corresponding to the
     * <code>identityProviderGoid</code> property, using a cache if possible.
     *
     * @return
     * @throws FindException if there was an error retrieving the provider
     * @throws ObjectNotFoundException if the requested provider was not found
     */
    protected IdentityProvider getIdentityProvider() throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(assertion.getIdentityProviderOid());
        if (provider == null) {
            throw new ObjectNotFoundException("id assertion refers to an id provider which does not exist anymore");
        } else {
            return provider;
        }
    }

    /**
     * Implement to decide whether the authenticated user is acceptable.
     */
    protected abstract AssertionStatus checkUser(AuthenticationResult authResult);

}
