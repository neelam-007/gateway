/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.http;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.message.Request;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;
import org.apache.axis.encoding.Base64;
import java.io.IOException;
import java.util.logging.Level;

/**
 * @author alex
 */
public class HttpBasicCredentialFinder extends HttpCredentialFinder {
    public static final String ENCODING = "UTF-8";
    public static final String SCHEME = "Basic";

    private void throwError( String err ) throws CredentialFinderException {
        LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
        throw new CredentialFinderException( err );
    }

    public PrincipalCredentials findCredentials(Request request) throws IOException, CredentialFinderException {
        String wwwAuthorize = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );

        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) return null;

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

            return new PrincipalCredentials( u, pass.getBytes(ENCODING), CredentialFormat.CLEARTEXT, realm );
        } else {
            // No colons
            String err = "Invalid HTTP Basic format!";
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
            throw new CredentialFinderException( err );
        }
    }
}
