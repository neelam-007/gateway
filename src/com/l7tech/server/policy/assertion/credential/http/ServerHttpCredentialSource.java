/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;

import java.util.logging.Level;

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
        if ( pc == null ) return AssertionStatus.AUTH_REQUIRED;
        String realm = pc.getRealm();
        if ( ( realm == null && _data.getRealm() == null ) || realm != null && realm.equals( _data.getRealm() ) ) {
            return doCheckCredentials( request, response );
        } else {
            throw new CredentialFinderException( "Realm mismatch", AssertionStatus.AUTH_FAILED );
        }
    }

    protected void throwError( String err ) throws CredentialFinderException {
        throwError( Level.SEVERE, err );
    }

    protected void throwError( Level level, String err ) throws CredentialFinderException {
        LogManager.getInstance().getSystemLogger().log( level, err );
        throw new CredentialFinderException( err );
    }

    protected abstract AssertionStatus doCheckCredentials( Request request, Response response );

    protected HttpCredentialSourceAssertion _data;
}
