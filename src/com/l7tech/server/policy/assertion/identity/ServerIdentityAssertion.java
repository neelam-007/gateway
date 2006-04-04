/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.AuthCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Subclasses of ServerIdentityAssertion are responsible for verifying that the entity
 * making a <code>Request</code> (as previously found using a CredentialSourceAssertion)
 * is authorized to do so.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class ServerIdentityAssertion implements ServerAssertion {
    private final Logger logger = Logger.getLogger(ServerIdentityAssertion.class.getName());

    private final Auditor auditor;
    private final ApplicationContext applicationContext;
    protected IdentityAssertion identityAssertion;

    public ServerIdentityAssertion(IdentityAssertion data, ApplicationContext ctx) {
        identityAssertion = data;
        if (ctx == null) {
            throw new IllegalArgumentException("Application Context is required");
        }
        this.auditor = new Auditor(this, ctx, Logger.getLogger(getClass().getName()));
        this.applicationContext = ctx;
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
        LoginCredentials pc = context.getCredentials();

        if (pc == null && context.getAuthenticatedUser() == null) {
            // No credentials have been found yet
            if (context.isAuthenticated()) {
                auditor.logAndAudit(AssertionMessages.AUTHENTICATED_BUT_CREDENTIALS_NOT_FOUND);
                throw new IllegalStateException("Request is authenticated but request has no LoginCredentials!");
            }

            // Authentication is required for any IdentityAssertion
            context.addResult(new AssertionResult(identityAssertion, AssertionStatus.AUTH_REQUIRED));
            // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
            context.setAuthenticationMissing();
            auditor.logAndAudit(AssertionMessages.CREDENTIALS_NOT_FOUND);
            return AssertionStatus.AUTH_REQUIRED;
        }

        if (context.isAuthenticated()) {
            AuthenticationResult authResult = context.getAuthenticationResult();
            // The user was authenticated by a previous IdentityAssertion.
            auditor.logAndAudit(AssertionMessages.ALREADY_AUTHENTICATED);
            return checkUser(authResult, context);
        }

        if (identityAssertion.getIdentityProviderOid() == Entity.DEFAULT_OID) {
            auditor.logAndAudit(AssertionMessages.ID_PROVIDER_ID_NOT_SET);
            throw new IllegalStateException("Can't call checkRequest() when no valid identityProviderOid has been set!");
        }

        String name;
        try {
            IdentityProvider provider = getIdentityProvider(context);
            AuthenticationResult authResult = AuthCache.getInstance().getCachedAuthResult(
                pc,
                provider,
                context.getAuthSuccessCacheTime(),
                context.getAuthFailureCacheTime()
            );
            if (authResult == null) return authFailed(context, pc, null);

            if (authResult.isCertSignedByStaleCA()) {
                HttpResponseKnob hrk = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
                hrk.setHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS, SecureSpanConstants.CERT_STALE);
            }

            User user = authResult.getUser();

            name = user.getLogin();
            if (name == null) name = user.getName();
            if (name == null) name = user.getSubjectDn();
            if (name == null) name = user.getUniqueIdentifier();

            // Authentication succeeded
            context.setAuthenticationResult(authResult);
            auditor.logAndAudit(AssertionMessages.AUTHENTICATED, new String[] {name});

            // Make sure this guy matches our criteria
            return checkUser(authResult, context);
        } catch (InvalidClientCertificateException icce) {
            auditor.logAndAudit(AssertionMessages.INVALID_CERT, new String[] {pc.getLogin()});
            // set some response header so that the CP is made aware of this situation
            context.getResponse().getHttpResponseKnob().addHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS,
                                                                  SecureSpanConstants.CERT_INVALID);
            return authFailed(context, pc, icce);
        } catch (MissingCredentialsException mce) {
            context.setAuthenticationMissing();
            return authFailed(context, pc, mce);
        } catch (AuthenticationException ae) {
            return authFailed(context, pc, ae);
        } catch (FindException fe) {
            auditor.logAndAudit(AssertionMessages.ID_PROVIDER_NOT_FOUND, new String[0], fe);
            // fla fix, allow the policy to continue in case the credentials be valid for
            // another id assertion down the road (fix for bug 374)
            // throw new IdentityAssertionException( err, fe );
            return AssertionStatus.AUTH_FAILED;
        }
    }

    /**
     * Authenticates and calls {@link #checkUser}.  Override at will.
     */
    protected AssertionStatus validateCredentials(IdentityProvider provider,
                                                  LoginCredentials pc,
                                                  PolicyEnforcementContext context)
            throws AuthenticationException, FindException, IOException
    {
        AuthenticationResult authResult = provider.authenticate(pc);
        if (authResult == null) return authFailed(context, pc, null);

        if (authResult.isCertSignedByStaleCA()) {
            HttpResponseKnob hrk = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
            hrk.setHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS, SecureSpanConstants.CERT_STALE);
        }

        User user = authResult.getUser();

        String name = user.getLogin();
        if (name == null) name = user.getName();
        if (name == null) name = user.getSubjectDn();
        if (name == null) name = user.getUniqueIdentifier();

        // Authentication succeeded
        context.setAuthenticationResult(authResult);
        auditor.logAndAudit(AssertionMessages.AUTHENTICATED, new String[] {name});

        // Make sure this guy matches our criteria
        return checkUser(authResult, context);
    }

    private AssertionStatus authFailed(PolicyEnforcementContext context, LoginCredentials pc, Exception e) {
        // we were losing the details of this authentication failure. important for debugging saml stuff
        logger.log(Level.FINE, "ServerIdentityAssertion failed", e);
        context.addResult(new AssertionResult(identityAssertion, AssertionStatus.AUTH_FAILED, e == null ? "" : e.getMessage(), e));
        String name = pc.getLogin();
        if (name == null || name.length() == 0) {
            X509Certificate cert = pc.getClientCert();
            if (cert != null) name = cert.getSubjectDN().getName();
        }

        String identityToAssert = null;
        if (identityAssertion instanceof SpecificUser) {
            SpecificUser su = (SpecificUser)identityAssertion;
            String idtomatch = su.getUserLogin();
            if (idtomatch == null) {
                idtomatch = su.getUserName();
            }
            identityToAssert = idtomatch;
            logger.info("could not verify identity " + idtomatch + " with credentials from " + name);
        } else if (identityAssertion instanceof MemberOfGroup) {
            MemberOfGroup mog = (MemberOfGroup)identityAssertion;
            String groupname = mog.getGroupName();
            identityToAssert = groupname;
            logger.info("cound not verify membership of group " + groupname + " with credentials from " + name);
        }

        auditor.logAndAudit(AssertionMessages.AUTHENTICATION_FAILED, new String[] {identityToAssert});
        return AssertionStatus.AUTH_FAILED;
    }

    /**
     * Loads the {@link IdentityProvider} object corresponding to the
     * <code>identityProviderOid</code> property, using a cache if possible.
     *
     * @param context
     * @return
     * @throws FindException
     */
    protected IdentityProvider getIdentityProvider(PolicyEnforcementContext context) throws FindException {
        IdentityProviderFactory ipf = (IdentityProviderFactory)applicationContext.getBean("identityProviderFactory");
        IdentityProvider provider = ipf.getProvider(identityAssertion.getIdentityProviderOid());
        if (provider == null) {
            auditor.logAndAudit(AssertionMessages.ID_PROVIDER_NOT_EXIST);
            throw new FindException("id assertion refers to an id provider which does not exist anymore");
        } else {
            return provider;
        }
    }

    /**
     * Override to decide whether the authenticated user is acceptable.
     */
    protected AssertionStatus checkUser(AuthenticationResult authResult, PolicyEnforcementContext context) {
        return AssertionStatus.NONE;
    }

}
