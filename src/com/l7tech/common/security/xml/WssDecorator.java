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

/**
 * Creates a Security header and decorates a message according to instructions passed in.
 *
 * @author mike
 */
public interface WssDecorator {
    public static class DecoratorException extends Exception {}

    void decorateMessage(Document message,
                         X509Certificate recipientCertificate,
                         X509Certificate senderCertificate,
                         PrivateKey senderPrivateKey,
                         boolean signTimestamp,
                         Element[] elementsToEncrypt,
                         Element[] elementsToSign)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException;
}
