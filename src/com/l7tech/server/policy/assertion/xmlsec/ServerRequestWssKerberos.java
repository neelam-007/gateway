package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.security.kerberos.KerberosClient;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.security.kerberos.KerberosUtils;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.token.SecurityContextToken;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.SecurityContext;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.DuplicateSessionException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.security.auth.login.LoginException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Server side processing for Kerberos Binary Security Tokens.
 *
 * @author $Author$
 * @version $Version: $
 */
public class ServerRequestWssKerberos implements ServerAssertion {

    //- PUBLIC

    public ServerRequestWssKerberos(RequestWssKerberos requestWssKerberos, ApplicationContext springContext) {
        this.requestWssKerberos = requestWssKerberos;
        auditor = new Auditor(this, springContext, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_NON_SOAP);
                return AssertionStatus.BAD_REQUEST;
            }
            wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_NO_WSS_LEVEL_SECURITY);
            context.setRequestPolicyViolated();
            context.setAuthenticationMissing();
            return AssertionStatus.FALSIFIED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        if (tokens == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_NO_TOKEN);
            context.setAuthenticationMissing();
            return AssertionStatus.AUTH_REQUIRED;
        }

        KerberosGSSAPReqTicket kerberosTicket = null;
        SecureConversationSession kerberosSession = null;
        for (int i = 0; i < tokens.length; i++) {
            XmlSecurityToken tok = tokens[i];
            if (tok instanceof KerberosSecurityToken) {
                kerberosTicket = ((KerberosSecurityToken) tok).getTicket();
            }
            else if(tok instanceof SecurityContextToken) {
                SecurityContext securityContext = ((SecurityContextToken)tok).getSecurityContext();
                if(securityContext instanceof SecureConversationSession) {
                    kerberosSession = (SecureConversationSession) securityContext;
                }
                else {
                    logger.warning("Found security context of incorrect type '"+(securityContext==null ? "null" : securityContext.getClass().getName())+"'.");
                }
            }
        }

        if (kerberosSession !=null ) { // process reference to previously sent ticket
            LoginCredentials creds = kerberosSession.getCredentials();
            if(kerberosSession.getExpiration() < System.currentTimeMillis()) {
                logger.info("Ignoring expired Kerberos session.");
            }
            else {
                KerberosServiceTicket kerberosServiceTicket = (KerberosServiceTicket) creds.getPayload();
                if(!requestWssKerberos.getServicePrincipalName().equals(kerberosServiceTicket.getServicePrincipalName())) {
                    logger.info("Ignoring Kerberos session for another service ('"+requestWssKerberos.getServicePrincipalName()+"', '"+kerberosServiceTicket.getServicePrincipalName()+"').");
                }
                else {
                    context.setCredentials(creds);
                    context.setAuthenticated(true);

                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_GOT_SESSION, new String[] {kerberosServiceTicket.getClientPrincipalName()});

                    // Set up response to be able to sign and encrypt using a reference to this ticket
                    DecorationRequirements dreq = context.getResponse().getSecurityKnob().getAlternateDecorationRequirements(null);
                    dreq.setKerberosTicket(kerberosServiceTicket);
                    dreq.setKerberosTicketId(kerberosSession.getIdentifier());
                    dreq.setIncludeKerberosTicketId(true);

                    return AssertionStatus.NONE;
                }
            }
        }
        else if (kerberosTicket != null) { // process ticket
            try {
                KerberosClient client = new KerberosClient();
                KerberosServiceTicket kerberosServiceTicket = client.getKerberosServiceTicket(requestWssKerberos.getServicePrincipalName(), kerberosTicket);
                kerberosTicket.setServiceTicket(kerberosServiceTicket);
                LoginCredentials loginCreds = new LoginCredentials(kerberosServiceTicket.getClientPrincipalName(),
                                                            null,
                                                            CredentialFormat.KERBEROSTICKET,
                                                            KerberosSecurityToken.class,
                                                            null,
                                                            kerberosServiceTicket);
                context.setCredentials(loginCreds);
                context.setAuthenticated(true);

                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_GOT_TICKET, new String[] {kerberosServiceTicket.getClientPrincipalName()});

                // stash for later reference
                SecureConversationContextManager sccm = SecureConversationContextManager.getInstance();
                final String sessionIdentifier = KerberosUtils.getSessionIdentifier(kerberosTicket);
                try {
                    sccm.createContextForUser(sessionIdentifier, kerberosServiceTicket.getExpiry(), null, loginCreds, new SecretKeySpec(kerberosServiceTicket.getKey(), "l7 shared secret"));
                }
                catch(DuplicateSessionException dse) {
                    //can't happen since duplicate tickets are detected by kerberos.
                    logger.log(Level.SEVERE, "Duplicate session key error when creating kerberos session.", dse);
                }

                // Set up response to be able to sign and encrypt using a reference to this ticket
                DecorationRequirements dreq = context.getResponse().getSecurityKnob().getAlternateDecorationRequirements(null);
                dreq.setKerberosTicket(kerberosServiceTicket);
                dreq.setKerberosTicketId(sessionIdentifier);
                dreq.setIncludeKerberosTicketId(true);

                return AssertionStatus.NONE;
            }
            catch(KerberosException ke) {
                if(ke.getCause() instanceof LoginException) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_INVALID_CONFIG, null, ke);
                    return AssertionStatus.FAILED;
                }
                else {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_INVALID_TICKET, null, ke);
                    return AssertionStatus.AUTH_REQUIRED;
                }
            }
        }
        auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_NO_TICKET);
        context.setAuthenticationMissing();
        return AssertionStatus.AUTH_REQUIRED;
    }

    //- PRIVATE

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private RequestWssKerberos requestWssKerberos;
}
