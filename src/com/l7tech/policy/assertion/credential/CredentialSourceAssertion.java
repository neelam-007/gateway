/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

import com.l7tech.credential.CredentialFinder;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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

    public AssertionStatus checkRequest( Request request, Response response ) throws IOException, PolicyAssertionException {
        try {
            PrincipalCredentials pc = request.getPrincipalCredentials();
            if ( pc == null ) {
                // No finder has been run yet!
                Class credFinderClass = getCredentialFinderClass();
                String classname = credFinderClass.getName();
                CredentialFinder finder = null;
                synchronized( _credentialFinders ) {
                    finder = (CredentialFinder)_credentialFinders.get( classname );
                    if ( finder == null ) {
                        finder = (CredentialFinder)credFinderClass.newInstance();
                        _credentialFinders.put( classname, finder );
                    }
                }
                pc = finder.findCredentials( request );
            }

            if ( pc == null ) {
                response.setAuthenticationMissing( true );
                LogManager.getInstance().getSystemLogger().log(Level.INFO, AssertionStatus.AUTH_REQUIRED.getMessage());
                return AssertionStatus.AUTH_REQUIRED;
            } else {
                request.setPrincipalCredentials( pc );
                return doCheckRequest( request, response );
            }
        } catch ( CredentialFinderException cfe ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, cfe);
            throw new PolicyAssertionException( cfe.getMessage(), cfe );
        } catch ( IllegalAccessException iae ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, iae);
            throw new PolicyAssertionException( iae.getMessage(), iae );
        } catch ( InstantiationException ie ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, ie);
            throw new PolicyAssertionException( ie.getMessage(), ie );
        }


    }

    public abstract AssertionStatus doCheckRequest( Request request, Response response ) throws CredentialFinderException;
    public abstract Class getCredentialFinderClass();

    protected transient Map _credentialFinders = new HashMap();
}
