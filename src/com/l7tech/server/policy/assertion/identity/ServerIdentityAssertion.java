/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.protocol.SecureSpanConstants;
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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Subclasses of ServerIdentityAssertion are responsible for verifying that the entity
 * making a <code>Request</code> (as previously found using a CredentialSourceAssertion)
 * is authorized to do so.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class ServerIdentityAssertion implements ServerAssertion {
    private final ApplicationContext applicationContext;
    private final Auditor auditor;
    private final Logger logger = Logger.getLogger(ServerIdentityAssertion.class.getName());

    public ServerIdentityAssertion(IdentityAssertion data, ApplicationContext ctx) {
        _data = data;
        if (ctx == null) {
            throw new IllegalArgumentException("Application Conext is required");
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
     * @return
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) {
        LoginCredentials pc = context.getCredentials();
        if (pc == null && context.getAuthenticatedUser() == null) {
            // No credentials have been found yet
            if (context.isAuthenticated()) {
                auditor.logAndAudit(AssertionMessages.AUTHENTICATED_BUT_CREDENTIALS_NOT_FOUND);
                throw new IllegalStateException("Request is authenticated but request has no LoginCredentials!");
            } else {
                // Authentication is required for any IdentityAssertion
                context.addResult(new AssertionResult(_data, AssertionStatus.AUTH_REQUIRED));
                // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
                context.setAuthenticationMissing();
                auditor.logAndAudit(AssertionMessages.CREDENTIALS_NOT_FOUND);
                return AssertionStatus.AUTH_REQUIRED;
            }
        } else {
            // A CredentialFinder has already run.

            if (context.isAuthenticated()) {
                User user = context.getAuthenticatedUser();
                // The user was authenticated by a previous IdentityAssertion.
                auditor.logAndAudit(AssertionMessages.ALREADY_AUTHENTICATED);
                return checkUser(user, context);
            } else {
                if (_data.getIdentityProviderOid() == Entity.DEFAULT_OID) {
                    auditor.logAndAudit(AssertionMessages.ID_PROVIDER_ID_NOT_SET);
                    throw new IllegalStateException("Can't call checkRequest() when no valid identityProviderOid has been set!");
                }

                String name = null;
                try {
                    IdentityProvider provider = getIdentityProvider(context);
                    User user = provider.authenticate(pc);
                    if (user == null) return authFailed(context, pc, null);

                    name = user.getLogin();
                    if (name == null) name = user.getName();
                    if (name == null) name = user.getSubjectDn();
                    if (name == null) name = user.getUniqueIdentifier();

                    // Authentication succeeded
                    context.setAuthenticated(true);
                    context.setAuthenticatedUser(user);
                    auditor.logAndAudit(AssertionMessages.AUTHENTICATED, new String[] {name});

                    // Make sure this guy matches our criteria
                    return checkUser(user, context);
                } catch (InvalidClientCertificateException icce) {
                    auditor.logAndAudit(AssertionMessages.INVALID_CERT, new String[] {name});
                    // set some response header so that the CP is made aware of this situation
                    context.getResponse().getHttpResponseKnob().addHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS,
                      SecureSpanConstants.INVALID);
                    return authFailed(context, pc, icce);
                } catch (MissingCredentialsException mce) {
                    context.setAuthenticationMissing();
                    return authFailed(context, pc, mce);
                } catch (AuthenticationException ae) {
                    return authFailed(context, pc, ae);
                } catch (FindException fe) {
                    auditor.logAndAudit(AssertionMessages.ID_PROVIDER_NOT_FOUND, new String[] {name}, fe);
                    // fla fix, allow the policy to continue in case the credentials be valid for
                    // another id assertion down the road (fix for bug 374)
                    // throw new IdentityAssertionException( err, fe );
                    return AssertionStatus.AUTH_FAILED;
                } catch (IOException e) {
                    auditor.logAndAudit(AssertionMessages.AUTHENTICATION_FAILED, new String[] {pc.getLogin()}, e);
                    return AssertionStatus.AUTH_FAILED;
                }
            }
        }
    }

    private AssertionStatus authFailed(PolicyEnforcementContext context, LoginCredentials pc, Exception e) {
        context.addResult(new AssertionResult(_data, AssertionStatus.AUTH_FAILED, e == null ? "" : e.getMessage(), e));
        String name = pc.getLogin();
        if (name == null || name.length() == 0) {
            X509Certificate cert = pc.getClientCert();
            if (cert != null) name = cert.getSubjectDN().getName();
        }

        String identityToAssert = null;
        if (_data instanceof SpecificUser) {
            SpecificUser su = (SpecificUser)_data;
            String idtomatch = su.getUserLogin();
            if (idtomatch == null) {
                idtomatch = su.getUserName();
            }
            identityToAssert = idtomatch;
            logger.info("could not verify identity " + idtomatch + " with credentials from " + name);
        } else if (_data instanceof MemberOfGroup) {
            MemberOfGroup mog = (MemberOfGroup)_data;
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
        IdentityProvider provider = ipf.getProvider(_data.getIdentityProviderOid());
        if (provider == null) {
            auditor.logAndAudit(AssertionMessages.ID_PROVIDER_NOT_EXIST);
            throw new FindException("id assertion refers to an id provider which does not exist anymore");
        } else {
            return provider;
        }
    }

    protected abstract AssertionStatus checkUser(User u, PolicyEnforcementContext context);
    protected IdentityAssertion _data;
}
