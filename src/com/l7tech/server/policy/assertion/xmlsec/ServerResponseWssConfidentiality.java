package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.token.SecurityContextToken;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.X509SecurityToken;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.AssertionMessages;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XML encryption on the soap response sent from the ssg server to the requestor (probably proxy).
 * <p/>
 * On the server side, this schedules decoration of a response with an xml encryption.
 * On the proxy side, this verifies that the Soap Response contains a valid xml encryption for the elements.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerResponseWssConfidentiality implements ServerAssertion {
    private SignerInfo signerInfo;

    public ServerResponseWssConfidentiality(ResponseWssConfidentiality data, ApplicationContext ctx) throws IOException {
        responseWssConfidentiality = data;
        KeystoreUtils ku = (KeystoreUtils)ctx.getBean("keystore");
        signerInfo = ku.getSslSignerInfo();
    }

    /**
     * despite the name of this method, i'm actually working on the response document here
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_REQUEST_NOT_SOAP);
                return AssertionStatus.BAD_REQUEST;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        if (!responseWssConfidentiality.getRecipientContext().localRecipient()) {
            X509Certificate clientCert = null;
            try {
            clientCert = CertUtils.decodeCert(HexUtils.decodeBase64(
                            responseWssConfidentiality.getRecipientContext().getBase64edX509Certificate(), true));
            } catch (CertificateException e) {
                String msg = "cannot retrieve the recipient cert";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
                throw new PolicyAssertionException(msg, e);
            }
            context.addDeferredAssertion(this, deferredDecoration(clientCert, null,
                                         responseWssConfidentiality.getRecipientContext()));
            return AssertionStatus.NONE;
        } else {
            ProcessorResult wssResult;
            try {
                wssResult = context.getRequest().getXmlKnob().getProcessorResult();
            } catch (SAXException e) {
                throw new CausedIOException(e);
            }

            if (wssResult == null) {
                auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_NO_WSS_SECURITY);
                context.setRequestPolicyViolated();
                return AssertionStatus.FAILED;
            }

            // Ecrypting the Response will require either the presence of a client cert (to encrypt the symmetric key)
            // or a SecureConversation in progress

            X509Certificate clientCert = null;
            SecurityContextToken secConvContext = null;
            SecurityToken[] tokens = wssResult.getSecurityTokens();
            for (int i = 0; i < tokens.length; i++) {
                SecurityToken token = tokens[i];
                if (token instanceof X509SecurityToken) {
                    X509SecurityToken x509token = (X509SecurityToken)token;
                    if (x509token.isPossessionProved()) {
                        if (clientCert != null) {
                            auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_MORE_THAN_ONE_TOKEN);
                            return AssertionStatus.BAD_REQUEST; // todo make multiple security tokens work
                        }
                        clientCert = x509token.asX509Certificate();
                    }
                } else if (token instanceof SamlSecurityToken) {
                    SamlSecurityToken samlToken = (SamlSecurityToken)token;
                    if (samlToken.isPossessionProved()) {
                        if (clientCert != null) {
                            auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_MORE_THAN_ONE_TOKEN);
                            return AssertionStatus.BAD_REQUEST; // todo make multiple security tokens work
                        }
                        clientCert = samlToken.getSubjectCertificate();
                    }
                } else if (token instanceof SecurityContextToken) {
                    SecurityContextToken secConvTok = (SecurityContextToken)token;
                    if (secConvTok.isPossessionProved()) {
                        secConvContext = secConvTok;
                    }
                }
            }

            if (clientCert == null && secConvContext == null) {
                auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_NO_CERT_OR_SC_TOKEN);
                context.setAuthenticationMissing(); // todo is it really, though?
                context.setRequestPolicyViolated();
                return AssertionStatus.FAILED; // todo verify that this return value is appropriate
            }

            context.addDeferredAssertion(this, deferredDecoration(clientCert, secConvContext, null));
            return AssertionStatus.NONE;
        }
    }

    private ServerAssertion deferredDecoration(final X509Certificate clientCert,
                                               final SecurityContextToken secConvTok, // todo what is secConvTok for?
                                               final XmlSecurityRecipientContext recipient) {
                                                                                        // answer fla -> it is for when the
                                                                                        // policy uses a secure conversation so that the
                                                                                        // response can be encrypted using that context
                                                                                        // instead of a client cert. this should be plugged in
                                                                                        // no idea why it is no longer there
        return new ServerAssertion() {
            public AssertionStatus checkRequest(PolicyEnforcementContext context)
                    throws IOException, PolicyAssertionException
            {
                Auditor auditor = new Auditor(context.getAuditContext(), logger);

                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_RESPONSE_NOT_SOAP);
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }

                // GET THE DOCUMENT
                Document soapmsg = null;
                try {
                    soapmsg = context.getResponse().getXmlKnob().getDocumentReadOnly();

                    final XpathExpression xpath = responseWssConfidentiality.getXpathExpression();
                    XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg,
                                                                           xpath.getNamespaces());
                    List selectedElements = null;
                    try {
                        selectedElements = evaluator.selectElements(xpath.getExpression());
                    } catch (JaxenException e) {
                        // this is thrown when there is an error in the expression
                        // this is therefore a bad policy
                        throw new PolicyAssertionException(e);
                    }

                    if (selectedElements == null || selectedElements.size() < 1) {
                        auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_RESPONSE_NOT_ENCRYPTED);
                        return AssertionStatus.NONE;
                    }
                    DecorationRequirements wssReq;
                    if (recipient != null) {
                        wssReq = context.getResponse().getXmlKnob().getAlternateDecorationRequirements(recipient);
                    } else {
                        wssReq = context.getResponse().getXmlKnob().getOrMakeDecorationRequirements();
                    }
                    wssReq.getElementsToEncrypt().addAll(selectedElements);

                    if (clientCert != null) {
                        wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                        wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
                        wssReq.setRecipientCertificate(clientCert);
                        wssReq.setSignTimestamp();
                    }

                    auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_CONF_RESPONSE_ENCRYPTED, new String[] {String.valueOf(selectedElements.size())});
                    return AssertionStatus.NONE;
                } catch (SAXException e) {
                    String msg = "cannot get an xml document from the response to encrypt";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
                    return AssertionStatus.SERVER_ERROR;
                } catch (CertificateException e) {
                    String msg = "cannot set the recipient cert.";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
                    return AssertionStatus.SERVER_ERROR;
                }
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private ResponseWssConfidentiality responseWssConfidentiality;
}
