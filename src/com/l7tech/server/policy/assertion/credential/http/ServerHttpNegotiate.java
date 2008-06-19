package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.security.kerberos.KerberosClient;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server implementation for Negotiate (Windows Integrated) Authentication
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerHttpNegotiate extends ServerHttpCredentialSource implements ServerAssertion {

    //- PUBLIC

    public ServerHttpNegotiate(HttpNegotiate data, ApplicationContext springContext) {
        super(data, springContext);
        this.auditor = new Auditor(this, springContext, logger);
    }

    @Override
    public AssertionStatus checkRequest( PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = super.checkRequest( context );

        if ( status == AssertionStatus.NONE ) {
            LoginCredentials creds = context.getLastCredentials();
            if ( creds != null && creds.getPayload() instanceof KerberosServiceTicket) {
                KerberosServiceTicket ticket = (KerberosServiceTicket) creds.getPayload();
                context.setVariable( "kerberos.realm", extractRealm(ticket.getClientPrincipalName()) );        
            }
        }

        return status;
    }

    //- PROTECTED

    @Override
    protected Map challengeParams(Message request, Map authParams) {
        return Collections.EMPTY_MAP;
    }

    @Override
    protected String scheme() {
        return ServerHttpNegotiate.SCHEME;
    }

    @Override
    protected LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
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
    private final ThreadLocal connectionCredentials = new ThreadLocal(); // stores Object[] = id, LoginCredentials
    private final Auditor auditor;

    @SuppressWarnings( { "RedundantArrayCreation" } )
    private LoginCredentials findCredentials( String wwwAuthorize, Object connectionId ) throws IOException {
        if ( wwwAuthorize == null || wwwAuthorize.length() == 0 ) {
            LoginCredentials loginCreds = getConnectionCredentials(connectionId);
            if (loginCreds != null) {
                logger.fine("Using connection credentials.");
            }
            else {
                logger.fine("No wwwAuthorize");
            }
            return loginCreds;
        }

        int spos = wwwAuthorize.indexOf(" ");
        if ( spos < 0 ) {
            logger.fine( "WWW-Authorize header contains no space; ignoring");
            return null;
        }

        String scheme = wwwAuthorize.substring( 0, spos );
        String base64 = wwwAuthorize.substring( spos + 1 );
        if ( !scheme().equals(scheme) ) {
            logger.fine( "WWW-Authorize scheme not Negotiate; ignoring");
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
            if (logger.isLoggable(Level.FINE)) {
                // then include the exception stack
                auditor.logAndAudit(AssertionMessages.HTTPNEGOTIATE_WARNING, new String[]{ke.getMessage()}, ke);
            } else {
                auditor.logAndAudit(AssertionMessages.HTTPNEGOTIATE_WARNING, new String[]{ke.getMessage()});
            }
            return null;
        }
    }

    @SuppressWarnings( { "unchecked" } )
    private void setConnectionCredentials(Object id, LoginCredentials credentials) {
        connectionCredentials.set(new Object[]{id,credentials});
    }

    @SuppressWarnings( { "unchecked" } )
    private LoginCredentials getConnectionCredentials(Object id) {
        LoginCredentials credentials = null;
        Object[] threadCachedCreds = (Object[]) connectionCredentials.get();

        if (threadCachedCreds != null) {
            if (threadCachedCreds[0].equals(id)) {
                credentials = (LoginCredentials) threadCachedCreds[1];
            }
            else {
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
