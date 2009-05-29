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
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
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
public class ServerRequestWssX509Cert extends AbstractMessageTargetableServerAssertion<RequestWssX509Cert> {

    //- PUBLIC

    public ServerRequestWssX509Cert( final RequestWssX509Cert subject, final ApplicationContext springContext ) {
        super(subject, subject);
        this.auditor = new Auditor(this, springContext, logger);
        this.securityTokenResolver = (SecurityTokenResolver)springContext.getBean("securityTokenResolver", SecurityTokenResolver.class);
    }
    
    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        if (!assertion.getRecipientContext().localRecipient()) {
            auditor.logAndAudit(AssertionMessages.WSS_X509_FOR_ANOTHER_USER);
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDesc,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;
        try {
            if (!message.isSoap()) {
                auditor.logAndAudit(AssertionMessages.WSS_X509_NON_SOAP, messageDesc);
                return isRequest() ? AssertionStatus.BAD_REQUEST : AssertionStatus.FALSIFIED;
            }

            if ( isRequest() ) {
                wssResults = message.getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(message, messageDesc, securityTokenResolver, auditor);
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.WSS_X509_NO_WSS_LEVEL_SECURITY, messageDesc);
            if ( isRequest() ) {
                context.setRequestPolicyViolated();
                context.setAuthenticationMissing();
            }
            return AssertionStatus.FALSIFIED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        if (tokens == null) {
            AssertionStatus result = getBadAuthStatus( context );
            auditor.logAndAudit(AssertionMessages.WSS_X509_NO_TOKEN, messageDesc, result.getMessage());
            return result;
        }

        Element processedSignatureElement = null;
        for (XmlSecurityToken tok : tokens) {
            if (tok instanceof X509SigningSecurityToken) {
                X509SigningSecurityToken x509Tok = (X509SigningSecurityToken)tok;
                if (x509Tok.isPossessionProved()) {

                    // Check for a single signature element, not token or certificate (bug 7157)
                    final X509Certificate signingCertificate = x509Tok.getMessageSigningCertificate();
                    if ( processedSignatureElement != null &&
                         processedSignatureElement != x509Tok.getSignedElements()[0].getSignatureElement() &&
                         !assertion.isAllowMultipleSignatures() ) {
                        auditor.logAndAudit(AssertionMessages.WSS_X509_TOO_MANY_VALID_SIG, messageDesc);
                        return isRequest() ? AssertionStatus.BAD_REQUEST : AssertionStatus.FALSIFIED;
                    }

                    processedSignatureElement = x509Tok.getSignedElements()[0].getSignatureElement();
                    X500Name x500name = new X500Name(signingCertificate.getSubjectX500Principal().getName());
                    String certCN = x500name.getCommonName();
                    authContext.addCredentials(
                            new LoginCredentials(certCN,
                                                null,
                                                CredentialFormat.CLIENTCERT,
                                                assertion.getClass(),
                                                null,
                                                signingCertificate));
                    auditor.logAndAudit(AssertionMessages.WSS_X509_CERT_LOADED, certCN);
                }
            }
        }

        if ( processedSignatureElement == null ) {
            AssertionStatus result = getBadAuthStatus( context );
            auditor.logAndAudit(AssertionMessages.WSS_X509_NO_PROVEN_CERT, messageDesc, result.getMessage());
            return result;
        }

        return AssertionStatus.NONE;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerRequestWssX509Cert.class.getName());

    private final Auditor auditor;
    private final SecurityTokenResolver securityTokenResolver;

    private AssertionStatus getBadAuthStatus( final PolicyEnforcementContext context ) {
        AssertionStatus status;

        if ( isRequest() ) {
            status = AssertionStatus.AUTH_REQUIRED;
            context.setAuthenticationMissing();
        } else {
            status = AssertionStatus.FALSIFIED;
        }

        return status;
    }
}
