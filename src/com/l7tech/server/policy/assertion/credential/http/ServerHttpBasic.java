/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.apache.axis.encoding.Base64;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpBasic extends ServerHttpCredentialSource implements ServerAssertion {
    public static final String ENCODING = "UTF-8";
    public static final String SCHEME = "Basic";
    public static final String REALM = "L7SSGBasicRealm";

    protected Map challengeParams(Request request, Response response) {
        return Collections.EMPTY_MAP;
    }

    protected String scheme() {
        return SCHEME;
    }

    public LoginCredentials findCredentials( Request request, Response response ) throws IOException, CredentialFinderException {
        String wwwAuthorize = (String)request.getParameter( Request.PARAM_HTTP_AUTHORIZATION );
        return findCredentials( wwwAuthorize );
    }

    public LoginCredentials findCredentials( String wwwAuthorize ) throws IOException, CredentialFinderException {
        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) return null;

        int spos = wwwAuthorize.indexOf(" ");
        if ( spos < 0 ) {
            logger.fine( "WWW-Authorize header contains no space; ignoring");
            return null;
        }

        String scheme = wwwAuthorize.substring( 0, spos );
        String base64 = wwwAuthorize.substring( spos + 1 );
        if ( !scheme().equals(scheme) ) {
            logger.fine( "WWW-Authorize scheme not Basic; ignoring");
            return null;
        }

        String userPassRealm = new String( Base64.decode( base64 ), ENCODING );
        String login = null;
        String pass = null;

        int cpos = userPassRealm.indexOf(":");
        if ( cpos >= 0 ) {
            login = userPassRealm.substring( 0, cpos );
            pass = userPassRealm.substring( cpos + 1 );

            logger.fine( "Found HTTP Basic credentials for user " + login );

            return new LoginCredentials( login, pass.getBytes(ENCODING), CredentialFormat.CLEARTEXT, null );
        } else {
            // No colons
            String err = "Invalid HTTP Basic format (no colon(s))";
            logger.warning(err);
            return null;
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
    private final Logger logger = Logger.getLogger(getClass().getName());
}
