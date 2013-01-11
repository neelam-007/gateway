package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.kerberos.*;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import com.l7tech.security.token.http.HttpNegotiateToken;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Server implementation for Negotiate (Windows Integrated) Authentication
 */
public class ServerHttpNegotiate extends ServerHttpCredentialSource<HttpNegotiate> {

    private static final byte[] NTLM_MESSAGE_PREFIX =  new byte[]{'N', 'T', 'L', 'M', 'S', 'S', 'P', 0};
    public static final String NTLM_SCHEME = "NTLM";

    //- PUBLIC

    public ServerHttpNegotiate(HttpNegotiate data) {
        super(data);
   }

    @Override
    public AssertionStatus checkRequest( PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = super.checkRequest( context );

        if ( status == AssertionStatus.NONE ) {
            LoginCredentials creds = context.getDefaultAuthenticationContext().getLastCredentials();
            if ( creds != null && creds.getPayload() instanceof KerberosServiceTicket) {
                KerberosServiceTicket ticket = (KerberosServiceTicket) creds.getPayload();
                context.setVariable( HttpNegotiate.KERBEROS_REALM, extractRealm(ticket.getClientPrincipalName()) );
                if(null != ticket.getEncData()) {
                    context.setVariable(HttpNegotiate.KERBEROS_DATA, ticket.getEncData());
                }
            }
        }

        return status;
    }

    //- PROTECTED

    @Override
    protected Map<String, String> challengeParams(Message request, Map<String, String> authParams) {
        return Collections.emptyMap();
    }

    @Override
    protected String scheme() {
        return ServerHttpNegotiate.SCHEME;
    }

    @Override
    protected LoginCredentials findCredentials(Message request, Map<String, String> authParams) throws IOException, CredentialFinderException {
        HttpRequestKnob httpRequestKnob = request.getHttpRequestKnob();
        String wwwAuthorize = httpRequestKnob.getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);
        Object connectionId = httpRequestKnob.getConnectionIdentifier();
        return findCredentials( request, wwwAuthorize, connectionId, authParams );
    }

    @Override
    protected String realm() {
        return "";
    }

    @Override
    protected AssertionStatus checkAuthParams(Map authParams) {
        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private static final String SCHEME = "Negotiate";
    private final ThreadLocal<Pair<Object, LoginCredentials>> connectionCredentials = new ThreadLocal<Pair<Object, LoginCredentials>>(); // stores Object[] = id, LoginCredentials

    @SuppressWarnings({ "RedundantArrayCreation", "ThrowableResultOfMethodCallIgnored" })
    private LoginCredentials findCredentials( final Message request,
                                              final String wwwAuthorize,
                                              final Object connectionId,
                                              Map<String, String> authParams) throws IOException {
        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) {
            LoginCredentials loginCreds = getConnectionCredentials(connectionId);
            if (loginCreds != null) {
                logAndAudit(AssertionMessages.HTTPNEGOTIATE_USING_CONN_CREDS);
            } else {
                logAndAudit(AssertionMessages.HTTPCREDS_NO_AUTHN_HEADER);
            }
            return loginCreds;
        }

        int spos = wwwAuthorize.indexOf(" ");
        if ( spos < 0 ) {
            logAndAudit(AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER);
            return null;
        }

        String scheme = wwwAuthorize.substring( 0, spos );
        String base64 = wwwAuthorize.substring( spos + 1 );
        byte[] token = HexUtils.decodeBase64(base64, true);

        if ( !scheme().equals(scheme) || token.length == 0) {
            logAndAudit(AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER);
            //check if the client negotiated NTLM
            if(scheme.equals(NTLM_SCHEME)) {
                authParams.put(NTLM_SCHEME, base64);
                logAndAudit(AssertionMessages.HTTPNEGOTIATE_NTLM_AUTH);
            }
            return null;
        }
        //check if the client using NTLM as a part of negotiation
        else if (ArrayUtils.matchSubarrayOrPrefix(token, 0, 1, NTLM_MESSAGE_PREFIX, 0) > -1) {
            authParams.put(NTLM_SCHEME, base64);
            logAndAudit(AssertionMessages.HTTPNEGOTIATE_NTLM_AUTH);
            return null;
        }

        KerberosGSSAPReqTicket ticket = new KerberosGSSAPReqTicket(token);

        try {
            KerberosClient client = new KerberosClient();
            String spn;
            try {
                spn = KerberosClient.getKerberosAcceptPrincipal(request.getHttpRequestKnob().getRequestURL().getProtocol(),
                        request.getHttpRequestKnob().getRequestURL().getHost(), false);
            } catch (KerberosException ke) {// fallback to system property name
                spn = KerberosClient.getGSSServiceName();
            }
            KerberosServiceTicket kerberosServiceTicket =
                    client.getKerberosServiceTicket(spn, getClientInetAddress(request), ticket);

            LoginCredentials loginCreds = LoginCredentials.makeLoginCredentials( new HttpNegotiateToken(kerberosServiceTicket), assertion.getClass() );

            setConnectionCredentials(connectionId, loginCreds);

            return loginCreds;
        }
        catch(KerberosException ke) {
            logAndAudit(AssertionMessages.HTTPNEGOTIATE_WARNING, new String[]{ke.getMessage()}, ExceptionUtils.getDebugException(ke));
            return null;
        }
    }

    @Override
    protected void challenge(PolicyEnforcementContext context, Map<String, String> authParams) {
        //do not send Negotiate challenge if the client is using NTLM
        if(!authParams.containsKey(NTLM_SCHEME)){
            StringBuilder challengeHeader = new StringBuilder();
            String scheme = scheme();
            if (authParams.containsKey(scheme)) {
                challengeHeader.append(scheme);
                challengeHeader.append(" ");
                challengeHeader.append(authParams.get(scheme));
            }
            else{
                challengeHeader.append(scheme);
            }

            String challenge = challengeHeader.toString();
            if(challenge.length() > 0) {
                logAndAudit(AssertionMessages.HTTPCREDS_CHALLENGING, challenge);
                HttpResponseKnob httpResponse = context.getResponse().getHttpResponseKnob();
                httpResponse.addChallenge(challenge);
            }
        }
    }

    private void setConnectionCredentials(Object id, LoginCredentials credentials) {
        connectionCredentials.set(new Pair<Object, LoginCredentials>(id,credentials));
    }

    private LoginCredentials getConnectionCredentials(Object id) {
        LoginCredentials credentials = null;
        Pair<Object, LoginCredentials> threadCachedCreds = connectionCredentials.get();

        if (threadCachedCreds != null) {
            if (threadCachedCreds.left.equals(id)) {
                credentials = threadCachedCreds.right;
            } else {
                connectionCredentials.set(null); // reset
            }
        }

        return credentials;
    }

    private String extractRealm( final String principal ) {
        String realm = "";

        int index = principal.lastIndexOf( "@" );
        if ( index > -1 ) {
            realm = principal.substring( index + 1 );
        }

        return realm;
    }

    private InetAddress getClientInetAddress( final Message message ) {
        InetAddress clientAddress = null;

        TcpKnob tcpKnob = message.getKnob(TcpKnob.class);
        if ( tcpKnob != null ) {
            try {
                clientAddress = InetAddress.getByName( tcpKnob.getRemoteAddress() );
            } catch (UnknownHostException e) {
                logger.log( Level.INFO,
                        "Could not create address for remote IP '"+tcpKnob.getRemoteAddress()+"'.",
                        ExceptionUtils.getDebugException( e ));
            }
        }

        return clientAddress;
    }
}
