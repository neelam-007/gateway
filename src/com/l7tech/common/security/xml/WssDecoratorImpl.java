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

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    public Document decorateMessage(Document message, X509Certificate recipientCertificate, X509Certificate senderCertificate, PrivateKey senderPrivateKey, Element[] elementsToEncrypt, Element[] elementsToSign) {
        return null;
    }
}
