/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerHttpCredentialSource extends ServerCredentialSourceAssertion implements ServerAssertion {
    public ServerHttpCredentialSource( HttpCredentialSourceAssertion data ) {
        super( data );
        _data = data;
    }

    public AssertionStatus checkCredentials( Request request, Response response ) throws CredentialFinderException {
        PrincipalCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) return challenge( request, response );
        String realm = pc.getRealm();
        if ( ( realm == null && _data.getRealm() == null ) || realm != null && realm.equals( _data.getRealm() ) ) {
            return AssertionStatus.NONE;
        }
        return challenge( request, response );
    }

    protected abstract String scheme();
    protected abstract AssertionStatus doCheckCredentials( Request request, Response response );

    protected AssertionStatus challenge( Request request, Response response ) {
        AssertionResult result = new AssertionResult( _data, request, AssertionStatus.AUTH_REQUIRED, "Authentication Required", new String[] { scheme() } );
        response.addResult( result );
        return AssertionStatus.FALSIFIED;
    }

    protected HttpCredentialSourceAssertion _data;
}
