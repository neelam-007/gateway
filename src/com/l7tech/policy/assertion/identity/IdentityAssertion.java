/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.proxy.datamodel.PendingRequest;

import java.security.Principal;

/**
 * @author alex
 */
public abstract class IdentityAssertion extends Assertion {
    protected IdentityAssertion( IdentityProvider provider ) {
        _identityProvider = provider;
    }

    protected IdentityAssertion() {
        super();
    }

    public AssertionStatus checkRequest(Request request, Response response) throws PolicyAssertionException {
        return doCheckRequest( request, response );
    }

    public AssertionStatus doCheckRequest( Request request, Response response ) throws IdentityAssertionException {
        PrincipalCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) {
            // No credentials have been found yet
            if ( request.isAuthenticated() )
                throw new IllegalStateException( "Request is authenticated but request has no PrincipalCredentials!" );
            else {
                response.addResult( new AssertionResult( this, request, AssertionStatus.AUTH_REQUIRED ) );
                response.setAuthenticationMissing( true );
                return AssertionStatus.AUTH_REQUIRED;
            }
        } else {
            Principal principal = pc.getPrincipal();
            byte[] credentials = pc.getCredentials();

            if ( request.isAuthenticated() ) {
                return doCheckPrincipal( principal );
            } else {
                if ( _identityProvider.authenticate( principal, credentials ) ) {
                    request.setAuthenticated(true);
                    return doCheckPrincipal( principal );
                } else {
                    response.setAuthenticationMissing( true );
                    return AssertionStatus.AUTH_FAILED;
                }
            }
        }
    }

    /** No identity providers on client side. */
    public AssertionStatus decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionStatus.NOT_APPLICABLE;
    }

    public void setIdentityProvider( IdentityProvider provider ) {
        _identityProvider = provider;
    }

    public IdentityProvider getIdentityProvider() {
        return _identityProvider;
    }

    protected abstract AssertionStatus doCheckPrincipal( Principal p );

    protected IdentityProvider _identityProvider;
}
