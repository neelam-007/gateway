/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

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

        public void setSignTimestamp(boolean signTimestamp) {
            this.signTimestamp = signTimestamp;
        }
        /**
         * populate this with Element objects
         */
        public List getElementsToEncrypt() {
            return elementsToEncrypt;
        }

        /**
         * populate this with Element objects
         */
        public List getElementsToSign() {
            return elementsToSign;
        }

        public LoginCredentials getUsernameTokenCredentials() {
            return usernameTokenCredentials;
        }

        public void setUsernameTokenCredentials(LoginCredentials usernameTokenCredentials) {
            this.usernameTokenCredentials = usernameTokenCredentials;
        }

        private X509Certificate recipientCertificate = null;
        private X509Certificate senderCertificate = null;
        private PrivateKey senderPrivateKey = null;
        private boolean signTimestamp;
        private List elementsToEncrypt = new ArrayList();
        private List elementsToSign = new ArrayList();
        private LoginCredentials usernameTokenCredentials = null;
    }

    /**
     * Decorate a soap message with WSS style security.
     * @param message the soap message to decorate
     * @param decorationRequirements details of what needs to be processed
     */
    void decorateMessage(Document message, DecorationRequirements decorationRequirements)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException;
}
