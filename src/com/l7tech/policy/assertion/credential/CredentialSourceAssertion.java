/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

import com.l7tech.credential.*;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.Locator;
import org.apache.log4j.Category;

/**
 * Asserts that the requester's credentials were found, and using a particular authentication mechanism.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class CredentialSourceAssertion extends Assertion {
    public CredentialSourceAssertion() {
        super();
    }

    public AssertionStatus checkRequest( Request request, Response response ) throws PolicyAssertionException {
        try {
            PrincipalCredentials pc = request.getPrincipalCredentials();
            if ( pc == null ) {
                // No finder has been run yet!
                Class credFinderClass = getCredentialFinderClass();
                CredentialFinder finder = (CredentialFinder)Locator.getDefault().lookup( credFinderClass );
                if ( finder == null ) throw new PolicyAssertionException( "Couldn't locate an appropriate CredentialFinder!" );
                pc = finder.findCredentials( request );
            }

            if ( pc == null ) {
                response.setAuthenticationMissing( true );
                _log.info( AssertionStatus.AUTH_REQUIRED.getMessage() );
                return AssertionStatus.AUTH_REQUIRED;
            } else {
                request.setPrincipalCredentials( pc );
                return doCheckRequest( request, response );
            }
        } catch ( CredentialFinderException cfe ) {
            throw new PolicyAssertionException( cfe.getMessage(), cfe );
        }
    }

    public abstract AssertionStatus doCheckRequest( Request request, Response response ) throws CredentialFinderException;
    public abstract Class getCredentialFinderClass();

    protected Category _log = Category.getInstance( getClass() );
}
