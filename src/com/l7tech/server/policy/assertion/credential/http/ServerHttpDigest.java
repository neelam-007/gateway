/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.DigestSessions;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpDigest extends ServerHttpCredentialSource implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerHttpDigest.class.getName());

    public ServerHttpDigest( HttpDigest data, ApplicationContext springContext ) {
        super(data);
        _data = data;
        auditor = new Auditor(this, springContext, logger);
    }

    public static final String ENCODING = "UTF-8";

    /**
     * Returns the authentication realm to use for this assertion.
     * We have little choice but to return a hardcoded value here because passwords are hashed with
     * this realm when they are stored in the database and the cleartext password is never transferred
     * in HTTP digest.
     *
     * @return a hardcoded realm. never null
     */
    protected String realm() {
        return HttpDigest.REALM;
    }

    protected AssertionStatus checkAuthParams(Map authParams) {
        if ( authParams == null ) return AssertionStatus.AUTH_REQUIRED;

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
            logger.fine( "Nonce " + nonce + " for user " + userName + " still valid" );
            return AssertionStatus.NONE;
        } else {
            logger.info( "Nonce " + nonce + " for user " + userName + " expired" );
            sessions.invalidate( nonce );
            return AssertionStatus.AUTH_FAILED;
        }
    }

    protected LoginCredentials doFindCredentials( Message request, Map authParams )
            throws CredentialFinderException
    {
        if ( authParams == null ) return null;

        String userName = (String)authParams.get( HttpDigest.PARAM_USERNAME );
        String realmName = (String)authParams.get( HttpDigest.PARAM_REALM );
        String digestResponse = (String)authParams.get( HttpDigest.PARAM_RESPONSE );

        authParams.put( HttpDigest.PARAM_URI, request.getHttpRequestKnob().getRequestUri() );
        authParams.put( HttpDigest.PARAM_METHOD, request.getHttpRequestKnob().getMethod() );

        if ( (userName == null) || (realmName == null) || ( digestResponse == null) )
            return null;

        return new LoginCredentials( userName, digestResponse.toCharArray(),
                                     CredentialFormat.DIGEST, _data.getClass(), realmName, authParams );
    }

    private Map myChallengeParams( String nonce ) {
        Map params = new HashMap();
        params.put( HttpDigest.PARAM_QOP, HttpDigest.QOP_AUTH );
        params.put( HttpDigest.PARAM_NONCE, nonce );
        params.put( HttpDigest.PARAM_OPAQUE, HexUtils.encodeMd5Digest( HexUtils.getMd5().digest( nonce.getBytes() ) ) );
        return params;
    }

    protected Map challengeParams( Message request, Map requestAuthParams ) {
        DigestSessions sessions = DigestSessions.getInstance();

        String nonce;
        if ( requestAuthParams == null )
            nonce = null;
        else
            nonce = (String)requestAuthParams.get( HttpDigest.PARAM_NONCE );

        if ( nonce == null || nonce.length() == 0 ) {
            // New session
            String newNonce = sessions.generate( request, _data.getNonceTimeout(), _data.getMaxNonceCount() );
            logger.fine( "Generated new nonce " + newNonce );
            return myChallengeParams( newNonce );
        } else {
            // Existing digest session
            if ( !sessions.use( nonce ) ) {
                // Nonce has been invalidated or is expired
                logger.info( "Nonce " + nonce + " is invalid or expired" );
                sessions.invalidate( nonce );
                nonce = sessions.generate( request, _data.getNonceTimeout(), _data.getMaxNonceCount() );
                logger.fine( "Generated new nonce " + nonce );
            }
            return myChallengeParams( nonce );
        }
    }

    protected String scheme() {
        return HttpDigest.SCHEME;
    }

    protected LoginCredentials findCredentials( Message request, Map authParams )
            throws IOException, CredentialFinderException
    {
        String authorization = request.getHttpRequestKnob().getHeaderSingleValue(AUTHORIZATION);

        if ( authorization == null || authorization.length() == 0 ) {
            return null;
        }

        StringTokenizer stok = new StringTokenizer( authorization, ", " );
        String scheme = null;
        String token, name, value;
        while ( stok.hasMoreTokens() ) {
            token = stok.nextToken();
            int epos = token.indexOf("=");
            if ( epos >= 0 ) {
                name = token.substring(0,epos);
                value = token.substring(epos+1);
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
                            logger.log( Level.WARNING, cfe.toString(), cfe );
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
                    logger.log( Level.WARNING, cfe.toString(), cfe );
                    throw cfe;
                }

                if ( !scheme().equals(scheme) ) {
                    logger.info("Invalid scheme '" + scheme + "' in WWW-Authorize header");
                    return null;
                }
            }
        }

        return doFindCredentials( request, authParams );
    }

    protected HttpDigest _data;
    protected Map _nonceTokens = new HashMap(37);
}