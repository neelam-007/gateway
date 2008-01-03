package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.security.kerberos.KerberosUtils;
import com.l7tech.common.security.kerberos.KerberosClient;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityContextToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.SecurityContext;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.secureconversation.DuplicateSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side processing for Kerberos Binary Security Tokens.
 *
 * @author $Author$
 * @version $Version: $
 */
public class ServerRequestWssKerberos extends AbstractServerAssertion implements ServerAssertion {

    //- PUBLIC

    public ServerRequestWssKerberos(RequestWssKerberos requestWssKerberos, ApplicationContext springContext) {
        super(requestWssKerberos);
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
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY);
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
                String configSpn = requestWssKerberos.getServicePrincipalName();
                if (configSpn == null) {
                    try {
                        configSpn = KerberosUtils.toGssName(KerberosClient.getKerberosAcceptPrincipal());
                    }
                    catch(KerberosException ke) {
                        // fallback to system property name
                    }
                    if (configSpn == null) {
                        configSpn = KerberosClient.getGSSServiceName();
                    }
                }

                if(!configSpn.equals(kerberosServiceTicket.getServicePrincipalName())) {
                    logger.info("Ignoring Kerberos session for another service ('"+requestWssKerberos.getServicePrincipalName()+"', '"+kerberosServiceTicket.getServicePrincipalName()+"').");
                }
                else {
                    context.addCredentials(creds);

                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_GOT_SESSION, new String[] {kerberosServiceTicket.getClientPrincipalName()});

                    // Set up response to be able to sign and encrypt using a reference to this ticket
                    addDeferredAssertion(context, kerberosServiceTicket);

                    return AssertionStatus.NONE;
                }
            }
        }
        else if (kerberosTicket != null) { // process ticket
            KerberosServiceTicket kerberosServiceTicket = kerberosTicket.getServiceTicket();
            assert kerberosServiceTicket!=null;
            LoginCredentials loginCreds = new LoginCredentials(
                                                        null,
                                                        null,
                                                        CredentialFormat.KERBEROSTICKET,
                                                        RequestWssKerberos.class,
                                                        null,
                                                        kerberosServiceTicket);
            context.addCredentials(loginCreds);

            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_GOT_TICKET, new String[] {kerberosServiceTicket.getClientPrincipalName()});

            // stash for later reference
            SecureConversationContextManager sccm = SecureConversationContextManager.getInstance();
            final String sessionIdentifier = KerberosUtils.getSessionIdentifier(kerberosTicket);
            try {
                sccm.createContextForUser(sessionIdentifier, kerberosServiceTicket.getExpiry(), null, loginCreds, kerberosServiceTicket.getKey());
            }
            catch(DuplicateSessionException dse) {
                //can't happen since duplicate tickets are detected by kerberos.
                logger.log(Level.SEVERE, "Duplicate session key error when creating kerberos session.", dse);
            }

            // Set up response to be able to sign and encrypt using a reference to this ticket
            addDeferredAssertion(context, kerberosServiceTicket);

            return AssertionStatus.NONE;
        }
        auditor.logAndAudit(AssertionMessages.REQUEST_WSS_KERBEROS_NO_TICKET);
        context.setAuthenticationMissing();
        return AssertionStatus.AUTH_REQUIRED;
    }

    //- PRIVATE

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    private RequestWssKerberos requestWssKerberos;

    private void addDeferredAssertion(PolicyEnforcementContext context, final KerberosServiceTicket kerberosServiceTicket) {
        context.addDeferredAssertion(this, new AbstractServerAssertion(requestWssKerberos) {
            public AssertionStatus checkRequest(PolicyEnforcementContext context)
                    throws IOException, PolicyAssertionException
            {
                DecorationRequirements dreq = context.getResponse().getSecurityKnob().getAlternateDecorationRequirements(null);
                dreq.setKerberosTicket(kerberosServiceTicket);

                return AssertionStatus.NONE;
            }
        });
    }
}
