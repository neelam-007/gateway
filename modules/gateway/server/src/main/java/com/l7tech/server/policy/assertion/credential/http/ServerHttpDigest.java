/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.policy.assertion.credential.DigestSessions;
import com.l7tech.util.HexUtils;
import com.l7tech.security.token.http.HttpDigestToken;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class ServerHttpDigest extends ServerHttpCredentialSource<HttpDigest> {
    private static final Logger logger = Logger.getLogger(ServerHttpDigest.class.getName());
    private final Auditor auditor;

    public ServerHttpDigest(HttpDigest data, ApplicationContext springContext) {
        super(data, springContext);
        this.auditor = new Auditor(this, springContext, logger);
    }

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
            auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_VALID, nonce, userName);
            return AssertionStatus.NONE;
        } else {
            auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_EXPIRED, nonce, userName);
            sessions.invalidate( nonce );
            return AssertionStatus.AUTH_FAILED;
        }
    }

    protected LoginCredentials doFindCredentials( Message request, Map<String, String> authParams )
            throws CredentialFinderException
    {
        if ( authParams == null ) return null;

        String userName = authParams.get( HttpDigest.PARAM_USERNAME );
        String realmName = authParams.get( HttpDigest.PARAM_REALM );
        String digestResponse = authParams.get( HttpDigest.PARAM_RESPONSE );

        authParams.put( HttpDigest.PARAM_URI, request.getHttpRequestKnob().getRequestUri() );
        authParams.put( HttpDigest.PARAM_METHOD, request.getHttpRequestKnob().getMethod().name() );

        if ( (userName == null) || (realmName == null) || ( digestResponse == null) )
            return null;

        return LoginCredentials.makeLoginCredentials( new HttpDigestToken( userName, digestResponse, realmName, authParams), assertion.getClass());
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected Map<String,String> findCredentialAuthParams(LoginCredentials pc, Map<String,String> authParam) {
        Object payload = pc.getPayload();
        if (payload instanceof Map) {
            return (Map<String,String>) payload;
        }
        return authParam;
    }

    private Map<String,String> myChallengeParams( String nonce ) {
        Map<String,String> params = new HashMap<String,String>();
        params.put( HttpDigest.PARAM_QOP, HttpDigest.QOP_AUTH );
        params.put( HttpDigest.PARAM_NONCE, nonce );
        params.put( HttpDigest.PARAM_OPAQUE, HexUtils.encodeMd5Digest( HexUtils.getMd5Digest( nonce.getBytes() ) ) );
        return params;
    }

    protected Map<String,String> challengeParams( Message request, Map<String, String> requestAuthParams ) {
        DigestSessions sessions = DigestSessions.getInstance();

        String nonce = requestAuthParams == null ? null : requestAuthParams.get(HttpDigest.PARAM_NONCE);

        if ( nonce == null || nonce.length() == 0 ) {
            // New session
            String newNonce = sessions.generate( request, assertion.getNonceTimeout(), assertion.getMaxNonceCount() );
            auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_GENERATED, newNonce);
            return myChallengeParams( newNonce );
        } else {
            // Existing digest session
            if ( !sessions.use( nonce ) ) {
                // Nonce has been invalidated or is expired
                final String username = requestAuthParams.get(HttpDigest.PARAM_USERNAME);
                auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_EXPIRED, nonce, username == null ? "<unknown>" : username);
                sessions.invalidate( nonce );
                nonce = sessions.generate( request, assertion.getNonceTimeout(), assertion.getMaxNonceCount() );
                auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_GENERATED, nonce);
            }
            return myChallengeParams( nonce );
        }
    }

    protected String scheme() {
        return HttpDigest.SCHEME;
    }

    protected LoginCredentials findCredentials( Message request, Map<String, String> authParams )
            throws IOException, CredentialFinderException
    {
        String authorization = request.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);

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
                            throw new CredentialFinderException( "Unterminated quoted string in WWW-Authorize header" );
                        }
                    }
                }

                authParams.put( name, value );
            } else {
                if ( scheme == null ) {
                    scheme = token;
                    authParams.put( HttpCredentialSourceAssertion.PARAM_SCHEME, scheme );
                } else {
                    throw new CredentialFinderException( "Unexpected value '" + token + "' in WWW-Authorize header" );
                }

                if ( !scheme().equals(scheme) ) {
                    auditor.logAndAudit(AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER);
                    return null;
                }
            }
        }

        return doFindCredentials( request, authParams );
    }
}