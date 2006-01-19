/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.decorator;

import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.xml.SecurityActor;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.util.SoapUtil;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author mike
 */
public class DecorationRequirements {

    public X509Certificate getRecipientCertificate() {
        return recipientCertificate;
    }

    public void setRecipientCertificate(X509Certificate recipientCertificate) {
        this.recipientCertificate = recipientCertificate;
    }

    /**
     * Get the sender's message signing certificate.
     *
     * @return The signing cert, if it should be encoded as a BST, or null.
     * @see #setSenderMessageSigningCertificate
     */
    public X509Certificate getSenderMessageSigningCertificate() {
        return senderMessageSigningCertificate;
    }

    /**
     * Set the sender's message signing certificate.  This must match the value of senderMessageSigningPrivateKey,
     * if it too is specified.
     * <p>
     * This does not need to be specified when using a SAML assertion unless the message signer certificate
     * is not included in the SAML assertion.  For example:
     * <p>
     * If senderSamlTicket is set to a Holder-of-key assertion, no senderMessageSigningCertificate should be set
     * since the SAML subject cert is the message signing cert.
     * <p>
     * If senderSamlTicket is set to a Sender-Vouches assertion, a senderMessageSigningCertificate should be set.
     * Sometimes this will be the same as the SAML assertion's issuer certificate, but not necessarily.
     * <p>
     * TODO use SignerInfo instead; possibly make SamlAssertion and X509BinarySecurityToken both implement
     * some kind of "message signature source" interface.
     * @param senderMessageSigningCertificate the signing cert, if it should be encoded as a BST, or null.
     */
    public void setSenderMessageSigningCertificate(X509Certificate senderMessageSigningCertificate) {
        this.senderMessageSigningCertificate = senderMessageSigningCertificate;
    }

    public PrivateKey getSenderMessageSigningPrivateKey() {
        return senderMessageSigningPrivateKey;
    }

    public void setSenderMessageSigningPrivateKey(PrivateKey senderMessageSigningPrivateKey) {
        this.senderMessageSigningPrivateKey = senderMessageSigningPrivateKey;
    }

    /**
     * Check if a signed timestamp is required to be present.
     * @return true if a signed timestamp will always be added.
     * @see #setSignTimestamp
     */
    public boolean isSignTimestamp() {
        return signTimestamp;
    }

    /**
     * Require a signed timestamp to be present.<p>
     * <p/>
     * If this is set, a timestamp will be added to the document regardless of the
     * content of elementsToSign.<p>
     * <p/>
     * Otherwise, a timestamp will added to the document only if elementsToSign
     * is non-empty.<p>
     * <p/>
     * Regardless of this setting, if a timestamp is added to the document it will always be signed,
     * either directly or indirectly.  It will be signed directly unless it will be covered by an
     * Envelope signature.<p>
     */
    public void setSignTimestamp() {
        setIncludeTimestamp();
        this.signTimestamp = true;
    }

    /**
     * If this is true, we'll always include a timestamp in the decorated message.
     * It won't be signed unless signTimestamp is set (or anything else is signed, in which
     * case a signed timestamp will always be included regardless).
     */
    public void setIncludeTimestamp() {
        this.includeTimestamp = true;
    }

    /**
     * @see #setIncludeTimestamp
     * @return true if a timestamp will be included always; false if a timestamp won't be included unless necessary
     */
    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    /**
     * populate this with Element objects
     */
    public Set getElementsToEncrypt() {
        return elementsToEncrypt;
    }

    /**
     * Returns the required encryption algorithm. The default is "http://www.w3.org/2001/04/xmlenc#aes128-cbc"
     *
     * @return the encryption algorithm requested
     */
    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    /**
     * Set the required encryption algoirthm.
     * Supported values are:
     * <ul>
     * <li> triple des - http://www.w3.org/2001/04/xmlenc#tripledes-cbc
     * <li> aes 128    - http://www.w3.org/2001/04/xmlenc#aes128-cbc
     * <li> aes 192    - http://www.w3.org/2001/04/xmlenc#aes192-cbc
     * <li> aes 256    - http://www.w3.org/2001/04/xmlenc#aes256-cbc
     * </ul>
     * @param encryptionAlgorithm the required algorithm
     */
    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        if (encryptionAlgorithm == null) {
            throw new IllegalArgumentException();
        }
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    /**
     * populate this with Element objects
     */
    public Set getElementsToSign() {
        return elementsToSign;
    }

    public UsernameToken getUsernameTokenCredentials() {
        return usernameTokenCredentials;
    }

    public void setUsernameTokenCredentials(UsernameToken usernameTokenCredentials) {
        this.usernameTokenCredentials = usernameTokenCredentials;
    }

    public Element getSenderSamlToken() {
        return senderSamlToken;
    }

    /**
     * Set the saml assertion token and include it into the signature
     *
     * @param senderSamlToken the sender saml token
     * @see DecorationRequirements#setSenderSamlToken(org.w3c.dom.Element, boolean)
     */
    public void setSenderSamlToken(Element senderSamlToken) {
        setSenderSamlToken(senderSamlToken, true);
    }

