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
import java.util.Iterator;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    public Document decorateMessage(Document message,
                                    X509Certificate recipientCertificate,
                                    X509Certificate senderCertificate,
                                    PrivateKey senderPrivateKey,
                                    Element[] elementsToEncrypt,
                                    Element[] elementsToSign)
        throws InvalidDocumentFormatException, GeneralSecurityException
    {
        Element securityHeader = createSecurityHeader(message);

        // todo finish this
        return null;
    }

    private Element createSecurityHeader(Document message) throws InvalidDocumentFormatException {
        // Wrap any existing header
        Element oldSecurity = SoapUtil.getSecurityElement(message);
        if (oldSecurity != null) {
            // todo -- support more than one layer of actor-wrapped security header
            oldSecurity.removeAttribute(SoapUtil.ACTOR_ATTR_NAME);
            oldSecurity.removeAttribute(SoapUtil.ROLE_ATTR_NAME);
            for (Iterator i = SoapUtil.ENVELOPE_URIS.iterator(); i.hasNext();) {
                String ns = (String)i.next();
                oldSecurity.removeAttributeNS(ns, SoapUtil.ACTOR_ATTR_NAME);
                oldSecurity.removeAttributeNS(ns, SoapUtil.ROLE_ATTR_NAME);
            }
            oldSecurity.setAttributeNS(message.getDocumentElement().getNamespaceURI(),
                                       message.getDocumentElement().getPrefix() + ":" + SoapUtil.ACTOR_ATTR_NAME,
                                       ACTOR_LAYER7_WRAPPED);
        }


        Element security = message.createElementNS(SoapUtil.SECURITY_NAMESPACE, SoapUtil.SECURITY_EL_NAME);
        security.setPrefix("wsse"); // todo - figure out how to find an existing namespace decl
        // todo - figure out how to ensure that this prefix is unique

        Element header = SoapUtil.getHeaderElement(message);
        Element firstHeader = XmlUtil.findFirstChildElement(header);
        if (firstHeader == null)
            header.appendChild(security);
        else
            header.insertBefore(security, firstHeader);

        return header;
    }

    private static final String ACTOR_LAYER7_WRAPPED = "http://www.layer7tech.com/ws/actor-wrapped";
}
