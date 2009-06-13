package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.xpath.DeferredFailureDomCompiledXpathHolder;
import com.l7tech.message.Message;
import org.jaxen.JaxenException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
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
 */
public class ServerWssEncryptElement extends AbstractMessageTargetableServerAssertion<WssEncryptElement> {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;
    private final X509Certificate recipientContextCert;
    private final DeferredFailureDomCompiledXpathHolder compiledXpath;

    public ServerWssEncryptElement(WssEncryptElement data, ApplicationContext ctx) throws IOException {
        super(data,data);
        this.auditor = new Auditor(this, ctx, logger);
        this.compiledXpath = new DeferredFailureDomCompiledXpathHolder(assertion.getXpathExpression());

        X509Certificate rccert = null;
        if (!assertion.getRecipientContext().localRecipient()) {
            try {
                rccert = CertUtils.decodeCert(HexUtils.decodeBase64(
                        assertion.getRecipientContext().getBase64edX509Certificate(), true));
            } catch (CertificateException e) {
                logger.log(Level.WARNING, "Assertion will always fail: recipient cert cannot be decoded: " + e.getMessage(), e);
                rccert = null;
            }
        }
        recipientContextCert = rccert;
    }

    // despite the name of this method, i'm actually working on the response document here
    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        X509Certificate clientCert = null;
        KerberosServiceTicket kerberosServiceTicket = null;
        SecurityContextToken secConvContext = null;
        EncryptedKey encryptedKey = null;
        String keyEncryptionAlgorithm = null;
        XmlSecurityRecipientContext recipientContext = null;

        if (!assertion.getRecipientContext().localRecipient()) {
            if (recipientContextCert == null) {
                String msg = "cannot retrieve the recipient cert";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
                throw new PolicyAssertionException(assertion, msg);
            }
            clientCert = recipientContextCert;
            recipientContext = assertion.getRecipientContext();
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
                if (token instanceof SamlSecurityToken) {
                    SamlSecurityToken samlToken = (SamlSecurityToken)token;
                    if (samlToken.isPossessionProved()) {
                        if (clientCert != null) {
                            return doMultipleTokenFailure();
                        }
                        clientCert = samlToken.getSubjectCertificate();
                    }
                } else if (token instanceof X509SigningSecurityToken) {
                    X509SigningSecurityToken x509token = (X509SigningSecurityToken)token;
                    if (x509token.isPossessionProved()) {
                        if (clientCert != null) {
                            return doMultipleTokenFailure();
                        }
                        clientCert = x509token.getMessageSigningCertificate();
                        keyEncryptionAlgorithm = wssResult.getLastKeyEncryptionAlgorithm();
                    }
                } else if (token instanceof KerberosSigningSecurityToken) {
                    KerberosSigningSecurityToken kerberosSecurityToken = (KerberosSigningSecurityToken)token;
                    if (kerberosServiceTicket != null) {
                        return doMultipleTokenFailure();
                    }
                    kerberosServiceTicket = kerberosSecurityToken.getServiceTicket();
                } else if (token instanceof SecurityContextToken) {
                    SecurityContextToken secConvTok = (SecurityContextToken)token;
                    if (secConvTok.isPossessionProved()) {
                        secConvContext = secConvTok;
                    }
                } else if (token instanceof EncryptedKey) {
                    if (encryptedKey != null) {
                        return doMultipleTokenFailure();
                    }
                    encryptedKey = (EncryptedKey)token;
                }
            }

