/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerHttpCredentialSource extends ServerCredentialSourceAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerHttpCredentialSource.class.getName());

    public static final String AUTHORIZATION = "Authorization";

    public ServerHttpCredentialSource( HttpCredentialSourceAssertion data ) {
        super( data );
        _data = data;
    }

    public AssertionStatus checkCredentials( LoginCredentials pc, Map authParams ) throws CredentialFinderException {
        if ( pc == null ) return AssertionStatus.AUTH_REQUIRED;
        String requestRealm = pc.getRealm();
        String assertRealm = realm();

        if ( requestRealm == null || requestRealm.length() == 0 ) requestRealm = assertRealm;

        if ( requestRealm.equals( assertRealm ) ) {
            return checkAuthParams(authParams);
        } else {
            throw new CredentialFinderException( "Realm mismatch: Expected '" + assertRealm + "', got '"+ requestRealm, AssertionStatus.AUTH_FAILED );
        }
    }

    protected void throwError( String err ) throws CredentialFinderException {
        throwError( Level.SEVERE, err );
    }

    protected void throwError( Level level, String err ) throws CredentialFinderException {
        logger.log( level, err );
        throw new CredentialFinderException( err );
    }

    protected void challenge(PolicyEnforcementContext context, Map authParams) {
        String scheme = scheme();
        StringBuffer challengeHeader = new StringBuffer( scheme );
        challengeHeader.append( " " );
        String realm = realm();
        if ( realm != null && realm.length() > 0 ) {
            challengeHeader.append( HttpCredentialSourceAssertion.PARAM_REALM );
            challengeHeader.append( "=" );
            challengeHeader.append( quoted( realm ) );
        }

        Map challengeParams = challengeParams(context.getRequest(), authParams);
        String name, value;
        Iterator i = challengeParams.keySet().iterator();
        if ( i.hasNext() ) challengeHeader.append( ", " );

        while ( i.hasNext() ) {
            name = (String)i.next();
            value = (String)challengeParams.get(name);
            if ( name != null && value != null ) {
                challengeHeader.append( name );
                challengeHeader.append( "=" );
                challengeHeader.append( quoted( value ) );
                if ( i.hasNext() ) challengeHeader.append( ", " );
            }
        }

        String challenge = challengeHeader.toString();

        logger.fine( "Sending WWW-Authenticate: " + challenge );
        HttpResponseKnob httpResponse = context.getResponse().getHttpResponseKnob();
        httpResponse.addHeader("WWW-Authenticate", challenge);
        // TODO worry about putting Digest first?
    }

    /**
     * Returns the authentication realm to use for this assertion.
     *
     * @return the authentication realm to use for this assertion.  Never null.
     */
    protected abstract String realm();

    private String quoted( String value ) {
        if ( value == null )
            return "\"\"";
        else
            return '"' + value + '"';
    }

    protected abstract AssertionStatus checkAuthParams(Map authParams);
    protected abstract Map challengeParams(Message request, Map authParams);
    protected abstract String scheme();

    protected HttpCredentialSourceAssertion _data;

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context.getRequest().getKnob(HttpRequestKnob.class) == null) {
            logger.info("Request not HTTP; unable to extract HTTP credentials");
            return AssertionStatus.NOT_APPLICABLE;
        }
        return super.checkRequest(context);
    }
}
