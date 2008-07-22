/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
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
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Subclasses of ServerIdentityAssertion are responsible for verifying that the entity
 * making a <code>Request</code> (as previously found using a CredentialSourceAssertion)
 * is authorized to do so.
 *
 * @author alex
 */
public abstract class ServerIdentityAssertion extends AbstractServerAssertion<IdentityAssertion> {
    private final Logger logger = Logger.getLogger(ServerIdentityAssertion.class.getName());

    protected final Auditor auditor;
    private final IdentityProviderFactory identityProviderFactory;
    protected IdentityAssertion identityAssertion;

    public ServerIdentityAssertion(IdentityAssertion data, ApplicationContext ctx) {
        super(data);
        identityAssertion = data;
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
     *
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) {
        List<LoginCredentials> pCredentials = context.getCredentials();

        if (pCredentials.size() < 1 && context.getLastAuthenticatedUser() == null) {
            // No credentials have been found yet
            if (context.isAuthenticated()) {
                auditor.logAndAudit(AssertionMessages.AUTHENTICATED_BUT_CREDENTIALS_NOT_FOUND);
                throw new IllegalStateException("Request is authenticated but request has no LoginCredentials!");
            }

            // Authentication is required for any IdentityAssertion
            // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
            context.setAuthenticationMissing();
            auditor.logAndAudit(AssertionMessages.CREDENTIALS_NOT_FOUND);
            return AssertionStatus.AUTH_REQUIRED;
        }

        if (identityAssertion.getIdentityProviderOid() == IdentityProviderConfig.DEFAULT_OID) {
            auditor.logAndAudit(AssertionMessages.ID_PROVIDER_ID_NOT_SET);
            throw new IllegalStateException("Can't call checkRequest() when no valid identityProviderOid has been set!");
        }

        AssertionStatus lastStatus = AssertionStatus.UNDEFINED;
        final IdentityProvider provider;
        try {
            provider = getIdentityProvider();
        } catch (FindException e) {
            auditor.logAndAudit(AssertionMessages.ID_PROVIDER_NOT_FOUND, new String[0], e);
            // fla fix, allow the policy to continue in case the credentials be valid for
            // another id assertion down the road (fix for bug 374)
            return AssertionStatus.AUTH_FAILED;
        }

        for (LoginCredentials pc : pCredentials) {
            try {
                lastStatus = validateCredentials(provider, pc, context);
                if (lastStatus.equals(AssertionStatus.NONE)) {
                    // successful output point
                    return lastStatus;
                }
            } catch (InvalidClientCertificateException icce) {
                auditor.logAndAudit(AssertionMessages.INVALID_CERT, pc.getLogin());
                // set some response header so that the CP is made aware of this situation
                context.getResponse().getHttpResponseKnob().addHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS,
                                                                      SecureSpanConstants.CERT_INVALID);
                lastStatus = authFailed(pc, icce);
            } catch (MissingCredentialsException mce) {
                context.setAuthenticationMissing();
                lastStatus = authFailed(pc, mce);
            } catch (AuthenticationException ae) {
                lastStatus = authFailed(pc, ae);
            }
        }
        auditor.logAndAudit(AssertionMessages.AUTHENTICATION_FAILED, identityAssertion.loggingIdentity());
        return lastStatus;
    }

    /**
     * Authenticates and calls {@link #checkUser}.  Override at will.
     */
    protected AssertionStatus validateCredentials(IdentityProvider provider,
                                                  LoginCredentials pc,
                                                  PolicyEnforcementContext context)
        throws AuthenticationException {
        AuthenticationResult authResult = AuthCache.getInstance().getCachedAuthResult(
            pc,
            provider,
            context.getAuthSuccessCacheTime(),
            context.getAuthFailureCacheTime()
        );
        if (authResult == null) return authFailed(pc, null);

        if (authResult.isCertSignedByStaleCA()) {
            HttpResponseKnob hrk = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
            hrk.setHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS, SecureSpanConstants.CERT_STALE);
        }

        User user = authResult.getUser();

        String name = user.getLogin();
        if (name == null) name = user.getName();
        if (name == null) name = user.getSubjectDn();
        if (name == null) name = user.getId();

        // Authentication success
        context.addAuthenticationResult(authResult);
        auditor.logAndAudit(AssertionMessages.AUTHENTICATED, name);

        // Make sure this guy matches our criteria
        return checkUser(authResult, context);
    }

    private AssertionStatus authFailed(LoginCredentials pc, Exception e) {
        // we were losing the details of this authentication failure. important for debugging saml stuff
        logger.log(Level.FINE, "ServerIdentityAssertion failed", e);
        String name = pc.getLogin();
        if (name == null || name.length() == 0) {
            X509Certificate cert = pc.getClientCert();
            if (cert != null) name = cert.getSubjectDN().getName();
        }

        String logid = identityAssertion.loggingIdentity();

        // Preserve old logging behavior until there's a compelling reason to change it
        if (identityAssertion instanceof MemberOfGroup)
            logger.info("could not verify membership of group " + logid + " with credentials from " + name);
        else if (identityAssertion instanceof SpecificUser)
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
     * @throws FindException
     */
    protected IdentityProvider getIdentityProvider() throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(identityAssertion.getIdentityProviderOid());
        if (provider == null) {
            auditor.logAndAudit(AssertionMessages.ID_PROVIDER_NOT_EXIST);
            throw new FindException("id assertion refers to an id provider which does not exist anymore");
        } else {
            return provider;
        }
    }

    /**
     * Implement to decide whether the authenticated user is acceptable.
     */
    protected abstract AssertionStatus checkUser(AuthenticationResult authResult, PolicyEnforcementContext context);

}
