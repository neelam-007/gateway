/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Creates a Security header and decorates a message according to instructions passed in.
 *
 * @author mike
 */
public interface WssDecorator {
    public static class DecoratorException extends Exception {}

    class DecorationRequirements {
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

        public SecureConversationSession getSecureConversationSession() {
            return secureConversationSession;
        }

        public void setSecureConversationSession(SecureConversationSession secureConversationSession) {
            this.secureConversationSession = secureConversationSession;
        }

        public interface SecureConversationSession {
            String getId();
            SecretKey getSecretKey();
            // todo, this makes no sense --fla
            int getGeneration();
            // todo, this makes no sense --fla
            int getLength();
        }

        private X509Certificate recipientCertificate = null;
        private X509Certificate senderCertificate = null;
        private LoginCredentials usernameTokenCredentials = null;
        private SecureConversationSession secureConversationSession = null;
        private PrivateKey senderPrivateKey = null;
        private boolean signTimestamp;
        private Set elementsToEncrypt = new LinkedHashSet();
        private Set elementsToSign = new LinkedHashSet();
    }

    /**
     * Decorate a soap message with WSS style security.
     * @param message the soap message to decorate
     * @param decorationRequirements details of what needs to be processed
     */
    void decorateMessage(Document message, DecorationRequirements decorationRequirements)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException;
}
