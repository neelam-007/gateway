/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class IdentityAssertion extends Assertion {
    protected IdentityAssertion() {
        super();
    }

    protected IdentityAssertion( long oid ) {
        _identityProviderOid = oid;
    }

    public AssertionStatus checkRequest( Request request, Response response ) throws IdentityAssertionException {
        PrincipalCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) {
            // No credentials have been found yet
            if ( request.isAuthenticated() ) {
                String err = "Request is authenticated but request has no PrincipalCredentials!";
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
                throw new IllegalStateException( err );
            } else {
                // Authentication is required for any IdentityAssertion
                response.addResult( new AssertionResult( this, request, AssertionStatus.AUTH_REQUIRED ) );
                // TODO: Some future IdentityAssertion might succeed, but this flag will remain true!
                response.setAuthenticationMissing( true );
                return AssertionStatus.AUTH_REQUIRED;
            }
        } else {
            // A CredentialFinder has already run.
            User user = pc.getUser();

            AssertionStatus status;
            if ( request.isAuthenticated() ) {
                // The user was authenticated by a previous IdentityAssertion.
                LogManager.getInstance().getSystemLogger().log(Level.FINEST, "Request already authenticated");
                status = doCheckUser( user );
            } else {
                if ( _identityProviderOid == Entity.DEFAULT_OID ) {
                    String err = "Can't call checkRequest() when no valid identityProviderOid has been set!";
                    LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
                    throw new IllegalStateException( err );
                }

                try {
                    if ( getIdentityProvider().authenticate( pc ) ) {
                        // Authentication succeeded
                        request.setAuthenticated(true);
                        LogManager.getInstance().getSystemLogger().log(Level.FINER, "Authenticated " + user.getLogin() );
                        // Make sure this guy matches our criteria
                        status = doCheckUser( user );
                    } else {
                        // Authentication failure
                        status = AssertionStatus.AUTH_FAILED;
                        LogManager.getInstance().getSystemLogger().log(Level.FINER, "Authentication failed for " + user.getLogin() );
                    }
                } catch ( FindException fe ) {
                    String err = "Couldn't find identity provider!";
                    LogManager.getInstance().getSystemLogger().log( Level.SEVERE, err, fe );
                    throw new IdentityAssertionException( err, fe );
                }

            }

            if ( status == AssertionStatus.AUTH_FAILED ) response.setAuthenticationMissing( true );

            return status;
        }
    }

    /** No identity providers on client side. */
    public AssertionStatus decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionStatus.NOT_APPLICABLE;
    }

    public void setIdentityProviderOid( long provider ) {
        if ( _identityProviderOid != provider ) _identityProvider = null;
        _identityProviderOid = provider;
    }

    protected IdentityProvider getIdentityProvider() throws FindException {
        if ( _identityProvider == null ) {
            IdentityProviderConfigManager configManager = new IdProvConfManagerServer();
            IdentityProviderConfig config = configManager.findByPrimaryKey( _identityProviderOid );
            _identityProvider = IdentityProviderFactory.makeProvider( config );
        }
        return _identityProvider;
    }

    public long getIdentityProviderOid() {
        return _identityProviderOid;
    }

    protected abstract AssertionStatus doCheckUser( User u );
    protected long _identityProviderOid = Entity.DEFAULT_OID;
    protected transient IdentityProvider _identityProvider;
}
