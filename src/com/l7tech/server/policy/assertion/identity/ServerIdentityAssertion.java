/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;

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

    public ServerIdentityAssertion(IdentityAssertion data, ApplicationContext ctx) {
        _data = data;
        if (ctx == null) {
            throw new IllegalArgumentException("Application Conext is required");
        }
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
                String err = "Request is authenticated but request has no LoginCredentials!";
                logger.log(Level.SEVERE, err);
                throw new IllegalStateException(err);
            } else {
                // Authentication is required for any IdentityAssertion
                context.addResult(new AssertionResult(_data, AssertionStatus.AUTH_REQUIRED));
                // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
                context.setAuthenticationMissing(true);
                logger.fine("No credentials found");
                return AssertionStatus.AUTH_REQUIRED;
            }
        } else {
            // A CredentialFinder has already run.

            if (context.isAuthenticated()) {
                User user = context.getAuthenticatedUser();
                // The user was authenticated by a previous IdentityAssertion.
                logger.log(Level.FINEST, "Request already authenticated");
                return checkUser(user, context);
            } else {
                if (_data.getIdentityProviderOid() == Entity.DEFAULT_OID) {
                    String err = "Can't call checkRequest() when no valid identityProviderOid has been set!";
                    logger.log(Level.SEVERE, err);
                    throw new IllegalStateException(err);
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
                    logger.log(Level.FINEST, "Authenticated " + name);
                    // Make sure this guy matches our criteria
                    return checkUser(user, context);
                } catch (InvalidClientCertificateException icce) {
                    logger.info("Invalid client cert for " + name);
                    // set some response header so that the CP is made aware of this situation
                    context.getResponse().getHttpResponseKnob().addHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS,
                      SecureSpanConstants.INVALID);
                    return authFailed(context, pc, icce);
                } catch (MissingCredentialsException mce) {
                    context.setAuthenticationMissing(true);
                    return authFailed(context, pc, mce);
                } catch (AuthenticationException ae) {
                    return authFailed(context, pc, ae);
                } catch (FindException fe) {
                    String err = "Couldn't find identity provider!";
                    logger.log(Level.SEVERE, err, fe);
                    // fla fix, allow the policy to continue in case the credentials be valid for
                    // another id assertion down the road (fix for bug 374)
                    // throw new IdentityAssertionException( err, fe );
                    return AssertionStatus.AUTH_FAILED;
                } catch (IOException e) {
                    logger.info("Authentication failed for " + pc.getLogin());
                    return AssertionStatus.AUTH_FAILED;
                }
            }
        }
    }

    private AssertionStatus authFailed(PolicyEnforcementContext context, LoginCredentials pc, Exception e) {
        context.addResult(new AssertionResult(_data, AssertionStatus.AUTH_FAILED, e == null ? "" : e.getMessage(), e));
        StringBuffer message = new StringBuffer();
        message.append("Authentication failed for ");
        String name = pc.getLogin();
        if (name == null || name.length() == 0) {
            X509Certificate cert = pc.getClientCert();
            if (cert != null) name = cert.getSubjectDN().getName();
        }
        message.append(name);
        if (e != null) {
            message.append(": ");
            message.append(e.getMessage());
        }
        // fla note: this is debug information since ServerSpecificUser.checkUser is already logging failure or success
        logger.finest(message.toString());
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
            String msg = "id assertion refers to an id provider which does not exist anymore";
            logger.warning(msg);
            throw new FindException(msg);
        } else {
            return provider;
        }
    }

    protected abstract AssertionStatus checkUser(User u, PolicyEnforcementContext context);

    protected final transient Logger logger = Logger.getLogger(getClass().getName());

    protected IdentityAssertion _data;
}
