package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.token.EncryptedKey;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.KeyReference;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.jaxen.JaxenException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). Also does
 * XML Encryption of the response's body if the assertion's property dictates it.
 * <p/>
 * On the server side, this schedules decoration of a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerResponseWssIntegrity implements ServerAssertion {
    private SignerInfo signerInfo;
    private final Auditor auditor;

    public ServerResponseWssIntegrity(ResponseWssIntegrity data, ApplicationContext ctx) throws IOException {
        responseWssIntegrity = data;
        KeystoreUtils ku = (KeystoreUtils)ctx.getBean("keystore");
        signerInfo = ku.getSslSignerInfo();
        auditor = new Auditor(this, ctx, logger);
    }

    /**
     * despite the name of this method, i'm actually working on the response document here
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        final ProcessorResult wssResult;
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResult = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        final XmlSecurityRecipientContext recipient = responseWssIntegrity.getRecipientContext();

        context.addDeferredAssertion(this, new ServerAssertion() {
            public AssertionStatus checkRequest(PolicyEnforcementContext context)
                    throws IOException, PolicyAssertionException
            {
                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_RESPONSE_NOT_SOAP);
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }


                // GET THE DOCUMENT
                Document soapmsg = null;
                final XmlKnob resXml;
                try {
                    resXml = context.getResponse().getXmlKnob();
                    soapmsg = resXml.getDocumentReadOnly();
                } catch (SAXException e) {
                    String msg = "cannot get an xml document from the response to sign";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
                    return AssertionStatus.SERVER_ERROR;
                }

                XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg,
                                                                       responseWssIntegrity.getXpathExpression().getNamespaces());
                List selectedElements = null;
                try {
                    selectedElements = evaluator.selectElements(responseWssIntegrity.getXpathExpression().getExpression());
                } catch (JaxenException e) {
                    // this is thrown when there is an error in the expression
                    // this is therefore a bad policy
                    throw new PolicyAssertionException(e);
                }

                if (selectedElements == null || selectedElements.size() < 1) {
                    auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_RESPONSE_NOT_SIGNED);
                    return AssertionStatus.NONE;
                }

                DecorationRequirements wssReq = null;
                try {
                    if (!recipient.localRecipient()) {
                        wssReq = resXml.getAlternateDecorationRequirements(recipient);
                    } else {
                        wssReq = resXml.getOrMakeDecorationRequirements();
                    }
                } catch (CertificateException e) {
                    String msg = "cannot set the recipient cert.";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
                    return AssertionStatus.SERVER_ERROR;
                }

                // TODO need some way to guess whether sender would prefer we sign with our cert or with his
                //      EncryptedKey.  For now, we'll cheat, and use EncryptedKey if the request used any wse11
                //      elements that we noticed.
                if (wssResult != null && wssResult.isWsse11Seen()) {
                    // Try to sign response using an existing EncryptedKey already known to the requestor,
                    // using #EncryptedKeySHA1 KeyInfo reference, instead of making an RSA signature,
                    // which is expensive.
                    if (wssReq.getEncryptedKeySha1() == null || wssReq.getEncryptedKey() == null) {
                        // No EncryptedKeySHA1 reference on response yet; create one
                        SecurityToken[] tokens = wssResult.getSecurityTokens();
                        for (int i = 0; i < tokens.length; i++) {
                            SecurityToken token = tokens[i];
                            if (token instanceof EncryptedKey) {
                                // We'll just use the first one we see
                                EncryptedKey ek = (EncryptedKey)token;
                                wssReq.setEncryptedKey(ek.getSecretKey());
                                wssReq.setEncryptedKeySha1(ek.getEncryptedKeySHA1());
                                break;
                            }
                        }
                    }
                }

                if (wssReq.getEncryptedKeySha1() == null || wssReq.getEncryptedKey() == null) {
                    // No luck with #EncryptedKeySHA1, so we'll have to do a full RSA signature using our own cert.
                    wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                    wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
                }

                wssReq.getElementsToSign().addAll(selectedElements);
                wssReq.setSignTimestamp();

                // how was the keyreference requested?
                String keyReference = responseWssIntegrity.getKeyReference();

                if (keyReference == null || KeyReference.BST.getName().equals(keyReference)) {
                    wssReq.setSuppressBst(false);
                } else if (KeyReference.SKI.getName().equals(keyReference)) {
                    wssReq.setSuppressBst(true);
                }

                auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_RESPONSE_SIGNED, new String[] {String.valueOf(selectedElements.size())});

                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private ResponseWssIntegrity responseWssIntegrity;
}
