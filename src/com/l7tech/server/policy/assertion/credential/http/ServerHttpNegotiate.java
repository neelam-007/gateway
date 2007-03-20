package com.l7tech.server.policy.assertion.credential.http;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Collections;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.kerberos.KerberosClient;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;

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

    //- PROTECTED

    protected Map challengeParams(Message request, Map authParams) {
        return Collections.EMPTY_MAP;
    }

    protected String scheme() {
        return ServerHttpNegotiate.SCHEME;
    }

    protected LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
        HttpRequestKnob httpRequestKnob = request.getHttpRequestKnob();
        String wwwAuthorize = httpRequestKnob.getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);
        Object connectionId = httpRequestKnob.getConnectionIdentifier();
        return findCredentials( wwwAuthorize, connectionId );
    }

    protected String realm() {
        return "";
    }

    protected AssertionStatus checkAuthParams(Map authParams) {
        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerHttpNegotiate.class.getName());
    private static final String SCHEME = "Negotiate";
    private final ThreadLocal connectionCredentials = new ThreadLocal(); // stores Object[] = id, LoginCredentials
    private final Auditor auditor;

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
                spn = KerberosClient.getKerberosAcceptPrincipal();
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

    private void setConnectionCredentials(Object id, LoginCredentials credentials) {
        connectionCredentials.set(new Object[]{id,credentials});
    }

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
}
