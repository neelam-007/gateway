/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * Creates a Security header and decorates a message according to instructions passed in.
 *
 * @author mike
 */
public interface WssDecorator {
    public static class DecoratorException extends Exception {}

    /**
     * Decorate a soap message with WSS style security.
     * @param message the soap message to decorate
     * @param recipientCertificate todo
     * @param senderCertificate todo
     * @param senderPrivateKey todo
     * @param signTimestamp todo
     * @param elementsToEncrypt todo
     * @param elementsToSign todo
     * @param usernameTokenCredentials optional provide cleartext credentials here to be included in a usernametoken
     */ 
    void decorateMessage(Document message,
                         X509Certificate recipientCertificate,
                         X509Certificate senderCertificate,
                         PrivateKey senderPrivateKey,
                         boolean signTimestamp,
                         Element[] elementsToEncrypt,
                         Element[] elementsToSign,
                         LoginCredentials usernameTokenCredentials)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException;
}