            if (clientCert == null && secConvContext == null && encryptedKey == null && kerberosServiceTicket==null) {
                auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_NO_CERT_OR_SC_TOKEN);
            }
        }

        return addDecorationRequirements(
                                 message,
                                 messageDescription,
                                 clientCert,
                                 kerberosServiceTicket,
                                 secConvContext,
                                 encryptedKey,
                                 keyEncryptionAlgorithm,
                                 recipientContext,
                                 context);
    }

    private AssertionStatus doMultipleTokenFailure() {
        auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MORE_THAN_ONE_TOKEN);
        return AssertionStatus.BAD_REQUEST; // todo make multiple security tokens work
    }

    /**
     * Immediately configure response decoration.
     *
     * @param message The message to be encrypted
     * @param clientCert client cert to encrypt to, or null to use alternate means
     * @param kerberosServiceTicket   kerberos ticked to use for encrypting response, or null to use alternate means
     * @param secConvTok WS-SecureConversation session to encrypt to, or null to use alternate means
     *                   for when the policy uses a secure conversation so that the response
     *                   can be encrypted using that context instead of a client cert.
     *                   this should be plugged in, no idea why it is no longer there
     * @param encryptedKey encrypted key already known to recipient, to use with #EncryptedKeySHA1 reference,
     *                     or null.  This will only be used if no other encryption source is available.
     * @param keyEncryptionAlgorithm The key encryption algorithm to use in the response (if X.509 cert)
     * @param recipient the intended recipient for the Security header to create
     * @param context  the PolicyEnforcementContext.  Required.
     * @return the AssertionStatus
     * @throws com.l7tech.policy.assertion.PolicyAssertionException  if the XPath expression is invalid
     * @throws java.io.IOException if there is a problem gathering info from the request
     */
    private AssertionStatus addDecorationRequirements(
                                              final Message message,
                                              final String messageDescription,
                                              final X509Certificate clientCert,
                                              final KerberosServiceTicket kerberosServiceTicket,
                                              final SecurityContextToken secConvTok,
                                              final EncryptedKey encryptedKey,
                                              final String keyEncryptionAlgorithm,
                                              final XmlSecurityRecipientContext recipient,
                                              final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        try {
            if (!message.isSoap()) {
                auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        // GET THE DOCUMENT
        final Document soapmsg;
        try {
            soapmsg = message.getXmlKnob().getDocumentReadOnly();

            final List selectedElements;
            try {
                selectedElements = compiledXpath.getCompiledXpath().rawSelectElements(soapmsg,
                        new PolicyEnforcementContextXpathVariableFinder(context));
            } catch (JaxenException e) {
                // this is thrown when there is an error in the expression
                // this is therefore a bad policy
                throw new PolicyAssertionException(assertion, e);
            }

            if (selectedElements == null || selectedElements.size() < 1) {
                auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_NOT_ENCRYPTED, messageDescription);
                return AssertionStatus.FALSIFIED;
            }
            DecorationRequirements wssReq;
            wssReq = message.getSecurityKnob().getAlternateDecorationRequirements(recipient);
            //noinspection unchecked
            wssReq.getElementsToEncrypt().addAll(selectedElements);
            wssReq.setEncryptionAlgorithm(assertion.getXEncAlgorithm());
            if (clientCert != null) {
                wssReq.setRecipientCertificate(clientCert);
                wssReq.setKeyEncryptionAlgorithm(assertion.getKeyEncryptionAlgorithm());
                if (wssReq.getKeyEncryptionAlgorithm()==null)
                    wssReq.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                // LYONSM: need to rethink configuring a signature and assuming a signature source here
                //wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                //wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
                //wssReq.setSignTimestamp();
            } else if (secConvTok != null) {
                // We'll rely on the ServerSecureConversation assertion to (have) configure(d) the WS-SC session.
                wssReq.setSignTimestamp();
            } else if (encryptedKey != null && encryptedKey.isUnwrapped()) {
                // As a last resort, we'll use an EncryptedKeySHA1 reference if we have nothing else to go on,
                // but only if it was already unwrapped.
                try {
                    wssReq.setEncryptedKey(encryptedKey.getSecretKey());
                    wssReq.setEncryptedKeySha1(encryptedKey.getEncryptedKeySHA1());
                } catch ( InvalidDocumentFormatException e) {
                    throw new IllegalStateException(); // can't happen, it's unwrapped already
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException(); // can't happen, it's unwrapped already
                }
                wssReq.setSignTimestamp();
            } else if (kerberosServiceTicket != null ) {
                wssReq.setKerberosTicket(kerberosServiceTicket);
            }

            auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_ENCRYPTED, messageDescription, String.valueOf(selectedElements.size()));
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to encrypt";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }
}
