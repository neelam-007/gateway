/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.apache.axis.encoding.Base64;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpBasic extends ServerHttpCredentialSource implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerHttpBasic.class.getName());

    public static final String ENCODING = "UTF-8";
    public static final String SCHEME = "Basic";
    public static final String REALM = "L7SSGBasicRealm";

    protected Map challengeParams(Message request, Map authParams) {
        return Collections.EMPTY_MAP;
    }

    protected String scheme() {
        return SCHEME;
    }

    public LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
        String wwwAuthorize = request.getHttpRequestKnob().getHeaderSingleValue(AUTHORIZATION);
        return findCredentials( wwwAuthorize );
    }

    public LoginCredentials findCredentials( String wwwAuthorize ) throws IOException {
        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) {
            logger.fine("No wwwAuthorize");
            return null;
        }

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

            return new LoginCredentials( login, pass.toCharArray(), CredentialFormat.CLEARTEXT,
                                         _data.getClass(), null );
        } else {
            // No colons
            String err = "Invalid HTTP Basic format (no colon(s))";
            logger.warning(err);
            return null;
        }
    }

    public ServerHttpBasic( HttpBasic data, ApplicationContext springContext ) {
        super(data);
        _data = data;
    }

    protected String realm() {
        String realm = _data.getRealm();
        if ( realm == null ) realm = REALM;
        return realm;
    }

    protected AssertionStatus checkAuthParams(Map authParams) {
        return AssertionStatus.NONE;
    }

    protected HttpBasic _data;
}
