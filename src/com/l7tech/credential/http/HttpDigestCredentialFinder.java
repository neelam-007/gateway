/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.http;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.identity.User;
import com.l7tech.message.Request;
import com.l7tech.logging.LogManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Retrieves HTTP Digest credentials from a request, if possible.
 *
 * @author alex
 */
public class HttpDigestCredentialFinder extends HttpCredentialFinder {
    public static final String ENCODING = "UTF-8";
    public static final String SCHEME = "Digest";

    public PrincipalCredentials findCredentials(Request request) throws IOException, CredentialFinderException {
        String wwwAuthorize = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );

        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) return null;

        Map authParams = new HashMap();
        StringTokenizer stok = new StringTokenizer( wwwAuthorize, " " );
        String login = null, scheme = null, ha1 = null, realm = null;
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
                            CredentialFinderException cfe = new CredentialFinderException( "Unterminated quoted string in WWW-Authorize Digest header" );
                            _log.log( Level.WARNING, cfe.toString(), cfe );
                            throw cfe;
                        }
                    }
                }

                if ( "username".equals(name) )
                    login = value;
                else if ( "response".equals( name ) )
                    ha1 = value;
                else if ( "realm".equals( name ) )
                    realm = value;

                authParams.put( name, value );
            } else {
                if ( scheme == null )
                    scheme = token;
                else {
                    CredentialFinderException cfe = new CredentialFinderException( "Unexpected value '" + token + "' in WWW-Authorize Digest header" );
                    _log.log( Level.WARNING, cfe.toString(), cfe );
                    throw cfe;
                }
                if ( !SCHEME.equals(scheme) ) throwError( "Invalid scheme '" + scheme + "' in WWW-Authorize: Digest header" );
            }
        }

        request.setParameter( Request.PARAM_HTTP_AUTH_PARAMS, authParams );

        User u = new User();
        u.setLogin( login );

        return new PrincipalCredentials( u, ha1.getBytes(ENCODING), CredentialFormat.DIGEST, realm );
    }

    protected Logger _log = LogManager.getInstance().getSystemLogger();

}
