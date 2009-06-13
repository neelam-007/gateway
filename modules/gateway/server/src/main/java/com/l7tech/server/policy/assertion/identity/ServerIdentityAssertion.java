/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.identity;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.server.audit.Auditor;
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
import java.util.logging.Logger;

/**
 * Subclasses of ServerIdentityAssertion are responsible for verifying that the entity
 * making a <code>Request</code> (as previously found using a CredentialSourceAssertion)
 * is authorized to do so.
 */
public abstract class ServerIdentityAssertion<AT extends IdentityAssertion> extends AbstractMessageTargetableServerAssertion<AT> {
    private final Logger logger = Logger.getLogger(ServerIdentityAssertion.class.getName());

    protected final Auditor auditor;
    private final IdentityProviderFactory identityProviderFactory;

    public ServerIdentityAssertion(AT data, ApplicationContext ctx) {
        super(data, data);
        if (ctx == null) {
            throw new IllegalArgumentException("Application Context is required");
        }
        this.auditor = new Auditor(this, ctx, Logger.getLogger(getClass().getName()));
        this.identityProviderFactory = (IdentityProviderFactory) ctx.getBean("identityProviderFactory", IdentityProviderFactory.class);
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
                auditor.logAndAudit(AssertionMessages.IDENTITY_AUTHENTICATED_NO_CREDS, messageDescription);
                throw new IllegalStateException("Request is authenticated but request has no LoginCredentials!");
            }

            // Authentication is required for any IdentityAssertion
            // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
            if ( isRequest() )
                context.setAuthenticationMissing();
            auditor.logAndAudit(AssertionMessages.IDENTITY_NO_CREDS);
            return AssertionStatus.AUTH_REQUIRED;
        }

        if (assertion.getIdentityProviderOid() == IdentityProviderConfig.DEFAULT_OID) {
            auditor.logAndAudit(AssertionMessages.IDENTITY_PROVIDER_NOT_SET);
            throw new IllegalStateException("Can't call checkRequest() when no valid identityProviderOid has been set!");
        }

        AssertionStatus lastStatus = AssertionStatus.UNDEFINED;
        final IdentityProvider provider;
        try {
            provider = getIdentityProvider();
        } catch (ObjectNotFoundException e) {
            auditor.logAndAudit(AssertionMessages.IDENTITY_PROVIDER_NOT_EXIST, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.AUTH_FAILED;
        } catch (FindException e) {
            auditor.logAndAudit(AssertionMessages.IDENTITY_PROVIDER_NOT_FOUND, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            // fla fix, allow the policy to continue in case the credentials be valid for
            // another id assertion down the road (fix for bug 374)
            return AssertionStatus.AUTH_FAILED;
        }

        // Check if already authenticated
        if ( assertion.getIdentityTag() == null ) {
            for ( AuthenticationResult authResult : authContext.getUntaggedAuthenticationResults() ) {
                lastStatus = checkUser(authResult);
                if (lastStatus.equals(AssertionStatus.NONE)) {
                    // successful output point
                    return lastStatus;
                }
            }
        } else {
            AuthenticationResult authResult = authContext.getAuthenticationResultForTag( assertion.getIdentityTag() );
            if ( authResult != null ) {
                lastStatus = checkUser(authResult);
                if (lastStatus.equals(AssertionStatus.NONE)) {
                    // successful output point
                    return lastStatus;
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
                if (lastStatus.equals(AssertionStatus.NONE)) {
                    // successful output point
                    return lastStatus;
                }
            } catch (InvalidClientCertificateException icce) {
                auditor.logAndAudit(AssertionMessages.IDENTITY_INVALID_CERT, pc.getLogin());
                if ( isRequest() ) {
                    // set some response header so that the CP is made aware of this situation
                    HttpResponseKnob httpResponseKnob = context.getResponse().getKnob(HttpResponseKnob.class);
                    if(httpResponseKnob != null) {
                        httpResponseKnob.addHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS,
                                                   SecureSpanConstants.CERT_INVALID);
                    }
                }
                lastStatus = authFailed(pc, icce);
            } catch (MissingCredentialsException mce) {
                if ( isRequest() )
                    context.setAuthenticationMissing();
                lastStatus = authFailed(pc, mce);
            } catch (AuthenticationException ae) {
                lastStatus = authFailed(pc, ae);
            }
        }
        auditor.logAndAudit(AssertionMessages.IDENTITY_AUTHENTICATION_FAILED, assertion.loggingIdentity());
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
        if (authResult == null) return authFailed(pc, null);

        if ( authResult.isCertSignedByStaleCA() && isRequest() ) {
            HttpResponseKnob hrk = context.getResponse().getKnob(HttpResponseKnob.class);
            hrk.setHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS, SecureSpanConstants.CERT_STALE);
        }

        User user = authResult.getUser();

        String name = user.getLogin();
        if (name == null) name = user.getName();
        if (name == null) name = user.getSubjectDn();
        if (name == null) name = user.getId();

        // Authentication success
        authContext.addAuthenticationResult(authResult, assertion.getIdentityTag());
        auditor.logAndAudit(AssertionMessages.IDENTITY_AUTHENTICATED, name);

        // Make sure this guy matches our criteria
        return checkUser(authResult);
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
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
     * <code>identityProviderOid</code> property, using a cache if possible.
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
