package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.security.token.X509SigningSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.CausedIOException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * This assertion verifies that the message contained an
 * xml digital signature but does not care about which elements
 * were signed. The cert used for the signature is
 * recorded in request.setPrincipalCredentials.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class ServerRequestWssX509Cert extends AbstractServerAssertion implements ServerAssertion {
    private final Auditor auditor;

    public ServerRequestWssX509Cert(RequestWssX509Cert subject, ApplicationContext springContext) {
        super(subject);
        this.subject = subject;
        this.auditor = new Auditor(this, springContext, logger);
    }
    
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!subject.getRecipientContext().localRecipient()) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_X509_FOR_ANOTHER_USER);
            return AssertionStatus.NONE;
        }
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_X509_NON_SOAP);
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
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_X509_NO_TOKEN);
            context.setAuthenticationMissing();
            return AssertionStatus.AUTH_REQUIRED;
        }
        X509Certificate gotACertAlready = null;
        for (XmlSecurityToken tok : tokens) {
            if (tok instanceof X509SigningSecurityToken) {
                X509SigningSecurityToken x509Tok = (X509SigningSecurityToken)tok;
                if (x509Tok.isPossessionProved()) {
                    X509Certificate okCert = x509Tok.getMessageSigningCertificate();
                    // todo, it is possible that a request has more than one signature by more than one
                    // identity. we should refactor request.setPrincipalCredentials to be able to remember
                    // more than one proven identity.
                    if (gotACertAlready != null) {
                        auditor.logAndAudit(AssertionMessages.REQUEST_WSS_X509_TOO_MANY_VALID_SIG);
                        return AssertionStatus.BAD_REQUEST;
                    }
                    gotACertAlready = okCert;
                }
            }
        }
        if (gotACertAlready != null) {
            X500Name x500name = new X500Name(gotACertAlready.getSubjectX500Principal().getName());
            String certCN = x500name.getCommonName();
            context.addCredentials(new LoginCredentials(certCN,
                                                        null,
                                                        CredentialFormat.CLIENTCERT,
                                                        subject.getClass(),
                                                        null,
                                                        gotACertAlready));
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_X509_CERT_LOADED, certCN);
            return AssertionStatus.NONE;
        }
        auditor.logAndAudit(AssertionMessages.REQUEST_WSS_X509_NO_PROVEN_CERT);
        context.setAuthenticationMissing();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private static final Logger logger = Logger.getLogger(ServerRequestWssX509Cert.class.getName());
    private RequestWssX509Cert subject;
}
