/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential;

import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerCredentialSourceAssertion implements ServerAssertion {
    public ServerCredentialSourceAssertion( CredentialSourceAssertion data ) {
        _data = data;
    }

    public AssertionStatus checkRequest( Request request, Response response ) throws IOException, PolicyAssertionException {
        try {
            PrincipalCredentials pc = request.getPrincipalCredentials();
            if ( pc == null ) {
                // No finder has been run yet!
                pc = findCredentials( request );
            }

            if ( pc == null ) {
                response.setAuthenticationMissing( true );
                _log.log(Level.INFO, AssertionStatus.AUTH_REQUIRED.getMessage());
                return AssertionStatus.AUTH_REQUIRED;
            } else {
                request.setPrincipalCredentials( pc );
                return checkCredentials( request, response );
            }
        } catch ( CredentialFinderException cfe ) {
            AssertionStatus status = cfe.getStatus();
            if ( status == null ) {
                _log.log(Level.SEVERE, cfe.getMessage(), cfe);
                throw new PolicyAssertionException( cfe.getMessage(), cfe );
            } else {
                response.addResult( new AssertionResult( _data, request, status, cfe.getMessage() ) );
                _log.log(Level.INFO, cfe.getMessage(), cfe);
                if ( status == AssertionStatus.AUTH_REQUIRED )
                    response.setAuthenticationMissing(true);
                else
                    response.setPolicyViolated(true);
                return status;
            }
        }
    }

    protected abstract PrincipalCredentials findCredentials( Request request ) throws IOException, CredentialFinderException;

    protected abstract AssertionStatus checkCredentials( Request request, Response response ) throws CredentialFinderException;

    protected Logger _log = LogManager.getInstance().getSystemLogger();
    protected transient Map _credentialFinders = new HashMap();

    protected CredentialSourceAssertion _data;
}
