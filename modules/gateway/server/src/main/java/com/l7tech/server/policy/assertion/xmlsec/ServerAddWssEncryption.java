package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.X509SigningSecurityToken;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.gateway.common.audit.AssertionMessages;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support class for server assertions that perform encryption.
 */
public abstract class ServerAddWssEncryption<AT extends Assertion> extends AbstractMessageTargetableServerAssertion<AT> {

    //- PUBLIC

    public ServerAddWssEncryption( final AT assertion,
                                   final SecurityHeaderAddressable securityHeaderAddressable,
                                   final MessageTargetable messageTargetable,
                                   final Logger logger ) {
        super( assertion, messageTargetable );
        this.securityHeaderAddressable = securityHeaderAddressable;

        X509Certificate rccert = null;
        if (!securityHeaderAddressable.getRecipientContext().localRecipient()) {
            try {
                rccert = CertUtils.decodeCert( HexUtils.decodeBase64(
                        securityHeaderAddressable.getRecipientContext().getBase64edX509Certificate(), true));
            } catch (CertificateException e) {
                logger.log( Level.WARNING, "Assertion will always fail: recipient cert cannot be decoded: " + e.getMessage(), e);
                rccert = null;
            }
        }
        recipientContextCert = rccert;
    }

    //- PROTECTED

    protected final X509Certificate recipientContextCert;
    protected final SecurityHeaderAddressable securityHeaderAddressable;

    protected EncryptionContext buildEncryptionContext( final PolicyEnforcementContext context )
            throws PolicyAssertionException, MultipleTokensException {
        X509Certificate clientCert = null;
        KerberosServiceTicket kerberosServiceTicket = null;
        SecurityContextToken secConvContext = null;
        EncryptedKey encryptedKey = null;
        String keyEncryptionAlgorithm = null;
        XmlSecurityRecipientContext recipientContext = null;

        if (!securityHeaderAddressable.getRecipientContext().localRecipient()) {
            if (recipientContextCert == null) {
                String msg = "cannot retrieve the recipient cert";
                getAuditor().logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
                throw new PolicyAssertionException(assertion, msg);
            }
            clientCert = recipientContextCert;
            recipientContext = securityHeaderAddressable.getRecipientContext();
        } else if ( isResponse() ) {
            final ProcessorResult wssResult = context.getRequest().getSecurityKnob().getProcessorResult();
            final XmlSecurityToken[] tokens;
            if ( wssResult != null ) {
                tokens = wssResult.getXmlSecurityTokens();
            } else {
                tokens = new XmlSecurityToken[0];
            }

            // Ecrypting the Response will require either the presence of a client cert (to encrypt the symmetric key)
            // or a SecureConversation in progress or an Encrypted Key or Kerberos Session
            for (XmlSecurityToken token : tokens) {
                if (token instanceof SamlSecurityToken ) {
                    SamlSecurityToken samlToken = (SamlSecurityToken)token;
                    if (samlToken.isPossessionProved()) {
                        if (clientCert != null) {
                            throw new MultipleTokensException();
                        }
                        clientCert = samlToken.getSubjectCertificate();
                    }
                } else if (token instanceof X509SigningSecurityToken ) {
                    X509SigningSecurityToken x509token = (X509SigningSecurityToken)token;
                    if (x509token.isPossessionProved()) {
                        if (clientCert != null) {
                            throw new MultipleTokensException();
                        }
                        clientCert = x509token.getMessageSigningCertificate();
                        keyEncryptionAlgorithm = wssResult.getLastKeyEncryptionAlgorithm();
                    }
                } else if (token instanceof KerberosSigningSecurityToken ) {
                    KerberosSigningSecurityToken kerberosSecurityToken = (KerberosSigningSecurityToken)token;
                    if (kerberosServiceTicket != null) {
                        throw new MultipleTokensException();
                    }
                    kerberosServiceTicket = kerberosSecurityToken.getServiceTicket();
                } else if (token instanceof SecurityContextToken) {
                    SecurityContextToken secConvTok = (SecurityContextToken)token;
                    if (secConvTok.isPossessionProved()) {
                        secConvContext = secConvTok;
                    }
                } else if (token instanceof EncryptedKey) {
                    if (encryptedKey != null) {
                        throw new MultipleTokensException();
                    }
                    encryptedKey = (EncryptedKey)token;
                }
            }
        }

        return new EncryptionContext(
                clientCert,
                kerberosServiceTicket,
                secConvContext,
                encryptedKey,
                keyEncryptionAlgorithm,
                recipientContext );
    }

