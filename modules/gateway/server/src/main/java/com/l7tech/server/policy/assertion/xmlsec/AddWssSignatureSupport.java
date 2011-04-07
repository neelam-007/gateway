package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditHaver;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.KeyInfoDetails;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

/**
 * Support class for assertion that configure WSS signature decoration requirements.
 */
public class AddWssSignatureSupport implements AuditHaver {
    private final AuditHaver auditHaver;
    private final SignerInfo signerInfo;
    private final boolean isPreferred;
    private final WssDecorationConfig wssConfig;
    private final boolean failIfNotSigning;
    private boolean isResponse;

    public AddWssSignatureSupport(AuditHaver auditHaver, WssDecorationConfig wssConfig, BeanFactory beanFactory, boolean failIfNotSigning, boolean isResponse) {
        this.auditHaver = auditHaver;
        this.wssConfig = wssConfig;
        this.failIfNotSigning = failIfNotSigning;
        this.isResponse = isResponse;

        try {
            this.signerInfo = ServerAssertionUtils.getSignerInfo(beanFactory, wssConfig);
            this.isPreferred = ServerAssertionUtils.isPreferredSigner( wssConfig );
        } catch (KeyStoreException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Audit getAuditor() {
        return auditHaver.getAuditor();
    }

    public SignerInfo getSignerInfo() {
        return signerInfo;
    }

    /**
     * Apply signature decoration requirements to a target message, and return a recommended AssertionStatus.
     *
     * @param context the PEC.  Required.
     * @param message the target message.  Required.
     * @param messageDescription the target message description.  Required.
     * @param authContext the authentication context.  Required.
     * @param signedElementSelector a {@link com.l7tech.server.policy.assertion.xmlsec.AddWssSignatureSupport.SignedElementSelector} that will add elementsToSign to the decoration requriements.  Required.
     * @return AssertionStatus.NONE upon success; NOT_APPLICABLE if target message isn't SOAP; FALSIFIED if failIfNotSigning and no elements are selected for signing; SERVER_ERROR on XML Parsing error.
     *         In all cases, any required auditing will already have been done.
     * @throws IOException if there is an error reading the target message
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if addDecorationRequirements indicates a misconfigured assertion
     */
    public AssertionStatus applySignatureDecorationRequirements(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, SignedElementSelector signedElementSelector)
            throws IOException, PolicyAssertionException {
        Audit auditor = auditHaver.getAuditor();

        ProcessorResult processorResult = null;
        if ( isResponse ) {
            try {
                if (context.getRequest().isSoap()) {
                    processorResult = context.getRequest().getSecurityKnob().getProcessorResult();
                }
            } catch (SAXException e) {
                throw new CausedIOException(e);
            }
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
            String msg = "cannot get an xml document from the " + messageDescription + " to sign";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, e);
            return AssertionStatus.SERVER_ERROR;
        }


        final DecorationRequirements wssReq = securityKnob.getAlternateDecorationRequirements(recipient);

        int howMany = signedElementSelector.selectElementsToSign(context, authContext, soapmsg, wssReq, message);
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
            if (wssReq.getEncryptedKeyReferenceInfo() == null || wssReq.getEncryptedKey() == null) {
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
                                    wssReq.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeEncryptedKeySha1Ref(ek.getEncryptedKeySHA1()));
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

        try {
            if ( isPreferred ) {
                if (wssReq.getPreferredSigningTokenType() == null)
                    wssReq.setPreferredSigningTokenType(DecorationRequirements.PreferredSigningTokenType.X509);
                wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
            } else if ((wssReq.getEncryptedKeyReferenceInfo() == null || wssReq.getEncryptedKey() == null)
                    && wssReq.getKerberosTicket() == null) {
                // No luck with #EncryptedKeySHA1 or Kerberos, so we'll have to do a full RSA signature using our own cert.
                wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
            }
        } catch (UnrecoverableKeyException e) {
            String msg = "Unable to access configured private key: " + ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        // how was the keyreference requested?
        String keyReference = wssConfig.getKeyReference();
        if (wssConfig.isUsingProtectTokens())
            wssReq.setProtectTokens(wssConfig.isProtectTokens());
        if (keyReference == null || KeyReference.BST.getName().equals(keyReference)) {
            wssReq.setKeyInfoInclusionType(KeyInfoInclusionType.CERT);
        } else if (KeyReference.SKI.getName().equals(keyReference)) {
            wssReq.setKeyInfoInclusionType(KeyInfoInclusionType.STR_SKI);
        } else if (KeyReference.ISSUER_SERIAL.getName().equals(keyReference)) {
            wssReq.setKeyInfoInclusionType(KeyInfoInclusionType.ISSUER_SERIAL);
        }

        // Was digest algorithm overridden?
        String digestAlgorithm = wssConfig.getDigestAlgorithmName();
        if (digestAlgorithm != null) {
            wssReq.setSignatureMessageDigest(digestAlgorithm);
        }

        auditor.logAndAudit(AssertionMessages.ADD_WSS_SIGNATURE_MESSAGE_SIGNED, messageDescription, String.valueOf(howMany));

        return AssertionStatus.NONE;
    }

    public static interface SignedElementSelector {
        /**
         * Configure the decoration requirements for this signature, adding any needed elementsToSign.
         * <p/>
         * This class provides the functionality to correctly apply these decoration requirements to the target message.
         * Some subclasses may want this functionality but not in all cases depending on the assertions configuration.
         * For those cases the logic that would normally go into checkRequest() can go into this method, following the
         * contract for return codes and logging, and use hasDecorationRequirements() as the means for signaling when the
         * decoration requirements are actually needed.
         *
         * @param context  the PolicyEnforcementContext.  Required.
         * @param authContext  the AuthenticationContext.  Required.
         * @param soapmsg  the message that is to be decorated. This Document is NOT WRITEABLE. If written to the changes will
         * be persisted so long as decoration requirements are added and hasDecorationRequirements() returns true. Required.
         * @param wssReq   the existing decoration requirements, to which the new signature requirements should be added.  Required.
         * @param targetMessage the target message for this assertion. Never null. Allows a writeable document to be obtained.
         * @return the number of elements selected for signing, zero if no elements were selected, or -1 if the assertion
         *          should fail (subclass is expected to have logged the reason already)
         * @throws com.l7tech.policy.assertion.PolicyAssertionException if the signature requirements cannot be added due to
         *                                                              a misconfigured assertion (for example, if it is
         *                                                              XPath based and the XPath is invalid)
         */
        int selectElementsToSign(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq, Message targetMessage)
                throws PolicyAssertionException;
    }
}
