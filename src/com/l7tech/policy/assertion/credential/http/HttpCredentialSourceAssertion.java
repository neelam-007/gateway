/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class HttpCredentialSourceAssertion extends CredentialSourceAssertion {

    public AssertionStatus checkCredentials( Request request, Response response ) throws CredentialFinderException {
        PrincipalCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) return challenge( request, response );
        String realm = pc.getRealm();
        if ( ( realm == null && _realm == null ) || realm != null && realm.equals( _realm ) ) {
            return AssertionStatus.NONE;
        }
        return challenge( request, response );
    }

    protected abstract String scheme();

    protected AssertionStatus challenge( Request request, Response response ) {
        AssertionResult result = new AssertionResult( this, request, AssertionStatus.AUTH_REQUIRED, "Authentication Required", new String[] { scheme() } );
        response.addResult( result );
        return AssertionStatus.FALSIFIED;
    }

    public String getRealm() {
        return _realm;
    }

    public void setRealm(String realm) {
        _realm = realm;
    }

    protected String _realm;
}