    /**
     * Set the sender saml token and indicate whether to include it in the signature.
     *
     * @param senderSamlToken  the sender saml token
     * @param sign include in signature or false to omitt assertion from signature
     */
    public void setSenderSamlToken(Element senderSamlToken, boolean sign) {
        this.senderSamlToken = senderSamlToken;
        this.includeSamlTokenInSignature = sign;
    }

    public boolean isIncludeSamlTokenInSignature() {
        return includeSamlTokenInSignature;
    }

    public SecureConversationSession getSecureConversationSession() {
        return secureConversationSession;
    }

    public void setSecureConversationSession(SecureConversationSession secureConversationSession) {
        this.secureConversationSession = secureConversationSession;
    }

    public String getPreferredWSUNamespace() {
        return preferredWSUNamespace;
    }

    /**
     * If this is not set, then the default SoapUtil.WSU_NAMESPACE
     * will be used.
     */
    public void setPreferredWSUNamespace(String ns) {
        preferredWSUNamespace = ns;
    }

    /**
     * If this is set along with EncryptedKey, then signing and encryption will use a KeyInfo that uses
     * a KeyInfo of #EncryptedKeySHA1 containing this string, which must be the Base64-encoded
     * SHA1 hash of the raw octets of the key (that is, the the pre-Base64-encoding octets of the
     * CipherValue in the EncryptedKey token being referenced).
     * <p/>
     * The EncryptedKey will be assumed already to be known to the recipient.  It is the callers
     * responsibility to ensure that the recipient will recognize this encryptedKeySha1 hash.
     * <p/>
     * This value will not be used unless both it and {@link #setEncryptedKey} are set to non-null values.
     * EncryptedKey should return the actual key to use for the signing and encryption.
     *
     * @param encryptedKeySha1 the base64-encoded SHA1 hash of the key octets from an EncryptedKey, or null
     *                         to disable use of #EncryptedKeySHA1 KeyInfo for signature and encryption blocks.
     */
    public void setEncryptedKeySha1(String encryptedKeySha1) {
        this.encryptedKeySha1 = encryptedKeySha1;
    }

    /**
     * @return  the base64-encoded SHA1 hash of the key octets from an EncryptedKey, or null
     *          to disable use of #EncryptedKeySHA1 KeyInfo for signature and encryption blocks.
     * @see #setEncryptedKeySha1
     */
    public String getEncryptedKeySha1() {
        return encryptedKeySha1;
    }

