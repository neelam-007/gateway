/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.token.EncryptedKey;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.xml.KeyReference;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfig;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 */
abstract class ServerResponseWssSignature extends AbstractServerAssertion implements ServerAssertion {
    protected final SignerInfo signerInfo;
    protected final ResponseWssConfig wssConfig;
    protected final Auditor auditor;

    protected ServerResponseWssSignature(ResponseWssConfig responseWssAssertion, ApplicationContext spring, Logger logger) {
        super((Assertion)responseWssAssertion);
        this.auditor = new Auditor(this, spring, logger);
        this.wssConfig = responseWssAssertion;
        KeystoreUtils ku = (KeystoreUtils)spring.getBean("keystore");
        try {
            this.signerInfo = ku.getSslSignerInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            wssResult = context.getRequest().getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        final XmlSecurityRecipientContext recipient = wssConfig.getRecipientContext();

        context.addDeferredAssertion(this, new AbstractServerAssertion((Assertion)wssConfig) {
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
                Document soapmsg;
                final XmlKnob resXml;
                final SecurityKnob resSec;
                try {
                    resXml = context.getResponse().getXmlKnob();
                    resSec = context.getResponse().getSecurityKnob();
                    soapmsg = resXml.getDocumentReadOnly();
                } catch (SAXException e) {
                    String msg = "cannot get an xml document from the response to sign";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
                    return AssertionStatus.SERVER_ERROR;
                }


                DecorationRequirements wssReq = resSec.getAlternateDecorationRequirements(recipient);

                int howMany = addDecorationRequirements(context, soapmsg, wssReq);
                if (howMany < 0) {
                    return AssertionStatus.FAILED;
                } else if (howMany == 0) {
                    auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_RESPONSE_NOT_SIGNED);
                    return AssertionStatus.NONE;
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
                        XmlSecurityToken[] tokens = wssResult.getXmlSecurityTokens();
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

                if ((wssReq.getEncryptedKeySha1() == null || wssReq.getEncryptedKey() == null)
                  && wssReq.getKerberosTicket() == null) {
                    // No luck with #EncryptedKeySHA1 or Kerberos, so we'll have to do a full RSA signature using our own cert.
                    wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                    wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
                }


                // how was the keyreference requested?
                String keyReference = wssConfig.getKeyReference();

                if (keyReference == null || KeyReference.BST.getName().equals(keyReference)) {
                    wssReq.setSuppressBst(false);
                } else if (KeyReference.SKI.getName().equals(keyReference)) {
                    wssReq.setSuppressBst(true);
                }

                auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_RESPONSE_SIGNED, new String[] {String.valueOf(howMany)});

                return AssertionStatus.NONE;
            }

        });

        return AssertionStatus.NONE;
    }

    /**
     * @return the number of elements selected for signing, zero if no elements were selected, or -1 if the assertion 
     *          should fail (subclass is expected to have logged the reason already)
     */
    protected abstract int addDecorationRequirements(PolicyEnforcementContext context, Document soapmsg, DecorationRequirements wssReq)
        throws PolicyAssertionException;
}
