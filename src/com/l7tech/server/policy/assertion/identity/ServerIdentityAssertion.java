/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.logging.LogManager;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.protocol.SecureSpanConstants;

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
     * Attempts to authenticate the request using the <code>PrincipalCredentials</code>
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
        PrincipalCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) {
            // No credentials have been found yet
            if ( request.isAuthenticated() ) {
                String err = "Request is authenticated but request has no PrincipalCredentials!";
                _log.log(Level.SEVERE, err);
                throw new IllegalStateException( err );
            } else {
                // Authentication is required for any IdentityAssertion
                response.addResult( new AssertionResult( _data, request, AssertionStatus.AUTH_REQUIRED ) );
                // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
                response.setAuthenticationMissing( true );
                return AssertionStatus.AUTH_REQUIRED;
            }
        } else {
            // A CredentialFinder has already run.
            User user = pc.getUser();

           if ( request.isAuthenticated() ) {
                // The user was authenticated by a previous IdentityAssertion.
                _log.log(Level.FINEST, "Request already authenticated");
                return checkUser( user );
            } else {
                if ( _data.getIdentityProviderOid() == Entity.DEFAULT_OID ) {
                    String err = "Can't call checkRequest() when no valid identityProviderOid has been set!";
                    _log.log(Level.SEVERE, err);
                    throw new IllegalStateException( err );
                }

                try {
                    IdentityProvider provider = getIdentityProvider();
                    provider.authenticate( pc );

                    // Authentication succeeded
                    request.setAuthenticated(true);
                    _log.log(Level.FINEST, "Authenticated " + user.getLogin() );
                    // Make sure this guy matches our criteria
                    return checkUser( user );
                } catch ( BadCredentialsException bce ) {
                    // Authentication failure
                    response.addResult( new AssertionResult( _data, request, AssertionStatus.AUTH_FAILED, bce.getMessage(), bce ));
                    _log.log(Level.INFO, "Authentication failed for " + user.getLogin() );
                    return AssertionStatus.AUTH_FAILED;
                } catch ( InvalidClientCertificateException icce ) {
                    response.addResult( new AssertionResult( _data, request, AssertionStatus.AUTH_FAILED, icce.getMessage(), icce ));
                    _log.log(Level.INFO, "Invalid client cert for " + user.getLogin() );
                    // set some response header so that the CP is made aware of this situation
                    response.setParameter(Response.PARAM_HTTP_CERT_STATUS, "invalid");
                    return AssertionStatus.AUTH_FAILED;
                } catch ( MissingCredentialsException mce ) {
                    response.setAuthenticationMissing(true);
                    response.addResult( new AssertionResult( _data, request, AssertionStatus.AUTH_REQUIRED, mce.getMessage(), mce ));
                    _log.log(Level.INFO, "Authentication failed for " + user.getLogin() );
                    return AssertionStatus.AUTH_REQUIRED;
                } catch ( AuthenticationException ae ) {
                    _log.log(Level.INFO, "Authentication failed for " + user.getLogin() );
                    response.addResult( new AssertionResult( _data, request, AssertionStatus.AUTH_FAILED, ae.getMessage(), ae ));
                    return AssertionStatus.AUTH_FAILED;
                } catch ( FindException fe ) {
                    String err = "Couldn't find identity provider!";
                    _log.log( Level.SEVERE, err, fe );
                    throw new IdentityAssertionException( err, fe );
                }

            }
        }
    }

    /**
     * Loads the <code>IdentityProvider</code> object corresponding to the
     * <code>identityProviderOid</code> property, using a cache if possible.
     * @return
     * @throws FindException
     */
    protected IdentityProvider getIdentityProvider() throws FindException {
        if ( _identityProvider == null ) {
            if ( _configManager == null ) _configManager = new IdProvConfManagerServer();
            IdentityProviderConfig config = _configManager.findByPrimaryKey( _data.getIdentityProviderOid() );
            _identityProvider = IdentityProviderFactory.makeProvider( config );
        }
        return _identityProvider;
    }

    protected abstract AssertionStatus checkUser( User u );

    protected transient IdentityProviderConfigManager _configManager = null;
    protected transient IdentityProvider _identityProvider;
    protected transient Logger _log = LogManager.getInstance().getSystemLogger();

    protected IdentityAssertion _data;
}
