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
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        LoginCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) return AssertionStatus.AUTH_REQUIRED;
        String requestRealm = pc.getRealm();
        String assertRealm = realm( request );

        if ( requestRealm == null || requestRealm.length() == 0 ) requestRealm = assertRealm;

        if ( requestRealm.equals( assertRealm ) ) {
            return doCheckCredentials( request, response );
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

    protected void challenge( Request request, Response response ) {
        String scheme = scheme();
        StringBuffer challengeHeader = new StringBuffer( scheme );
        challengeHeader.append( " " );
        String realm = realm( request );
        if ( realm != null && realm.length() > 0 ) {
            challengeHeader.append( HttpCredentialSourceAssertion.PARAM_REALM );
            challengeHeader.append( "=" );
            challengeHeader.append( quoted( realm ) );
        }

        Map challengeParams = challengeParams( request, response );
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
        Object[] existingChallenges = response.getParameterValues(Response.PARAM_HTTP_WWWAUTHENTICATE);
        if ( existingChallenges == null || existingChallenges.length == 0 ) {
            response.setParameter( Response.PARAM_HTTP_WWWAUTHENTICATE, challenge.toString() );
        } else {
            String[] newChallenges = new String[existingChallenges.length+1];
            if ( "Digest".equals(scheme) ) {
                // Put Digest first
                System.arraycopy( existingChallenges, 0, newChallenges, 1, existingChallenges.length );
                newChallenges[0] = challenge.toString();
            } else {
                // Put anything else later
                System.arraycopy( existingChallenges, 0, newChallenges, 0, existingChallenges.length );
                newChallenges[newChallenges.length-1] = challenge.toString();
            }
            response.setParameter( Response.PARAM_HTTP_WWWAUTHENTICATE, newChallenges );
        }
    }

    protected abstract String realm( Request request );

    private String quoted( String value ) {
        if ( value == null )
            return "\"\"";
        else
            return '"' + value + '"';
    }

    protected abstract AssertionStatus doCheckCredentials( Request request, Response response );
    protected abstract Map challengeParams( Request request, Response response );
    protected abstract String scheme();

    protected HttpCredentialSourceAssertion _data;
    protected Logger logger = LogManager.getInstance().getSystemLogger();
}
