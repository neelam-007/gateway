/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

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

    private static final String NONCEKEY = "Layer7-SSG-DigestNonceKey";

    /**
     * Generate a unique token. The token is generated according to the
     * following pattern. NOnceToken = Base64 ( MD5 ( client-IP ":"
     * time-stamp ":" private-key ) ).
     *
     * @param request HTTP Servlet request
     */
    protected String generateNonce( Request request ) {

        long currentTime = System.currentTimeMillis();

        String nonceValue = request.getTransportMetadata().getParameter( Request.PARAM_HTTP_REMOTE_ADDR ) + ":" +
            currentTime + ":" + NONCEKEY;

        byte[] buffer = _md5.digest(nonceValue.getBytes());
        nonceValue = HexUtils.encodeMd5Digest(buffer);

        // Updating the value in the no once hashtable
        _nonceTokens.put(nonceValue, new Long( currentTime + _data.getNonceTimeout() ));

        return nonceValue;
    }

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

                if ( HttpDigest.USERNAME.equals(name) )
                    login = value;
                else if ( HttpDigest.RESPONSE.equals( name ) )
                    ha1 = value;
                else if ( HttpDigest.REALM.equals( name ) )
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
                if ( !SCHEME.equals(scheme) ) {
                    throwError( Level.FINE, "Invalid scheme '" + scheme + "' in WWW-Authorize: Digest header" );
                }
            }
        }

        request.setParameter( Request.PARAM_HTTP_AUTH_PARAMS, authParams );

        User u = new User();
        u.setLogin( login );

        return new PrincipalCredentials( u, ha1.getBytes(ENCODING), CredentialFormat.DIGEST, realm );
    }


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
            realmName = request.getParameter( Request.PARAM_HTTP_SERVER_NAME ) + ":"
                + request.getParameter( Request.PARAM_HTTP_SERVER_PORT );

        byte[] buffer = _md5.digest(nOnce.getBytes());

        String authenticateHeader = "Digest realm=\"" + realmName + "\", "
            +  "qop=\"auth\", nonce=\"" + nOnce + "\", " + "opaque=\""
            + HexUtils.encodeMd5Digest(buffer) + "\"";

        response.setParameter( Response.PARAM_HTTP_WWWAUTHENTICATE, authenticateHeader );
    }

    protected AssertionStatus doCheckCredentials( Request request, Response response ) {
        Map authParams = (Map)request.getParameter( Request.PARAM_HTTP_AUTH_PARAMS );
        String nonce = (String)authParams.get( HttpDigest.NONCE );
        StringBuffer challenge = new StringBuffer();

        String userName = (String)authParams.get( HttpDigest.USERNAME );
        String realmName = (String)authParams.get( HttpDigest.REALM );
        String nOnce = (String)authParams.get( HttpDigest.NONCE );
        String nc = (String)authParams.get( HttpDigest.NC );
        String cnonce = (String)authParams.get( HttpDigest.CNONCE );
        String qop = (String)authParams.get( HttpDigest.QOP );
        String uri = (String)authParams.get( HttpDigest.URI );
        String digestResponse = null;
        String opaque = null;
        String method = (String)request.getParameter( Request.PARAM_HTTP_METHOD );

        if ( (userName == null) || (realmName == null) || (nOnce == null)
             || (uri == null) || (response == null) )
            return AssertionStatus.AUTH_FAILED;

        if (qop != null && (cnonce == null || nc == null))
            return AssertionStatus.AUTH_FAILED;

        String a2 = method + ":" + uri;

        String md5a2 = HexUtils.encodeMd5Digest( _md5.digest(a2.getBytes()) );

        response.setParameter( Response.PARAM_HTTP_WWWAUTHENTICATE, challenge.toString() );

        return ( authenticate(userName, digestResponse, nOnce, nc, cnonce, qop,
                                   realmName, md5a2));

    }

    private AssertionStatus authenticate( String userName, String digestResponse, String nonce, String nc, String cnonce, String qop, String realmName, String md5a2 ) {
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    protected HttpDigest _data;
    protected MessageDigest _md5;
    protected Map _nonceTokens = new HashMap(37);
}