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

    protected PrincipalCredentials findCredentials( Request request, Response response ) throws IOException, CredentialFinderException {
        String wwwAuthorize = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );

        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) {
            challenge( request, response );
            return null;
        }

        Map authParams = new HashMap();
        StringTokenizer stok = new StringTokenizer( wwwAuthorize, " " );
        String scheme = null;
        String token, name, value;
        while ( stok.hasMoreTokens() ) {
            token = stok.nextToken();
            int epos = token.indexOf("=");
            if ( epos >= 0 ) {
                name = token.substring(0,epos);
                value = token.substring(1,epos+1);
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
        String realm = _data.getRealm();
        if ( realm != null && realm.length() > 0 ) {
            challengeHeader.append( _data.PARAM_REALM );
            challengeHeader.append( "=" );
            challengeHeader.append( quoted( _data.getRealm() ) );
        }

        Map challengeParams = challengeParams( request, response );
        String name, value;
        for (Iterator i = challengeParams.keySet().iterator(); i.hasNext();) {
            name = (String)i.next();
            value = (String)challengeParams.get(name);
            if ( name != null && value != null ) {
                challengeHeader.append( name );
                challengeHeader.append( "=" );
                challengeHeader.append( quoted( value ) );
            }
        }

        String challenge = challengeHeader.toString();

        _log.fine( "Sending WWW-Authenticate: " + challenge );
        response.setParameter( Response.PARAM_HTTP_WWWAUTHENTICATE, challenge.toString() );
    }

    private String quoted( String value ) {
        if ( value.indexOf( " " ) >= 0 )
            return '"' + value + '"';
        else
            return value;
    }

    protected abstract AssertionStatus doCheckCredentials( Request request, Response response );
    protected abstract PrincipalCredentials doFindCredentials( Request request, Response response ) throws IOException, CredentialFinderException;
    protected abstract Map challengeParams( Request request, Response response );
    protected abstract String scheme();

    protected HttpCredentialSourceAssertion _data;
}
