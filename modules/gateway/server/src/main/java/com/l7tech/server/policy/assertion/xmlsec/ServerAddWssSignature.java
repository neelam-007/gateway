/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.Message;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.util.logging.Logger;

/**
 * Server assertion for Signing
 *
 * @author alex
 */
public abstract class ServerAddWssSignature<AT extends Assertion> extends AbstractMessageTargetableServerAssertion<AT> {
    protected final SignerInfo signerInfo;
    protected final WssDecorationConfig wssConfig;
    protected final Auditor auditor;
    protected final boolean failIfNotSigning;

    protected ServerAddWssSignature( final AT assertion,
                                     final WssDecorationConfig responseWssAssertion,
                                     final MessageTargetable messageTargetable,
                                     final ApplicationContext spring,
                                     final Logger logger,
                                     final boolean failIfNotSigning ) {
        super(assertion, messageTargetable);
        this.auditor = new Auditor(this, spring, logger);
        this.wssConfig = responseWssAssertion;
        this.failIfNotSigning = failIfNotSigning;
        try {
            this.signerInfo = ServerAssertionUtils.getSignerInfo(spring, responseWssAssertion);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // despite the name of this method, i'm actually working on the response document here
    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        final ProcessorResult processorResult;
        if ( isResponse() ) {
            try {
                if (!context.getRequest().isSoap()) {
                    auditor.logAndAudit(AssertionMessages.ADD_WSS_SIGNATURE_REQUEST_NOT_SOAP);
                    return AssertionStatus.NOT_APPLICABLE;
                }
                processorResult = context.getRequest().getSecurityKnob().getProcessorResult();
            } catch (SAXException e) {
                throw new CausedIOException(e);
            }
        } else {
            processorResult = null;   
        }

        final XmlSecurityRecipientContext recipient = wssConfig.getRecipientContext();

        try {
            if (!message.isSoap()) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_SIGNATURE_MESSAGE_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }


        // GET THE DOCUMENT
        Document soapmsg;
        final XmlKnob xmlKnob;
        final SecurityKnob securityKnob;
        try {
            xmlKnob = message.getXmlKnob();
            securityKnob = message.getSecurityKnob();
            soapmsg = xmlKnob.getDocumentReadOnly();
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to sign";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
            return AssertionStatus.SERVER_ERROR;
        }


        DecorationRequirements wssReq = securityKnob.getAlternateDecorationRequirements(recipient);

        int howMany = addDecorationRequirements(context, authContext, soapmsg, wssReq);
        if (howMany < 0) {
            return AssertionStatus.FAILED;
        } else if (howMany == 0) {
            if ( failIfNotSigning ) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_SIGNATURE_MESSAGE_NO_MATCHING_EL, messageDescription);
                return AssertionStatus.FALSIFIED;
            } else {
                return AssertionStatus.NONE;
            }
        }

        // TODO need some way to guess whether sender would prefer we sign with our cert or with his
        //      EncryptedKey.  For now, we'll cheat, and use EncryptedKey if the request used any wse11
        //      elements that we noticed.
        if (processorResult != null && context.isResponseWss11()) {
            // Try to sign response using an existing EncryptedKey already known to the requestor,
            // using #EncryptedKeySHA1 KeyInfo reference, instead of making an RSA signature,
            // which is expensive.
            if (wssReq.getEncryptedKeySha1() == null || wssReq.getEncryptedKey() == null) {
                // No EncryptedKeySHA1 reference on response yet; create one
                for ( LoginCredentials credentials : context.getAuthenticationContext( context.getRequest() ).getCredentials() ) {
                    SecurityToken[] tokens = credentials.getSecurityTokens();
                    for ( SecurityToken token : tokens ) {
                        if (token instanceof EncryptedKey && credentials.isSecurityTokenPresent( token ) ) {
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
        } else if (KeyReference.ISSUER_SERIAL.getName().equals(keyReference)) {
            wssReq.setKeyInfoInclusionType(KeyInfoInclusionType.ISSUER_SERIAL);
        }

        auditor.logAndAudit(AssertionMessages.ADD_WSS_SIGNATURE_MESSAGE_SIGNED, messageDescription, String.valueOf(howMany));

        return AssertionStatus.NONE;
    }

    /**
     * Configure the decoration requirements for this signature.
     *
     * @param context  the PolicyEnforcementContext.  Required.
     * @param authContext  the AuthenticationContext.  Required.
     * @param soapmsg  the message that is to be decorated.  Required.
     * @param wssReq   the existing decoration requirements, to which the new signature requirements should be added.  Required.
     * @return the number of elements selected for signing, zero if no elements were selected, or -1 if the assertion
     *          should fail (subclass is expected to have logged the reason already)
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if the signature requirements cannot be added due to
     *                                                              a misconfigured assertion (for example, if it is
     *                                                              XPath based and the XPath is invalid)
     */
    protected abstract int addDecorationRequirements(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq)
        throws PolicyAssertionException;

    @Override
    public Auditor getAuditor() {
        return auditor;
    }
}
