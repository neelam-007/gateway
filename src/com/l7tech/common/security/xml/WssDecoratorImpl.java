/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    public static final int TIMESTAMP_TIMOUT_SEC = 300;

    private static class Context {
        SecureRandom rand = new SecureRandom();
        long count = 0;
    }

    public void decorateMessage(Document message,
                                X509Certificate recipientCertificate,
                                X509Certificate senderCertificate,
                                PrivateKey senderPrivateKey,
                                Element[] elementsToEncrypt,
                                Element[] elementsToSign)
            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        Context c = new Context();

        Element securityHeader = createSecurityHeader(message);
        String securityHeaderId = createWsuId(c, securityHeader); // todo create Ids lazily as we find they are needed

        Element timestamp = addTimestamp(securityHeader);
        String timestampId = createWsuId(c, timestamp);

        if (senderCertificate != null) {
            Element securityToken = addX509BinarySecurityToken(securityHeader, senderCertificate);
            String securityTokenId = createWsuId(c, securityToken);
        }

        // todo encrypt
        //Element encryptedKey = addEncryptedKey();

        // todo sign
        //Element signature = addSignature(senderCertificate, senderPrivateKey);
    }

    /**
     * Generate a wsu:Id for the specified element, adds it to the element, and returns it.
     * @param c
     * @param element
     * @return
     */
    private String createWsuId(Context c, Element element) {
        String id = element.getLocalName() + "-" + c.count++ + "-" + c.rand.nextLong();
        element.setAttributeNS(SoapUtil.WSU_NAMESPACE, "wsu:Id", id);

        // todo use better logic to decide if wsu needs to be declared here
        if (element.getAttribute("xmlns:wsu").length() < 1)
            element.setAttribute("xmlns:wsu", SoapUtil.WSU_NAMESPACE);

        return id;
    }

    private Element addTimestamp(Element securityHeader) {
        Document message = securityHeader.getOwnerDocument();
        Element timestamp = message.createElementNS(SoapUtil.WSU_NAMESPACE,
                                                    SoapUtil.TIMESTAMP_EL_NAME);
        timestamp.setPrefix("wsu");
        timestamp.setAttribute("xmlns:" + timestamp.getPrefix(), timestamp.getNamespaceURI());
        securityHeader.appendChild(timestamp);

        Calendar now = Calendar.getInstance();
        timestamp.appendChild(makeTimestampChildElement(timestamp, SoapUtil.CREATED_EL_NAME, now.getTime()));
        now.add(Calendar.SECOND, TIMESTAMP_TIMOUT_SEC);
        timestamp.appendChild(makeTimestampChildElement(timestamp, SoapUtil.EXPIRES_EL_NAME, now.getTime()));
        return timestamp;
    }

    private Element makeTimestampChildElement(Element timestamp, String createdElName, Date time) {
        Document factory = timestamp.getOwnerDocument();
        Element element = factory.createElementNS(timestamp.getNamespaceURI(), createdElName);
        element.setPrefix(timestamp.getPrefix());
        DateFormat dateFormat = new SimpleDateFormat(SoapUtil.DATE_FORMAT_PATTERN);
        dateFormat.setTimeZone(SoapUtil.DATE_FORMAT_TIMEZONE);
        element.appendChild(factory.createTextNode(dateFormat.format(time)));
        return element;
    }

    private Element addX509BinarySecurityToken(Element securityHeader, X509Certificate certificate)
            throws CertificateEncodingException
    {
        Document factory = securityHeader.getOwnerDocument();
        Element element = factory.createElementNS(securityHeader.getNamespaceURI(),
                                                  SoapUtil.BINARYSECURITYTOKEN_EL_NAME);
        element.setPrefix(securityHeader.getPrefix());
        element.setAttribute("ValueType", element.getPrefix() + ":X509v3");
        element.setAttribute("EncodingType", element.getPrefix() + ":Base64Binary");
        element.appendChild(factory.createTextNode("\n" + HexUtils.encodeBase64(certificate.getEncoded()) + "\n"));
        securityHeader.appendChild(element);
        return element;
    }

    private Element createSecurityHeader(Document message) throws InvalidDocumentFormatException {
        // Wrap any existing header
        Element oldSecurity = SoapUtil.getSecurityElement(message);
        if (oldSecurity != null) {
            // todo -- support more than one layer of actor-wrapped security header
            SoapUtil.removeSoapAttr(oldSecurity, SoapUtil.ACTOR_ATTR_NAME);
            SoapUtil.removeSoapAttr(oldSecurity, SoapUtil.ROLE_ATTR_NAME);
            SoapUtil.setSoapAttr(message, oldSecurity, SoapUtil.ACTOR_ATTR_NAME, SoapUtil.ACTOR_LAYER7_WRAPPED);
        }

        return SoapUtil.makeSecurityElement(message);
    }
}
