/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Collections;

import org.apache.axis.encoding.Base64;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpBasic extends ServerHttpCredentialSource implements ServerAssertion {
    public static final String ENCODING = "UTF-8";
    public static final String SCHEME = "Basic";
    public static final String REALM = "L7SSGBasicRealm";

    public PrincipalCredentials doFindCredentials( Request request, Response response ) throws IOException, CredentialFinderException {
        String wwwAuthorize = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );
        return findCredentials(wwwAuthorize);
    }

    protected Map challengeParams(Request request, Response response) {
        return Collections.EMPTY_MAP;
    }

    protected String scheme() {
        return SCHEME;
    }

    public PrincipalCredentials findCredentials( Request request, Response response ) throws IOException, CredentialFinderException {
        String wwwAuthorize = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );
        return findCredentials( wwwAuthorize );
    }

    public PrincipalCredentials findCredentials( String wwwAuthorize ) throws IOException, CredentialFinderException {
        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) return null;

        int spos = wwwAuthorize.indexOf(" ");
        if ( spos < 0 ) throwError( "Invalid HTTP Basic header format" );
        String scheme = wwwAuthorize.substring( 0, spos );
        String base64 = wwwAuthorize.substring( spos + 1 );
        if ( !scheme().equals(scheme) ) throwError( "Invalid HTTP Basic header scheme" );

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
            logger.severe(err);
            throw new CredentialFinderException( err );
        }
    }

    public ServerHttpBasic( HttpBasic data ) {
        super( data );
        _data = data;
    }

    protected String realm(Request request) {
        String realm = _data.getRealm();
        if ( realm == null ) realm = REALM;
        return realm;
    }

    protected AssertionStatus doCheckCredentials(Request request, Response response) {
        return AssertionStatus.NONE;
    }

    protected HttpBasic _data;
    private Logger logger = LogManager.getInstance().getSystemLogger();
}
