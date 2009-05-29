package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Server implementation for Negotiate (Windows Integrated) Authentication
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerHttpNegotiate extends ServerHttpCredentialSource<HttpNegotiate> {

    //- PUBLIC

    public ServerHttpNegotiate(HttpNegotiate data, ApplicationContext springContext) {
        super(data, springContext);
        this.auditor = new Auditor(this, springContext, logger);
    }

    @Override
    public AssertionStatus checkRequest( PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = super.checkRequest( context );

        if ( status == AssertionStatus.NONE ) {
            LoginCredentials creds = context.getDefaultAuthenticationContext().getLastCredentials();
            if ( creds != null && creds.getPayload() instanceof KerberosServiceTicket) {
                KerberosServiceTicket ticket = (KerberosServiceTicket) creds.getPayload();
                context.setVariable( "kerberos.realm", extractRealm(ticket.getClientPrincipalName()) );        
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
        return findCredentials( wwwAuthorize, connectionId );
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

    private static final Logger logger = Logger.getLogger(ServerHttpNegotiate.class.getName());
    private static final String SCHEME = "Negotiate";
    private final ThreadLocal<Pair<Object, LoginCredentials>> connectionCredentials = new ThreadLocal<Pair<Object, LoginCredentials>>(); // stores Object[] = id, LoginCredentials
    private final Auditor auditor;

    @SuppressWarnings({ "RedundantArrayCreation", "ThrowableResultOfMethodCallIgnored" })
    private LoginCredentials findCredentials( String wwwAuthorize, Object connectionId ) throws IOException {
        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) {
            LoginCredentials loginCreds = getConnectionCredentials(connectionId);
            if (loginCreds != null) {
                auditor.logAndAudit(AssertionMessages.HTTPNEGOTIATE_USING_CONN_CREDS);
            } else {
                auditor.logAndAudit(AssertionMessages.HTTPCREDS_NO_AUTHN_HEADER);
            }
            return loginCreds;
        }

        int spos = wwwAuthorize.indexOf(" ");
        if ( spos < 0 ) {
            auditor.logAndAudit(AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER);
            return null;
        }

        String scheme = wwwAuthorize.substring( 0, spos );
        String base64 = wwwAuthorize.substring( spos + 1 );
        if ( !scheme().equals(scheme) ) {
            auditor.logAndAudit(AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER);
            return null;
        }

        byte[] token = HexUtils.decodeBase64( base64, true );
        KerberosGSSAPReqTicket ticket = new KerberosGSSAPReqTicket(token);

        try {
            KerberosClient client = new KerberosClient();
            String spn;
            try {
                spn = KerberosClient.getKerberosAcceptPrincipal(false);
            }
            catch(KerberosException ke) { // fallback to system property name
                spn = KerberosClient.getGSSServiceName();
            }
            KerberosServiceTicket kerberosServiceTicket = client.getKerberosServiceTicket(spn, ticket);
            ticket.setServiceTicket(kerberosServiceTicket);

            LoginCredentials loginCreds = new LoginCredentials(
                                                        null,
                                                        null,
                                                        CredentialFormat.KERBEROSTICKET,
                                                        HttpNegotiate.class,
                                                        null,
                                                        kerberosServiceTicket);

            setConnectionCredentials(connectionId, loginCreds);

            return loginCreds;
        }
        catch(KerberosException ke) {
            auditor.logAndAudit(AssertionMessages.HTTPNEGOTIATE_WARNING, new String[]{ke.getMessage()}, ExceptionUtils.getDebugException(ke));
            return null;
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
}
