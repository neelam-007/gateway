/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.util.HexUtils;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.DigestSessions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpDigest extends ServerHttpCredentialSource implements ServerAssertion {
    public ServerHttpDigest( HttpDigest data ) {
        super( data );
        _data = data;
        try {
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch ( NoSuchAlgorithmException e ) {
            throw new RuntimeException( e );
        }
    }

    public static final String ENCODING = "UTF-8";
    public static final String SCHEME = "Digest";


    /**
     * Generates the WWW-Authenticate header.
     * <p>
     * The header MUST follow this template :
     * <pre>
     *      WWW-Authenticate    = "WWW-Authenticate" ":" "Digest"
     *                            digest-challenge
     *
     *      digest-challenge    = 1#( realm | [ domain ] | nOnce |
     *                  [ digest-opaque ] |[ stale ] | [ algorithm ] )
     *
     *      realm               = "realm" "=" realm-value
     *      realm-value         = quoted-string
     *      domain              = "domain" "=" <"> 1#URI <">
     *      nonce               = "nonce" "=" nonce-value
     *      nonce-value         = quoted-string
     *      opaque              = "opaque" "=" quoted-string
     *      stale               = "stale" "=" ( "true" | "false" )
     *      algorithm           = "algorithm" "=" ( "MD5" | token )
     * </pre>
     *
     * @param request HTTP Servlet request
     * @param response HTTP Servlet response
     * @param nOnce nonce token
     */
    protected void setAuthenticateHeader( Request request,
                                          Response response,
                                          String nOnce ) {

        // Get the realm name
        String realmName = _data.getRealm();
        if (realmName == null)
            realmName = request.getParameter( Request.PARAM_SERVER_NAME ) + ":"
                + request.getParameter( Request.PARAM_SERVER_PORT );

        byte[] buffer = _md5.digest(nOnce.getBytes());

        String authenticateHeader = "Digest realm=\"" + realmName + "\", "
            +  "qop=\"auth\", nonce=\"" + nOnce + "\", " + "opaque=\""
            + HexUtils.encodeMd5Digest(buffer) + "\"";

        response.setParameter( Response.PARAM_HTTP_WWWAUTHENTICATE, authenticateHeader );
    }

    protected AssertionStatus doCheckCredentials( Request request, Response response ) {
        Map authParams = (Map)request.getParameter( Request.PARAM_HTTP_AUTH_PARAMS );
        String nonce = (String)authParams.get( HttpDigest.PARAM_NONCE );
        String userName = (String)authParams.get( HttpDigest.PARAM_USERNAME );
        String realmName = (String)authParams.get( HttpDigest.PARAM_REALM );
        String nc = (String)authParams.get( HttpDigest.PARAM_NC );
        String cnonce = (String)authParams.get( HttpDigest.PARAM_CNONCE );
        String qop = (String)authParams.get( HttpDigest.PARAM_QOP );
        String uri = (String)authParams.get( HttpDigest.PARAM_URI );
        String digestResponse = null;
        String opaque = null;
        String method = (String)request.getParameter( Request.PARAM_HTTP_METHOD );

        if ( (userName == null) || (realmName == null) || (nonce == null)
             || (uri == null) || (response == null) )
            return AssertionStatus.AUTH_FAILED;

        if (qop != null && (cnonce == null || nc == null))
            return AssertionStatus.AUTH_FAILED;

        String a2 = method + ":" + uri;

        String md5a2 = HexUtils.encodeMd5Digest( _md5.digest(a2.getBytes()) );

        return ( authenticate(userName, digestResponse, nonce, nc, cnonce, qop,
                                   realmName, md5a2));
    }

    protected PrincipalCredentials doFindCredentials( Request request, Response response ) throws CredentialFinderException {
        Map authParams = (Map)request.getParameter( Request.PARAM_HTTP_AUTH_PARAMS );
        // TODO
        return null;
    }

    protected Map challengeParams( Request request, Response response ) {
        Map requestAuthParams = (Map)request.getParameter( Request.PARAM_HTTP_AUTH_PARAMS );
        Map params = new HashMap();

        String requestNonce = (String)requestAuthParams.get( HttpDigest.PARAM_NONCE );
        if ( requestNonce == null || requestNonce.length() == 0 ) {
            String responseNonce = DigestSessions.getInstance().generateNonce( request, _data.getNonceTimeout(), _data.getMaxNonceCount() );

        } else {

        }


        // TODO
        return null;
    }

    protected String scheme() {
        return SCHEME;
    }

    private AssertionStatus authenticate( String userName, String digestResponse, String nonce, String nc, String cnonce, String qop, String realmName, String md5a2 ) {
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    protected HttpDigest _data;
    protected MessageDigest _md5;
    protected Map _nonceTokens = new HashMap(37);
}