/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.http;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.identity.User;
import com.l7tech.message.Request;
import org.apache.axis.encoding.Base64;
import org.apache.log4j.Category;

import java.io.UnsupportedEncodingException;

/**
 * @author alex
 */
public class HttpBasicCredentialFinder extends HttpCredentialFinder {
    public static final String ENCODING = "UTF-8";
    public static final String SCHEME = "Basic";

    private void throwError( String err ) throws CredentialFinderException {
        _log.error( err );
        throw new CredentialFinderException( err );
    }

    protected PrincipalCredentials doFindCredentials(Request request) throws CredentialFinderException {
        String wwwAuthorize = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );

        try {
            int spos = wwwAuthorize.indexOf(" ");
            if ( spos < 0 ) throwError( "Invalid HTTP Basic header format" );
            String scheme = wwwAuthorize.substring( 0, spos );
            String base64 = wwwAuthorize.substring( spos + 1 );
            if ( !SCHEME.equals(scheme) ) throwError( "Invalid HTTP Basic header scheme" );

            String userPassRealm = new String( Base64.decode( base64 ), ENCODING );
            int cpos1 = userPassRealm.indexOf(":");
            int cpos2 = userPassRealm.indexOf(":",cpos1+1);
            String login = null;
            String realm = null;
            String pass = null;
            if ( cpos1 >= 0 ) {
                login = userPassRealm.substring( 0, cpos1 );
                if ( cpos2 >= 0 )
                    realm = userPassRealm.substring( cpos2 + 1 );
                else
                    cpos2 = userPassRealm.length();

                pass = userPassRealm.substring( cpos1 + 1, cpos2 );

                User u = new User();
                u.setLogin( login );

                return new PrincipalCredentials( u, pass.getBytes(ENCODING), realm, this );
            } else {
                // No colons
                String err = "Invalid HTTP Basic format!";
                _log.warn( err );
                throw new CredentialFinderException( err );
            }
        } catch ( UnsupportedEncodingException uee ) {
            throw new CredentialFinderException( "Aieee!", uee );
        }
    }

    protected Category _log = Category.getInstance( getClass() );
}