    protected void applyDecorationRequirements( final DecorationRequirements wssReq,
                                                 final EncryptionContext encryptionContext ) {
        if (encryptionContext.clientCert != null) {
            wssReq.setRecipientCertificate(encryptionContext.clientCert);
            if (wssReq.getKeyEncryptionAlgorithm()==null)
                wssReq.setKeyEncryptionAlgorithm(encryptionContext.keyEncryptionAlgorithm);
            // LYONSM: need to rethink configuring a signature and assuming a signature source here
            //wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
            //wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
            //wssReq.setSignTimestamp();
        } else if (encryptionContext.secConvContext != null) {
            // We'll rely on the ServerSecureConversation assertion to (have) configure(d) the WS-SC session.
            wssReq.setSignTimestamp();
        } else if (encryptionContext.encryptedKey != null && encryptionContext.encryptedKey.isUnwrapped()) {
            // As a last resort, we'll use an EncryptedKeySHA1 reference if we have nothing else to go on,
            // but only if it was already unwrapped.
            try {
                wssReq.setEncryptedKey(encryptionContext.encryptedKey.getSecretKey());
                wssReq.setEncryptedKeySha1(encryptionContext.encryptedKey.getEncryptedKeySHA1());
            } catch ( InvalidDocumentFormatException e) {
                throw new IllegalStateException(); // can't happen, it's unwrapped already
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(); // can't happen, it's unwrapped already
            }
            wssReq.setSignTimestamp();
        } else if (encryptionContext.kerberosServiceTicket != null ) {
            wssReq.setKerberosTicket(encryptionContext.kerberosServiceTicket);
        }
    }

    protected static class MultipleTokensException extends Exception {}


    protected static class EncryptionContext {
        private final X509Certificate clientCert;
        private final KerberosServiceTicket kerberosServiceTicket;
        private final SecurityContextToken secConvContext;
        private final EncryptedKey encryptedKey;
        private final String keyEncryptionAlgorithm;
        private final XmlSecurityRecipientContext recipientContext;

        /**
         * Create a new EncryptionContext
         *
         * @param clientCert client cert to encrypt to, or null to use alternate means
         * @param kerberosServiceTicket   kerberos ticked to use for encrypting response, or null to use alternate means
         * @param secConvContext WS-SecureConversation session to encrypt to, or null to use alternate means
         *                   for when the policy uses a secure conversation so that the response
         *                   can be encrypted using that context instead of a client cert.
         *                   this should be plugged in, no idea why it is no longer there
         * @param encryptedKey encrypted key already known to recipient, to use with #EncryptedKeySHA1 reference,
         *                     or null.  This will only be used if no other encryption source is available.
         * @param keyEncryptionAlgorithm The key encryption algorithm to use in the response (if X.509 cert)
         * @param recipientContext the intended recipient for the Security header to create
         */
        private EncryptionContext( final X509Certificate clientCert,
                                   final KerberosServiceTicket kerberosServiceTicket,
                                   final SecurityContextToken secConvContext,
                                   final EncryptedKey encryptedKey,
                                   final String keyEncryptionAlgorithm,
                                   final XmlSecurityRecipientContext recipientContext ) {
            this.clientCert = clientCert;
            this.kerberosServiceTicket = kerberosServiceTicket;
            this.secConvContext = secConvContext;
            this.encryptedKey = encryptedKey;
            this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
            this.recipientContext = recipientContext;
        }

        public boolean hasEncryptionKey() {
            return clientCert != null || secConvContext != null || encryptedKey != null || kerberosServiceTicket != null;    
        }

        public XmlSecurityRecipientContext getRecipientContext() {
            return recipientContext;
        }
    }

}
