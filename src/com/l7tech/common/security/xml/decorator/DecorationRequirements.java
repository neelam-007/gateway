/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.decorator;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Element;

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
        this.signTimestamp = true;
    }

    /**
     * populate this with Element objects
     */
    public Set getElementsToEncrypt() {
        return elementsToEncrypt;
    }

    /**
     * populate this with Element objects
     */
    public Set getElementsToSign() {
        return elementsToSign;
    }

    public LoginCredentials getUsernameTokenCredentials() {
        return usernameTokenCredentials;
    }

    public void setUsernameTokenCredentials(LoginCredentials usernameTokenCredentials) {
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

    private X509Certificate recipientCertificate = null;
    private X509Certificate senderMessageSigningCertificate = null;
    private PrivateKey senderMessageSigningPrivateKey = null;
    private LoginCredentials usernameTokenCredentials = null;
    private Element senderSamlToken = null;
    private SecureConversationSession secureConversationSession = null;
    private boolean signTimestamp;
    private Set elementsToEncrypt = new LinkedHashSet();
    private Set elementsToSign = new LinkedHashSet();
    private String preferredSecurityNamespace = SoapUtil.SECURITY_NAMESPACE;
    private String preferredWSUNamespace = SoapUtil.WSU_NAMESPACE;
    private Date timestampCreatedDate = null;
    private int timestampTimeoutMillis = 0;
    private String securityHeaderActor = null;
    private boolean includeSamlTokenInSignature = false;
}
