package com.l7tech.security.xml.decorator;

import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.saml.SamlAssertion;
import org.w3c.dom.Element;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.l7tech.security.xml.decorator.DecorationRequirements.WsaHeaderSigningStrategy.DEFAULT_WSA_HEADER_SIGNING_BEHAVIOUR;

/**
 * @author mike
 */
public class DecorationRequirements {

    public DecorationRequirements() {
    }

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
     * @param signTimestamp true to set includeTimestamp to true and mark it as always signed; false to turn off forced-inclusion of signed timestamp.
     */
    public void setSignTimestamp(boolean signTimestamp) {
        if (signTimestamp)
            setIncludeTimestamp(true);
        this.signTimestamp = signTimestamp;
    }

    /**
     * If this is true, we'll always include a timestamp in the decorated message.
     * It won't be signed unless signTimestamp is set (or anything else is signed, in which
     * case a signed timestamp will always be included regardless).
     */
    public void setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    /**
     * @see #setIncludeTimestamp
     * @return true if a timestamp will be included always; false if a timestamp won't be included unless necessary
     */
    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    /**
     * @see #setOmitTimestamp
     * @return true if a timestamp will be omitted; false if a timestamp won't be omitted
     */
    public boolean isOmitTimestamp() {
        return omitTimestamp;
    }

    /**
     * If this is true, we'll always omit the timestamp in the decorated message.
     * It won't be signed unless signTimestamp is set.
     */
    public void setOmitTimestamp(boolean omitTimestamp) {
        this.omitTimestamp = omitTimestamp;
    }

    /**
     * @see #setNeverSignTimestamp
     * @return true if a signed timestamp will not be added.
     */
    public boolean isNeverSignTimestamp() {
        return neverSignTimestamp;
    }

    /**
     * If this is true, a signed timestamp will not be added. A signed timestamp will only be added if this is false.
     */
    public void setNeverSignTimestamp(boolean neverSignTimestamp) {
        this.neverSignTimestamp = neverSignTimestamp;
    }