    /**
     * Set the actual secret key bytes to use for signing and encryption when using #EncryptedKeySHA1 style
     * KeyInfos inside signature and encryption blocks.  See {@link #setEncryptedKey} for more information.
     * See {@link com.l7tech.common.security.AesKey} for a possible implementation.
     *
     * @param encryptedKey  symmetric key for signing and encryption, or null to disable use of #EncryptedKeySHA1.
     */
    public void setEncryptedKey(SecretKey encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    /**
     * @return symmetric key for signing and encryption, or null to disable use of #EncryptedKeySHA1.
     * @see #setEncryptedKey
     * @see #setEncryptedKeySha1
     */
    public SecretKey getEncryptedKey() {
        return encryptedKey;
    }

    /**
     * Set the wsse11:SignatureConfirmation value to use for this reply, if any.  This must be the still-base64-encoded
     * content of the dsig:SignatureValue whose value is being confirmed.  A SignatureConfirmation value will
     * only be included in the decorated message if the rest of the decoration requirements are sufficient to
     * allow the SignatureConfirmation to be signed.  That is, a signature source of some kind must also be supplied
     * or else the signatureConfirmation will not be added to the decorated message.  Valid signature sources include
     * a sender cert with private key, a sender SAML holder-of-key token with subject private key,
     * a secure conversation session, or an EncryptedKeySHA1 reference plus associated EncryptedKey shared secret.
     *
     * @param signatureConfirmation the base64 SignatureValue of the signature that is to be confirmed, or null.
     */
    public void setSignatureConfirmation(String signatureConfirmation) {
        this.signatureConfirmation = signatureConfirmation;
    }

    /**
     * @return the base64 SignatureValue of the signature that is to be confirmed, or null.
     * @see #setSignatureConfirmation
     */
    public String getSignatureConfirmation() {
        return signatureConfirmation;
    }

    public interface SecureConversationSession {
        String getId();

        byte[] getSecretKey();
    }

    public String getPreferredSecurityNamespace() {
        return preferredSecurityNamespace;
    }

    /**
     * If you dont set this, the default WSSE namespace will be used. You would
     * want to specify this namespace for example when decorating a response to
     * a request that already referred to a specific wsse namespace.
     *
     * @param preferredSecurityNamespace the wsse namespace.
     */
    public void setPreferredSecurityNamespace(String preferredSecurityNamespace) {
        this.preferredSecurityNamespace = preferredSecurityNamespace;
    }

    /**
     * @return The overridden created date to use for the timestamp, or null to allow it to default.
     */
    public Date getTimestampCreatedDate() {
        return timestampCreatedDate;
    }

    /**
     * @param timestampCreatedDate a date to override the Created date to use for the timestamp, or null to allow it to default.
     */
    public void setTimestampCreatedDate(Date timestampCreatedDate) {
        this.timestampCreatedDate = timestampCreatedDate;
    }

    /**
     * @return overridden timestamp lifetime in milliseconds, or 0 if it will be allowed to default.
     */
    public int getTimestampTimeoutMillis() {
        return timestampTimeoutMillis;
    }

    /**
     * @param timestampTimeoutMillis overridden timestamp lifetime in milliseconds, or 0 if it will be allowed to default.
     */
    public void setTimestampTimeoutMillis(int timestampTimeoutMillis) {
        this.timestampTimeoutMillis = timestampTimeoutMillis;
    }

    /**
     * @return true iff. this decoration requirements has one of the following tokens that can be used to sign elements:
     *         a sender cert and private key; a secure conversation session; or a sender SAML token.
     */
    public boolean hasSignatureSource() {
        return senderSamlToken != null || secureConversationSession != null ||
          (senderMessageSigningCertificate != null && senderMessageSigningPrivateKey != null);
    }

    /**
     * If not null, the wss decorations will be placed in a soap security header whose actor attribute value
     * is specified here.
     *
     * @return the soap security header actor value
     */
    public String getSecurityHeaderActor() {
        return securityHeaderActor;
    }

    /**
     * Set the soap security header actor value for the security header in which those wss decorations
     * must be inserted. null value means that these wss decorations belong in the default security
     * header (the one with no actor attribute).
     */
    public void setSecurityHeaderActor(String securityHeaderActor) {
        this.securityHeaderActor = securityHeaderActor;
    }

    /**
     * If a BinarySecurityToken would need to be generated to hold a sender signing certificate, enabling
     * suppressBst causes it to emit no BST, and instead to refer to the sender cert via its SubjectKeyIdentifier
     * in the Signature's KeyInfo.
     *
     * @return  false to allow normal BST addition, if needed.  True to never add a BST.
     */
    public boolean isSuppressBst() {
        return suppressBst;
    }

    /**
     * If a BinarySecurityToken would need to be generated to hold a sender signing certificate, enabling
     * suppressBst causes it to emit no BST, and instead to refer to the sender cert via its SubjectKeyIdentifier
     * in the Signature's KeyInfo.
     *
     * @param suppressBst  false to allow normal BST addition, if needed.  True to never add a BST.
     */
    public void setSuppressBst(boolean suppressBst) {
        this.suppressBst = suppressBst;
    }

    public KerberosServiceTicket getKerberosTicket() {
        return kerberosTicket;
    }

    public void setKerberosTicket(KerberosServiceTicket kerberosTicket) {
        this.kerberosTicket = kerberosTicket;
    }

    public boolean isIncludeKerberosTicket() {
        return includeKerberosTicket;
    }

    public void setIncludeKerberosTicket(boolean includeKerberosTicket) {
        this.includeKerberosTicket = includeKerberosTicket;
    }

    public String getKerberosTicketId() {
        return kerberosTicketId;
    }

    public void setKerberosTicketId(String kerberosTicketId) {
        this.kerberosTicketId = kerberosTicketId;
    }

    public boolean isIncludeKerberosTicketId() {
        return includeKerberosTicketId;
    }

    public void setIncludeKerberosTicketId(boolean includeKerberosTicketId) {
        this.includeKerberosTicketId = includeKerberosTicketId;
    }

    private X509Certificate recipientCertificate = null;
    private X509Certificate senderMessageSigningCertificate = null;
    private PrivateKey senderMessageSigningPrivateKey = null;
    private UsernameToken usernameTokenCredentials = null;
    private Element senderSamlToken = null;
    private SecureConversationSession secureConversationSession = null;
    private boolean includeTimestamp = true;
    private boolean signTimestamp;
    private Set elementsToEncrypt = new LinkedHashSet();
    private String encryptionAlgorithm = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    //private String encryptionAlgorithm = "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";
    private Set elementsToSign = new LinkedHashSet();
    private String preferredSecurityNamespace = SoapUtil.SECURITY_NAMESPACE;
    private String preferredWSUNamespace = SoapUtil.WSU_NAMESPACE;
    private Date timestampCreatedDate = null;
    private int timestampTimeoutMillis = 0;
    private String securityHeaderActor = SecurityActor.L7ACTOR.getValue();
    private boolean includeSamlTokenInSignature = false;
    private boolean suppressBst = false;
    private SecretKey encryptedKey = null;
    private String encryptedKeySha1 = null;
    private String signatureConfirmation = null;
    private KerberosServiceTicket kerberosTicket = null;
    private boolean includeKerberosTicket = false;
    private String kerberosTicketId = null;
    private boolean includeKerberosTicketId = false;
}
