/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfig;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author alex
 * @noinspection unchecked
 */
public abstract class ServerResponseWssSignature extends AbstractServerAssertion implements ServerAssertion {
    protected final SignerInfo signerInfo;
    protected final ResponseWssConfig wssConfig;
    protected final Auditor auditor;

    protected ServerResponseWssSignature(ResponseWssConfig responseWssAssertion, ApplicationContext spring, Logger logger) {
        super((Assertion)responseWssAssertion);
        this.auditor = new Auditor(this, spring, logger);
        this.wssConfig = responseWssAssertion;
        try {
            this.signerInfo = getSignerInfo(spring, responseWssAssertion);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get the SignerInfo (cert chain and private key) to use for the specified object.
     * If the object is an instance of PrivateKeyable that requests a specific private key, that private key will
     * be returned.  Otherwise the default private key will be returned.
     *
     * TODO move this somewhere more reasonable (com.l7tech.server.policy.assertion?)
     *
     * @param ctx  the Spring context.  Required.
     * @param maybePrivateKeyable  an Object that might be an instance of PrivateKeyable.  Optional.
     * @return The SslSignerInfo to use for the specified object.  Never null.
     * @throws KeyStoreException if there is a problem loading the requested cert chain and private key.
     */
    public static SignerInfo getSignerInfo(ApplicationContext ctx, Object maybePrivateKeyable) throws KeyStoreException {
        try {
            if (maybePrivateKeyable instanceof PrivateKeyable) {
                PrivateKeyable keyable = (PrivateKeyable)maybePrivateKeyable;
                if (!keyable.isUsesDefaultKeyStore()) {
                    final long keystoreId = keyable.getNonDefaultKeystoreId();
                    final String keyAlias = keyable.getKeyAlias();
                    com.l7tech.server.security.keystore.SsgKeyStoreManager sksm =
                            (SsgKeyStoreManager)ctx.getBean("ssgKeyStoreManager", com.l7tech.server.security.keystore.SsgKeyStoreManager.class);
                    SsgKeyEntry keyEntry = sksm.lookupKeyByKeyAlias(keyAlias, keystoreId);
                    X509Certificate[] certChain = keyEntry.getCertificateChain();
                    PrivateKey privateKey = keyEntry.getPrivateKey();
                    return new SignerInfo(privateKey, certChain);
                }
            }

            // Default keystore
            DefaultKey ku = (DefaultKey)ctx.getBean("defaultKey", DefaultKey.class);
            return ku.getSslInfo();
        } catch (IOException e) {
            throw new KeyStoreException("Can't read the keystore for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new KeyStoreException("Can't read the keystore for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException("Can't read the keystore for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        } catch (ObjectNotFoundException e) {
            throw new KeyStoreException("Can't find private key for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        }
    }

    // despite the name of this method, i'm actually working on the response document here
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
            auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_RESPONSE_NO_MATCHING_EL);
            return AssertionStatus.FALSIFIED;
        }

        // TODO need some way to guess whether sender would prefer we sign with our cert or with his
        //      EncryptedKey.  For now, we'll cheat, and use EncryptedKey if the request used any wse11
        //      elements that we noticed.
        if (wssResult != null && context.isResponseWss11()) {
            // Try to sign response using an existing EncryptedKey already known to the requestor,
            // using #EncryptedKeySHA1 KeyInfo reference, instead of making an RSA signature,
            // which is expensive.
            if (wssReq.getEncryptedKeySha1() == null || wssReq.getEncryptedKey() == null) {
                // No EncryptedKeySHA1 reference on response yet; create one
                XmlSecurityToken[] tokens = wssResult.getXmlSecurityTokens();
                for (XmlSecurityToken token : tokens) {
                    if (token instanceof EncryptedKey) {
                        // We'll just use the first one we see that's unwrapped
                        EncryptedKey ek = (EncryptedKey)token;
                        if (ek.isUnwrapped()) {
                            try {
                                wssReq.setEncryptedKey(ek.getSecretKey());
                                wssReq.setEncryptedKeySha1(ek.getEncryptedKeySHA1());
                            } catch ( InvalidDocumentFormatException e) {
                                throw new IllegalStateException(e); // Can't happen - it's unwrapped already
                            } catch (GeneralSecurityException e) {
                                throw new IllegalStateException(e); // Can't happen - it's unwrapped already
                            }
                            break;
                        }
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
        wssReq.setProtectTokens(wssConfig.isProtectTokens());
        if (keyReference == null || KeyReference.BST.getName().equals(keyReference)) {
            wssReq.setKeyInfoInclusionType(KeyInfoInclusionType.CERT);
        } else if (KeyReference.SKI.getName().equals(keyReference)) {
            wssReq.setKeyInfoInclusionType(KeyInfoInclusionType.STR_SKI);
        }

        auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_INT_RESPONSE_SIGNED, String.valueOf(howMany));

        return AssertionStatus.NONE;
    }

    /**
     * Configure the decoration requirements for this signature.
     *
     * @param context  the PolicyEnforcementContext.  Required.
     * @param soapmsg  the message that is to be decorated.  Required.
     * @param wssReq   the existing decoration requirements, to which the new signature requirements should be added.  Required.
     * @return the number of elements selected for signing, zero if no elements were selected, or -1 if the assertion
     *          should fail (subclass is expected to have logged the reason already)
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if the signature requirements cannot be added due to
     *                                                              a misconfigured assertion (for example, if it is
     *                                                              XPath based and the XPath is invalid)
     */
    protected abstract int addDecorationRequirements(PolicyEnforcementContext context, Document soapmsg, DecorationRequirements wssReq)
        throws PolicyAssertionException;
}