    /**
     * populate this with Element objects
     */
    public Map<Element, ElementEncryptionConfig> getElementsToEncrypt() {
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

    public void addElementToSign(Element element) {
        getElementsToSign().add(element);
    }

    /**
     * populate this with Element objects
     * @return the current set of elements to sign.  never null
     */
    public Set<Element> getElementsToSign() {
        return elementsToSign;
    }

    public Set<String> getPartsToSign() {
        return partsToSign;
    }

    public boolean isSignPartHeaders() {
        return signPartHeaders;
    }

    public void setSignPartHeaders(boolean signPartHeaders) {
        this.signPartHeaders = signPartHeaders;
    }

    public UsernameToken getUsernameTokenCredentials() {
        return usernameTokenCredentials;
    }

    public void setUsernameTokenCredentials(UsernameToken usernameTokenCredentials) {
        this.usernameTokenCredentials = usernameTokenCredentials;
    }

    public SamlAssertion getSenderSamlToken() {
        return senderSamlToken;
    }

    /**
     * Set the saml assertion token and include it into the signature
     *
     * @param senderSamlToken the sender saml token
     * @see DecorationRequirements#setSenderSamlToken(SamlAssertion, boolean)
     */
    public void setSenderSamlToken(SamlAssertion senderSamlToken) {
        setSenderSamlToken(senderSamlToken, true);
    }

    /**
     * Set the sender saml token and indicate whether to include it in the signature.
     *
     * @param senderSamlToken  the sender saml token
     * @param sign include in signature or false to omitt assertion from signature
     */
    public void setSenderSamlToken(SamlAssertion senderSamlToken, boolean sign) {
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

    /**
     * @return if true, decorator will avoid including an SCT in the message; instead, derived keys dervied from a wccs session will use a direct URI reference to the session ID. 
     */
    public boolean isOmitSecurityContextToken() {
        return omitSecurityContextToken;
    }

    /**
     * @param omitSecurityContextToken  true to avoid including an SCT in the message; instead, derived keys derived from a wssc session will use a direct URI reference to the session ID.
     */
    public void setOmitSecurityContextToken(boolean omitSecurityContextToken) {
        this.omitSecurityContextToken = omitSecurityContextToken;
    }

    /**
     * If this is set along with EncryptedKey, then signing and encryption will use a KeyInfo or KeyIdentifier that uses
     * the specified reference information.
     * <p/>
     * See {@link com.l7tech.security.xml.KeyInfoDetails#makeEncryptedKeySha1Ref(java.lang.String)} in order to use
     * a KeyInfo of #EncryptedKeySHA1 containing the EncryptedKeySHA1 string, which must be the Base64-encoded
     * SHA1 hash of the raw octets of the key (that is, the the pre-Base64-encoding octets of the
     * CipherValue in the EncryptedKey token being referenced).
     * <p/>
     * The EncryptedKey will be assumed already to be known to the recipient.  It is the callers
     * responsibility to ensure that the recipient will recognize this key identifier mechanism.
     * <p/>
     * This value will not be used unless both it and {@link #setEncryptedKey} are set to non-null values.
     * {@link @getEncryptedKey} should return the actual secret key to use (if only as a derivation source) for the signing and encryption.
     * <p/>
     *
     * @param encryptedKeyReferenceInfo the key identifier value and valuetype to use when referring to the encrypted key.
     *                         For example, to cause uses of the corresponding secret to point to it via EncryptedKeySHA1
     *                         referenes, this would contain (as the value) the base64-encoded SHA1 hash of the key octets
     *                         from an EncryptedKey.
     *                         <p/>
     *                         If this is null, a secret key provided with {@link #setEncryptedKey(byte[])} will not
     *                         be used for signing or encryption (either directly or via derived keys) unless
     *                         the decorator needs to create a new EncryptedKey token (in which case it may use
     *                         the specified key material rather than generating fresh material).
     */
    public void setEncryptedKeyReferenceInfo(KeyInfoDetails encryptedKeyReferenceInfo) {
        this.encryptedKeyReferenceInfo = encryptedKeyReferenceInfo;
    }

    /**
     * @return  the base64-encoded SHA1 hash of the key octets from an EncryptedKey, or null
     *          to disable use of #EncryptedKeySHA1 KeyInfo for signature and encryption blocks.
     * @see #setEncryptedKeyReferenceInfo
     */
    public KeyInfoDetails getEncryptedKeyReferenceInfo() {
        return encryptedKeyReferenceInfo;
    }

    /**
     * Set the actual secret key bytes to use for signing and encryption when using #EncryptedKeySHA1 style
     * KeyInfos inside signature and encryption blocks.  See {@link #setEncryptedKey} for more information.
     * <p/>
     * If {@link #getEncryptedKeyReferenceInfo} returns null, no EncryptedKeySHA1 references will be generated.  However,
     * if a new EncryptedKey is generated anyway, it will prefer to use this secret key, if it is non-null,
     *  rather than generating a new one.
     *
     * @param encryptedKey  symmetric key for signing and encryption, or null to disable use of #EncryptedKeySHA1.
     */
    public void setEncryptedKey(byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    /**
     * @return symmetric key for signing and encryption, or null to disable use of #EncryptedKeySHA1.
     * @see #setEncryptedKey
     */
    public byte[] getEncryptedKey() {
        return encryptedKey;
    }

    /**
     * Set the key encryption algorithm to use.
     *
     * <ul>
     * <li>http://www.w3.org/2001/04/xmlenc#rsa-1_5</li>
     * <li>http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p</li>
     * </ul>
     *
     * @param keyEncryptionAlgorithm The algorithm to use, null for default
     */
    public void setKeyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
    }

    /**
     * Get the key encryption algorithm.
     *
     * @return The algorithm to use, null if not set
     */
    public String getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    /**
     * Add a wsse11:SignatureConfirmation value to use for this reply, if required. This must be the still-base64-encoded
     * content of the dsig:SignatureValue whose value is being confirmed.  SignatureConfirmation values will
     * only be included in the decorated message if the rest of the decoration requirements are sufficient to
     * allow the SignatureConfirmation elements to be signed.  That is, a signature source of some kind must also be supplied
     * or else the signatureConfirmation will not be added to the decorated message.  Valid signature sources include
     * a sender cert with private key, a sender SAML holder-of-key token with subject private key,
     * a secure conversation session, or an EncryptedKeySHA1 reference plus associated EncryptedKey shared secret.
     *
     * @param signatureConfirmation the base64 SignatureValue of the signature that is to be confirmed,
     *                              or null if the request did not contain any signatures but confirmation is needed.
     */
    public void addSignatureConfirmation(String signatureConfirmation) {
        if ( (signatureConfirmations.contains(null) && signatureConfirmation != null) ||
             (!signatureConfirmations.isEmpty() && !signatureConfirmations.contains(null) && signatureConfirmation == null))
            throw new IllegalArgumentException("Cannot confirm both the lack and the presence of signatures in a request.");
        this.signatureConfirmations.add(signatureConfirmation);
    }

    /**
     * Get the wsse11:SignatureConfirmation values to include in this message.
     * If the request did not have any signatures but required confirmation, a list containing exactly a null entry is returned.
     *
     * @return a list of base64 SignatureValue strings of the signatures that are to be confirmed,
     *         or a list with a null value to confirm that no signatures from the request were processed.
     *         May be empty but never null.
     * @see #addSignatureConfirmation
     */
    public Collection<String> getSignatureConfirmations() {
        return signatureConfirmations;
    }

    /**
     * @return true if any SAML assertion(s) referenced from a Signature should use References to a non-schema-compliant
     * wsu:Id attribute in the assertion instead of using the STR dereference transform.
     */
    public boolean isSuppressSamlStrTransform() {
        return suppressSamlStrTransform;
    }

    /**
     * @param suppressSamlStrTransform true if any SAML assertion(s) referenced from a Signature should use References
     * to a non-schema-compliant wsu:Id attribute in the assertion instead of using the STR dereference transform.
     */
    public void setSuppressSamlStrTransform(boolean suppressSamlStrTransform) {
        this.suppressSamlStrTransform = suppressSamlStrTransform;
    }

    /**
     * Check if a particular WSS version has been explicitly requested.
     *
     * @return the explicitly-requested WSS version, or null.
     */
    public WsSecurityVersion getWssVersion() {
        return wssVersion;
    }

    /**
     * Explicitly request a particular version of WS-Security be used during decoration.
     * <p/>
     * Setting this to WSS 1.0
     * <p/>
     * Currently the only effect of setting this to WSS 1.1 is to cause {@link #isWss11} to always return true,
     * which may cause signature confirmations to be added (by users of WssDecorator, rather than by the decorator itself) prior to decoration.
     *
     * @param wssVersion a specific WSS version to request, or null.
     */
    public void setWssVersion(WsSecurityVersion wssVersion) {
        this.wssVersion = wssVersion;
    }

    public boolean isWss11() {
        return wssVersion != null
                ? wssVersion.equals(WsSecurityVersion.WSS11)
                : !signatureConfirmations.isEmpty() || encryptUsernameToken;
        // todo: check for other WSS11 decorations -- encrypted headers, when they will be supported
    }

    public void addElementToEncrypt(Element element) {
        addElementToEncrypt(element, new ElementEncryptionConfig(true));
    }

    public void addElementToEncrypt(Element element, ElementEncryptionConfig config) {
        getElementsToEncrypt().put(element, config);
    }

    public interface SecureConversationSession extends SecurityContext {
        String getId();
        Element getElement();
        byte[] getSecretKey();
        String getSCNamespace();
    }

    /** A simple implementation of SecureConversationSession for users with simple needs. */
    public static class SimpleSecureConversationSession implements SecureConversationSession {
        private String id;
        private byte[] key;
        private String ns;

        public SimpleSecureConversationSession() {
        }

        public SimpleSecureConversationSession(String id, byte[] key, String ns) {
            this.id = id;
            this.key = key;
            this.ns = ns;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public byte[] getSecretKey() {
            return key;
        }

        @Override
        public SecurityToken getSecurityToken() {
            return null;
        }

        @Override
        public String getSCNamespace() {
            return ns;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setSecretKey(byte[] key) {
            this.key = key;
        }

        public void setSCNamespace(String ns) {
            this.ns = ns;
        }

        @Override
        public byte[] getSharedSecret() {
            return key;
        }

        @Override
        public Element getElement() {
            return null;
        }
    }

    /**
     * Get the namespace settings to use during decoration.
     *
     * @return a NamespaceFactory.  Never null.
     */
    public NamespaceFactory getNamespaceFactory() {
        return namespaceFactory;
    }

    /**
     * Set new namespace settings to use during decoration.
     *
     * @param namespaceFactory the new NamespaceFactory, or null to use the default namespaces.
     */
    public void setNamespaceFactory(NamespaceFactory namespaceFactory) {
        if (namespaceFactory == null) namespaceFactory = new NamespaceFactory();
        this.namespaceFactory = namespaceFactory;
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
    public long getTimestampTimeoutMillis() {
        return timestampTimeoutMillis;
    }

    /**
     * @param timestampTimeoutMillis overridden timestamp lifetime in milliseconds, or 0 if it will be allowed to default.
     */
    public void setTimestampTimeoutMillis(long timestampTimeoutMillis) {
        this.timestampTimeoutMillis = timestampTimeoutMillis;
    }

    public enum TimestampResolution { DEFAULT, NANOSECONDS, MILLISECONDS, SECONDS }

    public TimestampResolution getTimestampResolution() {
        return timestampResolution;
    }

    public void setTimestampResolution( final TimestampResolution timestampResolution ) {
        this.timestampResolution = timestampResolution;
    }

    /**
     * @return true iff. this decoration requirements has one of the following tokens that can be used to sign elements:
     *         a sender cert and private key; a secure conversation session; or a sender SAML token.
     */
    public boolean hasSignatureSource() {
        return senderSamlToken != null || secureConversationSession != null ||
          (senderMessageSigningCertificate != null && senderMessageSigningPrivateKey != null) ||
          (encryptUsernameToken && usernameTokenCredentials != null) ||
          (encryptedKey != null && encryptedKeyReferenceInfo != null);
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
     * Should the security header actor attribute be in the SOAP namespace.
     *
     * <p>This does not apply to the SOAP 1.2 role attribute.</p>
     *
     * @return True if the attribute should be namespaced
     */
    public boolean isSecurityHeaderActorNamespaced() {
        return securityHeaderActorNamespaced;
    }

    public void setSecurityHeaderActorNamespaced( final boolean securityHeaderActorNamespaced ) {
        this.securityHeaderActorNamespaced = securityHeaderActorNamespaced;
    }

    /** @return true if Security header should be created asserting mustUnderstand; false if it should not assert mustUnderstand; and null if it should use the WssDecorator's default behavior. */
    public Boolean getSecurityHeaderMustUnderstand() {
        Boolean mustUnderstand;

        switch ( securityHeaderMustUnderstand ) {
            case YES:
                mustUnderstand = true;
                break;
            case NO:
            case SKIP:
                mustUnderstand = false;
                break;
            default:
                mustUnderstand = null;
        }

        return mustUnderstand;
    }

    /** @param securityHeaderMustUnderstand true if Security header should be created asserting mustUnderstand; false if it should not assert mustUnderstand; and null if it should use WssDecorator's default behavior. */
    public void setSecurityHeaderMustUnderstand(Boolean securityHeaderMustUnderstand) {
        setSecurityHeaderMustUnderstand( securityHeaderMustUnderstand, true );
    }

    /**
     * @param securityHeaderMustUnderstand true if Security header should be created asserting mustUnderstand; false if it should not assert mustUnderstand; and null if it should use WssDecorator's default behavior.
     * @param alwaysInclude true to include the mustUnderstand attribute even if it makes no difference to the meaning of the header (i.e. is false)
     */
    public void setSecurityHeaderMustUnderstand(Boolean securityHeaderMustUnderstand, boolean alwaysInclude ) {
        if ( securityHeaderMustUnderstand==null ) {
            this.securityHeaderMustUnderstand = SecurityHeaderMustUnderstand.DEFAULT;
        } else if ( securityHeaderMustUnderstand ) {
            this.securityHeaderMustUnderstand = SecurityHeaderMustUnderstand.YES;
        } else {
            this.securityHeaderMustUnderstand = alwaysInclude ?
                    SecurityHeaderMustUnderstand.NO :
                    SecurityHeaderMustUnderstand.SKIP;
        }
    }

    /**
     * If true the the decorator should attempt to reuse any existing security header for the same actor/role.
     *
     * <p>If a header is reused then the namespaces in the message may not match those set in the
     * decoration requirements.</p>
     *
     * @return true if reuse is permissible
     */
    public boolean isSecurityHeaderReusable() {
        return securityHeaderReusable;
    }

    /**
     * Set the header as reusable (or not)
     *
     * @param securityHeaderReusable true to allow reuse.
     */
    public void setSecurityHeaderReusable(boolean securityHeaderReusable) {
        this.securityHeaderReusable = securityHeaderReusable;
    }

    /**
     * If a BinarySecurityToken would need to be generated to hold a sender signing certificate, using something other
     * than {@link KeyInfoInclusionType#CERT} (the default) causes the decorator to emit no BST, and instead to refer to
     * the sender cert via either a {@link KeyInfoInclusionType#STR_THUMBPRINT SHA-1 thumbprint},
     * {@link KeyInfoInclusionType#STR_SKI SKI} or {@link KeyInfoInclusionType#ISSUER_SERIAL X509IssuerSerial}
     * in the Signature's KeyInfo.
     *
     * @return the KeyInfo inclusion type.
     */
    public KeyInfoInclusionType getKeyInfoInclusionType() {
        return keyInfoInclusionType;
    }

    /**
     * If a BinarySecurityToken would need to be generated to hold a sender signing certificate, using something other
     * than {@link KeyInfoInclusionType#CERT} (the default) causes the decorator to emit no BST, and instead to refer to
     * the sender cert via either a {@link KeyInfoInclusionType#STR_THUMBPRINT SHA-1 thumbprint},
     * {@link KeyInfoInclusionType#STR_SKI SKI} or {@link KeyInfoInclusionType#ISSUER_SERIAL X509IssuerSerial}
     * in the Signature's KeyInfo.
     *
     * @param keyInfoInclusionType the KeyInfo inclusion type.
     */
    public void setKeyInfoInclusionType(KeyInfoInclusionType keyInfoInclusionType) {
        this.keyInfoInclusionType = keyInfoInclusionType;
    }

    /**
     * Get the key reference type for use with encryption.
     *
     * @return The encryption KeyInfoInclusionType
     */
    public KeyInfoInclusionType getEncryptionKeyInfoInclusionType() {
        return encryptionKeyInfoInclusionType;
    }

    /**
     * Set the key references type for use with encryption.
     *
     * @param encryptionKeyInfoInclusionType The encryption type to use.
     */
    public void setEncryptionKeyInfoInclusionType( final KeyInfoInclusionType encryptionKeyInfoInclusionType ) {
        this.encryptionKeyInfoInclusionType = encryptionKeyInfoInclusionType;
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

    public boolean isSignUsernameToken() {
        return signUsernameToken;
    }

    public void setSignUsernameToken(boolean signUsernameToken) {
        this.signUsernameToken = signUsernameToken;
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

    /**
     * Check if any included UsernameToken is to be signed and encrypted.
     * @see #setEncryptUsernameToken(boolean)
     * @return true if any UsernameToken included in the decorated message should be signed and encrypted.
     */
    public boolean isEncryptUsernameToken() {
        return encryptUsernameToken;
    }

    /**
     * Encrypt and sign any UsernameToken that is inserted into the message.  The UsernameToken will be moved
     * after the reference list so that it will be decrypted before being used by the recipient.  Encryption
     * will use the best-preference encryption source configured for this DecorationRequirements; at minimum,
     * a recipient certificate will enable a new EncryptedKey to be created.  The signature will use the
     * best-preference siganture source; at minimum, the EncryptedKey created for encryption can be reused
     * for signing.
     *
     * @param encryptUsernameToken  if true, sign and encrypt any UsernameToken that is included in the message.
     */
    public void setEncryptUsernameToken(boolean encryptUsernameToken) {
        this.encryptUsernameToken = encryptUsernameToken;
    }

    /**
     * If true, encryption and signing should be done using a pair of new DerivedKeyTokens, if applicable,
     * rather than using the same secret key to both sign and encrypt.  Note that WS-SecureConversation
     * will always use derived keys regardless of this setting.
     * <p/>
     * This is not necessary if an EncryptedKey will be used only for encryption, and only for a single message.
     *
     * @return true if Derived Keys should be used for signing and encryption rather than referencing
     *         the secret key in an EncryptedKey directly.
     */
    public boolean isUseDerivedKeys() {
        return useDerivedKeys;
    }

    /**
     * If true, encryption and signing should be done using a pair of new DerivedKeyTokens, if applicable,
     * rather than using the same secret key to both sign and encrypt.  Note that WS-SecureConversation
     * will always use derived keys regardless of this setting.
     * <p/>
     * This is not necessary if an EncryptedKey will be used only for encryption, and only for a single message.
     *
     * @param useDerivedKeys true if Derived Keys should be used for signing and encryption rather than referencing
     *         the secret key in an EncryptedKey directly.  Otherwise, derived keys will not be used unless
     *         necessary (ie, with WS-SecureConversation)
     */
    public void setUseDerivedKeys(boolean useDerivedKeys) {
        this.useDerivedKeys = useDerivedKeys;
    }

    public boolean isProtectTokens() {
        return protectTokens;
    }

    /**
     * If true, the token on which the signature is based will be included in the signature if it is
     * present in the message.  This is similar to the semantics of the sp:ProtectTokens assertion in WS-SecurityPolicy.
     *
     * @param protectTokens true to sign the BST (or other signing token); false to leave it unsigned.
     */
    public void setProtectTokens(boolean protectTokens) {
        this.protectTokens = protectTokens;
    }

    public boolean isEncryptSignature() {
        return encryptSignature;
    }

    /**
     * Set whether the signature should be encrypted.
     * 
     * @param encryptSignature true to encrypt any Signature we add to the message.
     */
    public void setEncryptSignature(boolean encryptSignature) {
        this.encryptSignature = encryptSignature;
    }

    /**
     * Gets the signature message digest string (i.e. SHA-1, SHA-256, etc).
     *
     * @return message digest value, or null if the decorator is being allowed to select a default.
     */
    public String getSignatureMessageDigest() {
        return signatureMessageDigest;
    }

    /**
     * Remove any configured recipient tokens.
     */
    public void clearTokens() {
        recipientCertificate = null;
        secureConversationSession = null;
        encryptedKey = null;
        encryptedKeyReferenceInfo = null;
        kerberosTicket = null;
        includeKerberosTicket = false;
        kerberosTicketId = null;
    }

    /**
     * Sets the message digest algorithm value intened for signing (i.e. SHA-1, SHA-256, etc).
     *
     * @param signatureMessageDigest the message digest algorithm to set, or null to allow the decorator to select a default.
     */
    public void setSignatureMessageDigest(String signatureMessageDigest) {
        this.signatureMessageDigest = signatureMessageDigest;
    }

    /**
     * @return the message digest alg name (ie, "SHA-384") for signature Reference values, or null if using the same as the signature method.
     */
    public String getSignatureReferenceMessageDigest() {
        return signatureReferenceMessageDigest;
    }

    /**
     * @param signatureReferenceMessageDigest set the digest algorithm name (ie, "SHA-384") for signature Reference values, if different from the
     *              signature method, or null to just use the same digest as will be used for the signature method.
     */
    public void setSignatureReferenceMessageDigest(String signatureReferenceMessageDigest) {
        this.signatureReferenceMessageDigest = signatureReferenceMessageDigest;
    }

    enum SecurityHeaderMustUnderstand { YES, NO, SKIP, DEFAULT }

    SecurityHeaderMustUnderstand getMustUnderstand() {
        return securityHeaderMustUnderstand;
    }

    public enum PreferredSigningTokenType { X509, ENCRYPTED_KEY, SAML_HOK, SCT  } // currently only X509, EncryptedKey, or SAML Holder-of-key can be preferred

    /**
     * Get the preferred signing token type if any.
     *
     * @return The preferred token type or null.
     */
    public PreferredSigningTokenType getPreferredSigningTokenType() {
        return preferredSigningTokenType;
    }

    /**
     * Set the preferred signing token type.
     *
     * @param preferredSigningTokenType The type to prefer, or null to clear the current preference
     */
    public void setPreferredSigningTokenType( final PreferredSigningTokenType preferredSigningTokenType ) {
        this.preferredSigningTokenType = preferredSigningTokenType;
    }

    /**
     * Determine if any WS-Addressing headers found in the target message should be signed.
     * 
     * @return true if found headers should be signed, false otherwise.
     */
    public WsaHeaderSigningStrategy getWsaHeaderSignStrategy() {
        return wsaHeaderSignStrategy;
    }

    /**
     * Set whether the WSS decorator should look for and sign any found WS-Addressing headers.

     * Null - default behaviour (sign WSA headers if found and something else is being signed)
     * True - always sign
     * False - never sign
     *
     * @param wsaHeaderSignStrategy true to sign any found WS-Addressing headers
     */
    public void setWsaHeaderSignStrategy(WsaHeaderSigningStrategy wsaHeaderSignStrategy) {
        this.wsaHeaderSignStrategy = wsaHeaderSignStrategy;
    }

    private X509Certificate recipientCertificate = null;
    private X509Certificate senderMessageSigningCertificate = null;
    private PrivateKey senderMessageSigningPrivateKey = null;
    private UsernameToken usernameTokenCredentials = null;
    private boolean signUsernameToken = false;
    private SamlAssertion senderSamlToken = null;
    private SecureConversationSession secureConversationSession = null;
    private boolean omitSecurityContextToken = false;
    private boolean includeTimestamp = true;
    private boolean signTimestamp;
    private boolean omitTimestamp;
    private boolean neverSignTimestamp;
    private Map<Element, ElementEncryptionConfig> elementsToEncrypt = new LinkedHashMap<>();
    private String encryptionAlgorithm = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    private String keyEncryptionAlgorithm = null;
    private Set<Element> elementsToSign = new LinkedHashSet<>();
    private Set<String> partsToSign = new LinkedHashSet<>();
    private boolean signPartHeaders = false;
    private NamespaceFactory namespaceFactory = new NamespaceFactory();
    private Date timestampCreatedDate = null;
    private long timestampTimeoutMillis = 0;
    private TimestampResolution timestampResolution = TimestampResolution.DEFAULT;
    private boolean securityHeaderReusable = false;
    private String securityHeaderActor = SoapConstants.L7_SOAP_ACTOR;
    private boolean securityHeaderActorNamespaced = ConfigFactory.getBooleanProperty( "com.l7tech.security.xml.decorator.soap.soapActorNamespaced", true );
    private SecurityHeaderMustUnderstand securityHeaderMustUnderstand = SecurityHeaderMustUnderstand.DEFAULT;
    private boolean includeSamlTokenInSignature = false;
    private KeyInfoInclusionType keyInfoInclusionType = KeyInfoInclusionType.CERT;
    private KeyInfoInclusionType encryptionKeyInfoInclusionType = KeyInfoInclusionType.STR_SKI;
    private byte[] encryptedKey = null;
    private KeyInfoDetails encryptedKeyReferenceInfo = null;
    private Set<String> signatureConfirmations = new HashSet<>();
    private KerberosServiceTicket kerberosTicket = null;
    private boolean includeKerberosTicket = false;
    private String kerberosTicketId = null;
    private boolean includeKerberosTicketId = false;
    private boolean encryptUsernameToken = false;
    private boolean encryptSignature = false;
    private boolean useDerivedKeys = false;
    private boolean suppressSamlStrTransform = false;
    private boolean protectTokens = false;
    private String signatureMessageDigest = null;
    private String signatureReferenceMessageDigest = null;
    private PreferredSigningTokenType preferredSigningTokenType = null;
    private WsSecurityVersion wssVersion = null;
    private WsaHeaderSigningStrategy wsaHeaderSignStrategy = DEFAULT_WSA_HEADER_SIGNING_BEHAVIOUR;

    public enum WsaHeaderSigningStrategy {
        NEVER_SIGN_WSA_HEADERS,
        ALWAYS_SIGN_WSA_HEADERS,
        //Default behaviour is to sign any found WSA headers when something else is also being signed.
        //this is pre 5.4.1 behaviour and is retained. Post 5.4.1 can also specify explicitly that headers should
        //always be signed or never signed.
        //Note: If WS-Addressing header elements were explicitly added by the WS-Addressing assertion, these will be
        //in the signature regardless of the signing strategy, just not any other headers which were already in the message.
        DEFAULT_WSA_HEADER_SIGNING_BEHAVIOUR
    }
}
