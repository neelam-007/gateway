/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.*;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertionException;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.util.logging.Level;
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
    public ServerIdentityAssertion( IdentityAssertion data ) {
        _data = data;
    }

    /**
     * Attempts to authenticate the request using the <code>LoginCredentials</code>
     * previously found with a <code>ServerCredentialSourceAssertion</code>, and if
     * successful calls checkUser() to verify that the authenticated user is
     * authorized to make the request.
     *
     * @param request
     * @param response
     * @return
     * @throws IdentityAssertionException
     */
    public AssertionStatus checkRequest( Request request, Response response ) throws IdentityAssertionException {
        LoginCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) {
            // No credentials have been found yet
            if (request.isAuthenticated()) {
                String err = "Request is authenticated but request has no LoginCredentials!";
                logger.log(Level.SEVERE, err);
                throw new IllegalStateException( err );
            } else {
                // Authentication is required for any IdentityAssertion
                response.addResult(new AssertionResult(_data, AssertionStatus.AUTH_REQUIRED));
                // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
                response.setAuthenticationMissing(true);
                logger.fine("No credentials found");
                return AssertionStatus.AUTH_REQUIRED;
            }
        } else {
            // A CredentialFinder has already run.

           if ( request.isAuthenticated() ) {
               User user = request.getUser();
                // The user was authenticated by a previous IdentityAssertion.
                logger.log(Level.FINEST, "Request already authenticated");
                return checkUser( user );
            } else {
                if ( _data.getIdentityProviderOid() == Entity.DEFAULT_OID ) {
                    String err = "Can't call checkRequest() when no valid identityProviderOid has been set!";
                    logger.log(Level.SEVERE, err);
                    throw new IllegalStateException( err );
                }

                try {
                    IdentityProvider provider = getIdentityProvider();
                    User user = provider.authenticate( pc );

                    // Authentication succeeded
                    request.setAuthenticated(true);
                    request.setUser( user );
                    logger.log(Level.FINEST, "Authenticated " + user.getLogin() );
                    // Make sure this guy matches our criteria
                    return checkUser( user );
                } catch ( BadCredentialsException bce ) {
                    // Authentication failure
                    response.addResult( new AssertionResult( _data, AssertionStatus.AUTH_FAILED, bce.getMessage(), bce ));
                    logger.info("Authentication failed for " + pc.getLogin() );
                    return AssertionStatus.AUTH_FAILED;
                } catch ( InvalidClientCertificateException icce ) {
                    response.addResult( new AssertionResult( _data, AssertionStatus.AUTH_FAILED, icce.getMessage(), icce ));
                    logger.info("Invalid client cert for " + pc.getLogin() );
                    // set some response header so that the CP is made aware of this situation
                    response.setParameter(Response.PARAM_HTTP_CERT_STATUS, SecureSpanConstants.INVALID);
                    return AssertionStatus.AUTH_FAILED;
                } catch ( MissingCredentialsException mce ) {
                    response.setAuthenticationMissing(true);
                    response.addResult( new AssertionResult( _data, AssertionStatus.AUTH_REQUIRED, mce.getMessage(), mce ));
                    logger.info("Authentication failed for " + pc.getLogin() );
                    return AssertionStatus.AUTH_REQUIRED;
                } catch ( AuthenticationException ae ) {
                    logger.info("Authentication failed for " + pc.getLogin() );
                    response.addResult( new AssertionResult( _data, AssertionStatus.AUTH_FAILED, ae.getMessage(), ae ));
                    return AssertionStatus.AUTH_FAILED;
                } catch ( FindException fe ) {
                    String err = "Couldn't find identity provider!";
                    logger.log(Level.SEVERE, err, fe);
                    // fla fix, allow the policy to continue in case the credentials be valid for
                    // another id assertion down the road (fix for bug 374)
                    // throw new IdentityAssertionException( err, fe );
                    return AssertionStatus.AUTH_FAILED;
                }
            }
        }
    }

    /**
     * Loads the {@link IdentityProvider} object corresponding to the
     * <code>identityProviderOid</code> property, using a cache if possible.
     * @return
     * @throws FindException
     */
    protected IdentityProvider getIdentityProvider() throws FindException {
        IdentityProvider provider = IdentityProviderFactory.getProvider(_data.getIdentityProviderOid());
        if (provider == null) {
            String msg = "id assertion refers to an id provider which does not exist anymore";
            logger.warning(msg);
            throw new FindException(msg);
        } else {
            return provider;
        }
    }

    protected abstract AssertionStatus checkUser( User u );

    protected final transient Logger logger = Logger.getLogger(getClass().getName());

    protected IdentityAssertion _data;
}
