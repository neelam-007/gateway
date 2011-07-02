package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.security.token.http.HttpDigestToken;
import com.l7tech.server.policy.assertion.credential.DigestSessions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Server side implementation of the HttpDigestAssertion.
 *
 * @see HttpDigest
 * @author Alex (moved from package com.l7tech.server.policy.assertion.credential.http and back again.)
 */
public class ServerHttpDigest extends ServerHttpCredentialSource<HttpDigest> {

    // - PUBLIC

    public ServerHttpDigest(HttpDigest assertion) throws PolicyAssertionException {
        super(assertion);
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
    @Override
    protected String realm() {
        return HexUtils.REALM;
    }

    @Override
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
            logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_VALID, nonce, userName);
            return AssertionStatus.NONE;
        } else {
            logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_EXPIRED, nonce, userName);
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

    @Override
    protected Map<String,String> challengeParams( Message request, Map<String, String> requestAuthParams ) {
        DigestSessions sessions = DigestSessions.getInstance();

        String nonce = requestAuthParams == null ? null : requestAuthParams.get(HttpDigest.PARAM_NONCE);

        if ( nonce == null || nonce.length() == 0 ) {
            // New session
            String newNonce = sessions.generate(assertion.getNonceTimeout(), assertion.getMaxNonceCount() );
            logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_GENERATED, newNonce);
            return myChallengeParams( newNonce );
        } else {
            // Existing digest session
            if ( !sessions.use( nonce ) ) {
                // Nonce has been invalidated or is expired
                final String username = requestAuthParams.get(HttpDigest.PARAM_USERNAME);
                logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_EXPIRED, nonce, username == null ? "<unknown>" : username);
                sessions.invalidate( nonce );
                nonce = sessions.generate(assertion.getNonceTimeout(), assertion.getMaxNonceCount() );
                logAndAudit(AssertionMessages.HTTPDIGEST_NONCE_GENERATED, nonce);
            }
            return myChallengeParams( nonce );
        }
    }

    @Override
    protected String scheme() {
        return HttpDigest.SCHEME;
    }

    @Override
    protected LoginCredentials findCredentials( Message request, Map<String, String> authParams )
            throws IOException, CredentialFinderException
    {
        String authorization = request.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);

        if ( authorization == null || authorization.length() == 0 ) {
            return null;
        }

        if ( !populateAuthParams( authParams, authorization.trim() ) ) {
            logAndAudit(AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER);
            return null;
        }

        return doFindCredentials( request, authParams );
    }

    // - PACKAGE

    static boolean populateAuthParams( final Map<String, String> authParams,
                                       final String authorization ) throws CredentialFinderException {
        if ( OLD_PARSER ) {
            return popluateAuthParamsST( authParams, authorization );
        } else {
            return popluateAuthParamsHT( authParams, authorization );
        }
    }

    /**
     * Original parser using StringTokenizer, has bugs with quoted string parsing
     */
    static boolean popluateAuthParamsST( final Map<String, String> authParams,
                                         final String authorization ) throws CredentialFinderException  {
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
                        StringBuilder valueBuffer = new StringBuilder( value.substring( 1 ) );
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

                if ( !HttpDigest.SCHEME.equals(scheme) ) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Parse using HeaderTokenizer to fix issues with quoted string parsing
     */
    static boolean popluateAuthParamsHT( final Map<String, String> authParams,
                                         final String authorization ) throws CredentialFinderException  {
        final int schemeEnd = authorization.indexOf( ' ' );
        if ( schemeEnd > 0 ) {
            final String scheme = authorization.substring( 0, schemeEnd );
            if ( !HttpDigest.SCHEME.equals( scheme ) ) {
                return false;
            }
            authParams.put( HttpCredentialSourceAssertion.PARAM_SCHEME, HttpDigest.SCHEME );
        }

        final HeaderTokenizer ht = new HeaderTokenizer(authorization.substring( schemeEnd+1 ), ",\\\"\t =", false);
        try {
            while ( true ) {
                // name
                HeaderTokenizer.Token token = ht.next();
                if ( token.getType() == HeaderTokenizer.Token.EOF ) {
                    break;
                }
                expect( token, "name", HeaderTokenizer.Token.ATOM );
                final String name = token.getValue();

                // =
                consume(ht, "\t ");
                token = ht.next();
                expect( token, "equals", (int)'=' );
                consume(ht, "\t ");

                // value
                token = ht.next();
                if ( token.getType() == HeaderTokenizer.Token.QUOTEDSTRING ) {
                    authParams.put( name, token.getValue() );
                } else if ( isAtomOrAtomChar( token.getType() ) ) {
                    final StringBuilder valueBuilder = new StringBuilder();
                    while ( true ) {
                        valueBuilder.append( token.getValue() );

                        if ( isAtomOrAtomChar( ht.peek().getType() ) ) {
                            token = ht.next();
                        } else {
                            // consecutive peek() calls not supported, so we have to consume the next token
                            token = ht.next();
                            if ( token.getType()!=(int)' ' && token.getType()!=(int)'\t' && token.getType()!= (int)',' ) {
                                expect( token, "separator", (int)' ' ); //fail and throw
                            }
                            break;
                        }
                    }
                    authParams.put( name, valueBuilder.toString() );
                } else { //fail and throw
                    expect( token, "value", HeaderTokenizer.Token.ATOM );
                }

                // eat separator and whitespace
                consume( ht, ",\t " );
            }
        } catch ( ParseException e ) {
            throw new CredentialFinderException( "Error processing header: " + ExceptionUtils.getMessage( e ), AssertionStatus.BAD_REQUEST );
        }

        return true;
    }

    // - PRIVATE

    private static final boolean OLD_PARSER = SyspropUtil.getBoolean( "com.l7tech.server.policy.assertion.credential.http.oldDigestParser", false );

    private Map<String,String> myChallengeParams( String nonce ) {
        Map<String,String> params = new HashMap<String,String>();
        params.put( HttpDigest.PARAM_QOP, HttpDigest.QOP_AUTH );
        params.put( HttpDigest.PARAM_NONCE, nonce );
        params.put( HttpDigest.PARAM_OPAQUE, HexUtils.encodeMd5Digest( HexUtils.getMd5Digest( nonce.getBytes() ) ) );
        return params;
    }

    private static boolean isAtomOrAtomChar( final int tokenType ) {
        boolean atomOrAtomChar = false;

        if ( tokenType != HeaderTokenizer.Token.COMMENT &&
             tokenType != HeaderTokenizer.Token.EOF &&
             tokenType != HeaderTokenizer.Token.QUOTEDSTRING ) {
            if ( tokenType == HeaderTokenizer.Token.ATOM ) {
                atomOrAtomChar = true;
            } else if ( !Character.isISOControl(tokenType) && tokenType!=(int)' ' && tokenType!=(int)'\t' && tokenType!= (int)',' ) {
                atomOrAtomChar = true;
            }
        }

        return atomOrAtomChar;
    }

    private static void expect( final HeaderTokenizer.Token token,
                                final String description,
                                final int expectedType ) throws CredentialFinderException {
        if ( token.getType() != expectedType ) {
            throw new CredentialFinderException( "Error processing header, expected "+description+".", AssertionStatus.BAD_REQUEST );
        }
    }

    private static void consume( final HeaderTokenizer ht, final String characters ) throws ParseException {
        while ( true ) {
            final HeaderTokenizer.Token token = ht.peek();
            if ( characters.indexOf( token.getType() ) >=0 ) {
                ht.next();
            } else {
                break;
            }
        }
    }
}
