package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.token.X509SigningSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ArrayUtils;
import com.l7tech.message.Message;
import com.l7tech.common.io.CertUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

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
 */
public class ServerRequireWssX509Cert extends AbstractMessageTargetableServerAssertion<RequireWssX509Cert> {

    //- PUBLIC

    public ServerRequireWssX509Cert( final RequireWssX509Cert subject, final ApplicationContext springContext ) {
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
                return getBadMessageStatus();
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

        Element[] requiredSignatureElements = null;
        if ( assertion.getSignatureElementVariable() != null ) {
            requiredSignatureElements = new Element[0]; // fail if we don't match anything
            try {
                Object variable = context.getVariable( assertion.getSignatureElementVariable() );
                if ( variable instanceof Element ) {
                    requiredSignatureElements = new Element[]{ (Element) variable };
                } else if ( variable instanceof Element[] ) {
                    requiredSignatureElements = (Element[]) variable;                        
                }
            } catch (NoSuchVariableException e) {
                // so we won't find any creds
            }
        }

        Element processedSignatureElement = null;
        for (XmlSecurityToken tok : tokens) {
            if (tok instanceof X509SigningSecurityToken) {
                X509SigningSecurityToken x509Tok = (X509SigningSecurityToken)tok;
                if (x509Tok.isPossessionProved() && isTargetSignature(requiredSignatureElements,x509Tok)) {

                    // Check for a single signature element, not token or certificate (bug 7157)
                    final X509Certificate signingCertificate = x509Tok.getMessageSigningCertificate();
                    if ( processedSignatureElement != null &&
                         processedSignatureElement != x509Tok.getSignedElements()[0].getSignatureElement() &&
                         !assertion.isAllowMultipleSignatures() ) {
                        auditor.logAndAudit(AssertionMessages.WSS_X509_TOO_MANY_VALID_SIG, messageDesc);
                        return getBadMessageStatus();
                    }

                    processedSignatureElement = x509Tok.getSignedElements()[0].getSignatureElement();
                    String certCN = CertUtils.extractFirstCommonNameFromCertificate(signingCertificate);
                    authContext.addCredentials( LoginCredentials.makeLoginCredentials( x509Tok, assertion.getClass() ) );
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

    private static final Logger logger = Logger.getLogger(ServerRequireWssX509Cert.class.getName());

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

    /**
     * If particular signature(s) are desired, check that the token matches.
     */
    private boolean isTargetSignature( final Element[] targets, final X509SigningSecurityToken token ) {
        boolean match = false;

        if ( targets == null ) {
            match = true;
        } else {
            final SignedElement[] signedElements = token.getSignedElements();
            if ( signedElements.length > 0 ) {
                final Element signatureElement = signedElements[0].getSignatureElement();
                if ( signatureElement != null ) {
                    if ( ArrayUtils.contains( targets, signatureElement )) {
                        match = true;
                    } else {
                        // we'll have to try a slower DOM comparison
                        for ( Element targetElement : targets ) {
                            if ( signatureElement.isEqualNode( targetElement ) ) {
                                match = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return match;
    }
}
