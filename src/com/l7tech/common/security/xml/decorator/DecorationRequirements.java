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

    public X509Certificate getSenderCertificate() {
        return senderCertificate;
    }

    public void setSenderCertificate(X509Certificate senderCertificate) {
        this.senderCertificate = senderCertificate;
    }

    public PrivateKey getSenderPrivateKey() {
        return senderPrivateKey;
    }

    public void setSenderPrivateKey(PrivateKey senderPrivateKey) {
        this.senderPrivateKey = senderPrivateKey;
    }

    public boolean isSignTimestamp() {
        return signTimestamp;
    }

    /**
     * Set whether a signed timestamp is required.<p>
     *
     * If this is true, a timestamp will be added to the document regardless of the
     * content of elementsToSign.<p>
     *
     * If this is false, a timestamp will added to the document only if elementsToSign
     * is non-empty.<p>
     *
     * Regardless of this setting, if a timestamp is added to the document it will always be signed,
     * either directly or indirectly.  It will be signed directly unless it will be covered by an
     * Envelope signature.<p>
     *
     */
    public void setSignTimestamp(boolean signTimestamp) {
        this.signTimestamp = signTimestamp;
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

    public void setSenderSamlToken(Element senderSamlToken) {
        this.senderSamlToken = senderSamlToken;
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
     * @param preferredSecurityNamespace the wsse namespace.
     */
    public void setPreferredSecurityNamespace(String preferredSecurityNamespace) {
        this.preferredSecurityNamespace = preferredSecurityNamespace;
    }

    /** @return The overridden created date to use for the timestamp, or null to allow it to default. */
    public Date getTimestampCreatedDate() {
        return timestampCreatedDate;
    }

    /** @param timestampCreatedDate a date to override the Created date to use for the timestamp, or null to allow it to default. */
    public void setTimestampCreatedDate(Date timestampCreatedDate) {
        this.timestampCreatedDate = timestampCreatedDate;
    }

    /** @return overridden timestamp lifetime in milliseconds, or 0 if it will be allowed to default. */
    public int getTimestampTimeoutMillis() {
        return timestampTimeoutMillis;
    }

    /** @param timestampTimeoutMillis overridden timestamp lifetime in milliseconds, or 0 if it will be allowed to default. */
    public void setTimestampTimeoutMillis(int timestampTimeoutMillis) {
        this.timestampTimeoutMillis = timestampTimeoutMillis;
    }

    private X509Certificate recipientCertificate = null;
    private X509Certificate senderCertificate = null;
    private LoginCredentials usernameTokenCredentials = null;
    private Element senderSamlToken = null;
    private SecureConversationSession secureConversationSession = null;
    private PrivateKey senderPrivateKey = null;
    private boolean signTimestamp;
    private Set elementsToEncrypt = new LinkedHashSet();
    private Set elementsToSign = new LinkedHashSet();
    private String preferredSecurityNamespace = SoapUtil.SECURITY_NAMESPACE;
    private String preferredWSUNamespace = SoapUtil.WSU_NAMESPACE;
    private Date timestampCreatedDate = null;
    private int timestampTimeoutMillis = 0;
}
