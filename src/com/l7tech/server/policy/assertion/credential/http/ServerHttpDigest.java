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
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.DigestSessions;
import com.l7tech.identity.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;

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

    /**
     * We have little choice but to return a hardcoded value here.
     *
     * @param request
     * @return
     */
    protected String realm( Request request ) {
        return HttpDigest.REALM;
        /*
        String realm = _data.getRealm();
        if ( realm == null || realm.length() == 0 ) {
            realm = request.getParameter( Request.PARAM_SERVER_NAME ) + ":"
                + request.getParameter( Request.PARAM_SERVER_PORT );
        }
        return realm;
        */
    }

    protected AssertionStatus doCheckCredentials(Request request, Response response) {
        Map authParams = (Map)request.getParameter( Request.PARAM_HTTP_AUTH_PARAMS );
        String nonce = (String)authParams.get( HttpDigest.PARAM_NONCE );
        String userName = (String)authParams.get( HttpDigest.PARAM_USERNAME );
        String realmName = (String)authParams.get( HttpDigest.PARAM_REALM );
        String nc = (String)authParams.get( HttpDigest.PARAM_NC );
        String cnonce = (String)authParams.get( HttpDigest.PARAM_CNONCE );
        String qop = (String)authParams.get( HttpDigest.PARAM_QOP );
        String uri = (String)authParams.get( HttpDigest.PARAM_URI );
        String digestResponse = (String)authParams.get( HttpDigest.PARAM_RESPONSE );

        if ( (userName == null) || (realmName == null) || (nonce == null)
             || (uri == null) || ( digestResponse == null) )
            return AssertionStatus.AUTH_REQUIRED;

        if (qop != null && (cnonce == null || nc == null))
            return AssertionStatus.AUTH_REQUIRED;

        DigestSessions sessions = DigestSessions.getInstance();

        if ( sessions.use( nonce ) ) {
            _log.fine( "Nonce " + nonce + " still valid" );
            return AssertionStatus.NONE;
        } else {
            _log.info( "Nonce " + nonce + " expired" );
            sessions.invalidate( nonce );
            return AssertionStatus.AUTH_FAILED;
        }
    }

    protected PrincipalCredentials doFindCredentials( Request request, Response response ) throws CredentialFinderException {
        Map authParams = (Map)request.getParameter( Request.PARAM_HTTP_AUTH_PARAMS );

        if ( authParams == null ) return null;

        String userName = (String)authParams.get( HttpDigest.PARAM_USERNAME );
        String realmName = (String)authParams.get( HttpDigest.PARAM_REALM );
        String digestResponse = (String)authParams.get( HttpDigest.PARAM_RESPONSE );

        authParams.put( HttpDigest.PARAM_METHOD, request.getParameter( Request.PARAM_HTTP_REQUEST_URI ) );

        if ( (userName == null) || (realmName == null) || ( digestResponse == null) )
            return null;

        User u = new User();
        u.setLogin( userName );

        try {
            return new PrincipalCredentials( u, digestResponse.getBytes( ENCODING ), CredentialFormat.DIGEST, realmName, authParams );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException( e );
        }
    }

    private Map myChallengeParams( String nonce ) {
        Map params = new HashMap();
        params.put( HttpDigest.PARAM_QOP, HttpDigest.QOP_AUTH );
        params.put( HttpDigest.PARAM_NONCE, nonce );
        params.put( HttpDigest.PARAM_OPAQUE, HexUtils.encodeMd5Digest( _md5.digest( nonce.getBytes() ) ) );
        return params;
    }

    protected Map challengeParams( Request request, Response response ) {
        Map requestAuthParams = (Map)request.getParameter( Request.PARAM_HTTP_AUTH_PARAMS );
        DigestSessions sessions = DigestSessions.getInstance();

        String nonce;
        if ( requestAuthParams == null )
            nonce = null;
        else
            nonce = (String)requestAuthParams.get( HttpDigest.PARAM_NONCE );

        if ( nonce == null || nonce.length() == 0 ) {
            // New session
            String newNonce = sessions.generate( request, _data.getNonceTimeout(), _data.getMaxNonceCount() );
            return myChallengeParams( newNonce );
        } else {
            // Existing digest session
            if ( !sessions.use( nonce ) ) {
                // Nonce has been invalidated or is expired
                sessions.invalidate( nonce );
                nonce = sessions.generate( request, _data.getNonceTimeout(), _data.getMaxNonceCount() );
            }
            return myChallengeParams( nonce );
        }
    }

    protected String scheme() {
        return HttpDigest.SCHEME;
    }

    protected HttpDigest _data;
    protected MessageDigest _md5;
    protected Map _nonceTokens = new HashMap(37);
}