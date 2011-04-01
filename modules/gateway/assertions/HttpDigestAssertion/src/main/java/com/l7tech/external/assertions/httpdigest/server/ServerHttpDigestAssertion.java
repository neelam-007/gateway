package com.l7tech.external.assertions.httpdigest.server;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.security.token.http.HttpDigestToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.external.assertions.httpdigest.HttpDigestAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpCredentialSource;
import com.l7tech.util.HexUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the HttpDigestAssertion.
 *
 * @see com.l7tech.external.assertions.httpdigest.HttpDigestAssertion
 * @author Alex (moved from package com.l7tech.server.policy.assertion.credential.http)
 */
public class ServerHttpDigestAssertion extends ServerHttpCredentialSource<HttpDigestAssertion> {

    // - PUBLIC

    public ServerHttpDigestAssertion(HttpDigestAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    // - PROTECTED
    
    /**
     * Returns the authentication realm to use for this assertion.
     * We have little choice but to return a hardcoded value here because passwords are hashed with
     * this realm when they are stored in the database and the cleartext password is never transferred
     * in HTTP digest.
     *
     * @return a hardcoded realm. never null
     */
    protected String realm() {
        return HexUtils.REALM;
    }

    protected AssertionStatus checkAuthParams(Map authParams) {
        if ( authParams == null ) return AssertionStatus.AUTH_REQUIRED;

        String nonce = (String)authParams.get( HttpDigestAssertion.PARAM_NONCE );
        String userName = (String)authParams.get( HttpDigestAssertion.PARAM_USERNAME );
        String realmName = (String)authParams.get( HttpDigestAssertion.PARAM_REALM );
        String nc = (String)authParams.get( HttpDigestAssertion.PARAM_NC );
        String cnonce = (String)authParams.get( HttpDigestAssertion.PARAM_CNONCE );
        String qop = (String)authParams.get( HttpDigestAssertion.PARAM_QOP );
        String uri = (String)authParams.get( HttpDigestAssertion.PARAM_URI );
        String digestResponse = (String)authParams.get( HttpDigestAssertion.PARAM_RESPONSE );

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

        String userName = authParams.get( HttpDigestAssertion.PARAM_USERNAME );
        String realmName = authParams.get( HttpDigestAssertion.PARAM_REALM );
        String digestResponse = authParams.get( HttpDigestAssertion.PARAM_RESPONSE );

        authParams.put( HttpDigestAssertion.PARAM_URI, request.getHttpRequestKnob().getRequestUri() );
        authParams.put( HttpDigestAssertion.PARAM_METHOD, request.getHttpRequestKnob().getMethod().name() );

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

    protected Map<String,String> challengeParams( Message request, Map<String, String> requestAuthParams ) {
        DigestSessions sessions = DigestSessions.getInstance();

        String nonce = requestAuthParams == null ? null : requestAuthParams.get(HttpDigestAssertion.PARAM_NONCE);

        if ( nonce == null || nonce.length() == 0 ) {
            // New session
            String newNonce = sessions.generate(assertion.getNonceTimeout(), assertion.getMaxNonceCount() );
            auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_GENERATED, newNonce);
            return myChallengeParams( newNonce );
        } else {
            // Existing digest session
            if ( !sessions.use( nonce ) ) {
                // Nonce has been invalidated or is expired
                final String username = requestAuthParams.get(HttpDigestAssertion.PARAM_USERNAME);
                auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_EXPIRED, nonce, username == null ? "<unknown>" : username);
                sessions.invalidate( nonce );
                nonce = sessions.generate(assertion.getNonceTimeout(), assertion.getMaxNonceCount() );
                auditor.logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_GENERATED, nonce);
            }
            return myChallengeParams( nonce );
        }
    }

    protected String scheme() {
        return HttpDigestAssertion.SCHEME;
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

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(ServerHttpDigestAssertion.class.getName());

    private final HttpDigestAssertion assertion;
    private final Auditor auditor;
    private final String[] variablesUsed;

    private Map<String,String> myChallengeParams( String nonce ) {
        Map<String,String> params = new HashMap<String,String>();
        params.put( HttpDigestAssertion.PARAM_QOP, HttpDigestAssertion.QOP_AUTH );
        params.put( HttpDigestAssertion.PARAM_NONCE, nonce );
        params.put( HttpDigestAssertion.PARAM_OPAQUE, HexUtils.encodeMd5Digest( HexUtils.getMd5Digest( nonce.getBytes() ) ) );
        return params;
    }
}
