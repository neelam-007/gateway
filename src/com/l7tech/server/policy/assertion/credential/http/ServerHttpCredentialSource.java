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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.logging.Level;
import java.io.IOException;

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
        LogManager.getInstance().getSystemLogger().log( level, err );
        throw new CredentialFinderException( err );
    }

    protected PrincipalCredentials findCredentials( Request request, Response response ) throws IOException, CredentialFinderException {
        String authorization = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );

        if ( authorization == null || authorization.length() == 0 ) {
            return null;
        }

        Map authParams = new HashMap();
        StringTokenizer stok = new StringTokenizer( authorization, ", " );
        String scheme = null;
        String token, name, value;
        while ( stok.hasMoreTokens() ) {
            token = stok.nextToken();
            int epos = token.indexOf("=");
            if ( epos >= 0 ) {
                name = token.substring(0,epos);
                value = token.substring(epos+1);
                if ( value.startsWith("\"") ) {
                    if ( value.endsWith("\"") ) {
                        // Single-word quoted string
                        value = value.substring( 1, value.length() - 1 );
                    } else {
                        // Multi-word quoted string
                        StringBuffer valueBuffer = new StringBuffer( value.substring(1) );
                        value = null;
                        while ( stok.hasMoreTokens() ) {
                            token = stok.nextToken();
                            if ( token.endsWith("\"") ) {
                                valueBuffer.append( token.substring( 0, token.length()-1 ) );
                                value = valueBuffer.toString();
                                break;
                            } else
                                valueBuffer.append( token );
                            valueBuffer.append( " " );
                        }
                        if ( value == null ) {
                            CredentialFinderException cfe = new CredentialFinderException( "Unterminated quoted string in WWW-Authorize header" );
                            _log.log( Level.WARNING, cfe.toString(), cfe );
                            throw cfe;
                        }
                    }
                }

                authParams.put( name, value );
            } else {
                if ( scheme == null ) {
                    scheme = token;
                    authParams.put( HttpCredentialSourceAssertion.PARAM_SCHEME, scheme );
                } else {
                    CredentialFinderException cfe = new CredentialFinderException( "Unexpected value '" + token + "' in WWW-Authorize header" );
                    _log.log( Level.WARNING, cfe.toString(), cfe );
                    throw cfe;
                }
                if ( !scheme().equals(scheme) ) {
                    throwError( Level.FINE, "Invalid scheme '" + scheme + "' in WWW-Authorize: Digest header" );
                }
            }
        }

        request.setParameter( Request.PARAM_HTTP_AUTH_PARAMS, authParams );

        return doFindCredentials( request, response );
    }

    protected void challenge( Request request, Response response ) {
        StringBuffer challengeHeader = new StringBuffer( scheme() );
        challengeHeader.append( " " );
        String realm = realm( request );
        if ( realm != null && realm.length() > 0 ) {
            challengeHeader.append( _data.PARAM_REALM );
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

        _log.fine( "Sending WWW-Authenticate: " + challenge );
        response.setParameter( Response.PARAM_HTTP_WWWAUTHENTICATE, challenge.toString() );
    }

    protected abstract String realm( Request request );

    private String quoted( String value ) {
        if ( value == null )
            return "\"\"";
        else
            return '"' + value + '"';
    }

    protected abstract AssertionStatus doCheckCredentials( Request request, Response response );
    protected abstract PrincipalCredentials doFindCredentials( Request request, Response response ) throws IOException, CredentialFinderException;
    protected abstract Map challengeParams( Request request, Response response );
    protected abstract String scheme();

    protected HttpCredentialSourceAssertion _data;
}
