/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.AesKey;
import com.ibm.xml.enc.type.*;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.EncryptionContext;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.KeyInfoResolvingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    public static final int TIMESTAMP_TIMOUT_SEC = 300;
    public static final String KEYID_VALUETYPE_SKI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier";

    private static class Context {
        SecureRandom rand = new SecureRandom();
        long count = 0;
        Map idToElementCache = new HashMap();
    }

    private static class CausedDecoratorException extends DecoratorException {
        CausedDecoratorException(String message) {
            super();
            initCause(new RuntimeException(message));
        }

        CausedDecoratorException(String message, Throwable cause) {
            super();
            initCause(new RuntimeException(message, cause));
        }

        CausedDecoratorException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    public void decorateMessage(Document message,
                                X509Certificate recipientCertificate,
                                X509Certificate senderCertificate,
                                PrivateKey senderPrivateKey,
                                Element[] elementsToEncrypt,
                                Element[] elementsToSign)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException
    {
        Context c = new Context();

        Element securityHeader = createSecurityHeader(message);

        addTimestamp(securityHeader);

        if (senderCertificate != null)
            addX509BinarySecurityToken(securityHeader, senderCertificate);

        // todo encrypt
        addEncryptedKey(c, securityHeader, recipientCertificate, elementsToEncrypt);

        // todo sign
        Element signature = addSignature(c, senderCertificate, senderPrivateKey, elementsToSign, securityHeader);
    }

    private Element addSignature(Context c, X509Certificate senderCertificate, PrivateKey senderPrivateKey,
                                 Element[] elementsToSign, Element securityHeader) {
        // make sure all elements already have an id
        for (int i = 0; i < elementsToSign.length; i++) {
            getOrCreateWsuId(c, elementsToSign[i]);
        }
        return null;
    }

    private Element addEncryptedKey(Context c,
                                    Element securityHeader,
                                    X509Certificate recipientCertificate,
                                    Element[] elementsToEncrypt)
            throws GeneralSecurityException, CausedDecoratorException
    {
        Document soapMsg = securityHeader.getOwnerDocument();

        // Make a bulk encryption key
        byte[] keyBytes = new byte[16];
        c.rand.nextBytes(keyBytes);

        // Stuff it into an EncryptedKey
        String xencNs = SoapUtil.XMLENC_NS;
        String xenc = "xenc";
        Element encryptedKey = soapMsg.createElementNS(xencNs, SoapUtil.ENCRYPTEDKEY_EL_NAME);
        encryptedKey.setPrefix(xenc);
        encryptedKey.setAttribute("xmlns:" + xenc, xencNs);
        securityHeader.appendChild(encryptedKey);

        Element encryptionMethod = soapMsg.createElementNS(xencNs, "EncryptionMethod");
        encryptionMethod.setPrefix(xenc);
        encryptionMethod.setAttribute("Algorithm", SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO);
        encryptedKey.appendChild(encryptionMethod);

        // todo - need to do something reasonable here when there is no SKI in the recipient cert.
        // Options include omitting the KeyInfo (as we are doing now) and including a copy of the entire cert
        byte[] recipSki = recipientCertificate.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
        if (recipSki != null)
            addKeyInfo(encryptedKey, recipSki);

        Element cipherData = soapMsg.createElementNS(xencNs, "CipherData");
        cipherData.setPrefix(xenc);
        encryptedKey.appendChild(cipherData);

        Element cipherValue = soapMsg.createElementNS(xencNs,  "CipherValue");
        cipherValue.setPrefix(xenc);
        cipherValue.appendChild(soapMsg.createTextNode(encryptWithRsa(keyBytes, recipientCertificate.getPublicKey())));
        cipherData.appendChild(cipherValue);

        Element referenceList = soapMsg.createElementNS(xencNs, "ReferenceList");
        referenceList.setPrefix(xenc);
        encryptedKey.appendChild(referenceList);

        for (int i = 0; i < elementsToEncrypt.length; i++) {
            Element element = elementsToEncrypt[i];
            Element encryptedElement = encryptElement(c, element, keyBytes);

            Element dataReference = soapMsg.createElementNS(xencNs, "DataReference");
            dataReference.setPrefix(xenc);
            dataReference.setAttribute("URI", "#" + getOrCreateWsuId(c, encryptedElement));
            referenceList.appendChild(dataReference);

            // todo go and encrypt this element now
        }

        return encryptedKey;
    }

    /**
     * Encrypt the specified element.  Returns the new EncryptedData element.
     * @param c
     * @param element
     * @param keyBytes
     * @return the EncryptedData element that replaces the specified element.
     */
    private Element encryptElement(Context c, Element element, byte[] keyBytes)
            throws CausedDecoratorException, GeneralSecurityException
    {
        Document soapMsg = element.getOwnerDocument();

        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());
        KeyInfo keyInfo = new KeyInfo();
        KeyName kn = new KeyName();
        kn.setName("thekey");
        keyInfo.addKeyName(kn);

        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(EncryptionMethod.AES128_CBC);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setKeyInfo(keyInfo);
        Element encDataElement = null;
        try {
            encDataElement = encData.createElement(soapMsg, true);
        } catch (StructureException e) {
            throw new CausedDecoratorException(e);
        }

        // Create encryption context and encrypt the header subtree
        EncryptionContext ec = new EncryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        af.setProvider(JceProvider.getSymmetricJceProvider().getName());
        ec.setAlgorithmFactory(af);
        ec.setEncryptedType(encDataElement, EncryptedData.CONTENT, null, null);

        ec.setData(element);
        ec.setKey(new AesKey(keyBytes, 128));

        try {
            ec.encrypt();
            ec.replace();
        } catch (KeyInfoResolvingException e) {
            throw new CausedDecoratorException(e); // can't happen
        } catch (StructureException e) {
            throw new CausedDecoratorException(e); // shouldn't happen
        } catch (IOException e) {
            throw new CausedDecoratorException(e); // shouldn't happen
        }

        return ec.getEncryptedTypeAsElement();
    }

    /**
     * Get the wsu:Id for the specified element.  If it doesn't already have a wsu:Id attribute a new one
     * is created for the element.
     * @param c
     * @param element
     * @return
     */
    private String getOrCreateWsuId(Context c, Element element) {
        String id = SoapUtil.getElementWsuId(element);
        if (id == null) {
            id = createWsuId(c, element);
        }
        return id;
    }

    private String encryptWithRsa(byte[] keyBytes, PublicKey publicKey) throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] paddedKeyBytes = padSymmetricKeyForRsaEncryption(keyBytes);
        byte[] encrypted = rsa.doFinal(paddedKeyBytes);
        return HexUtils.encodeBase64(encrypted, true);
    }

    private byte[] padSymmetricKeyForRsaEncryption(byte[] keyBytes) {
        // todo, if necessary
        return keyBytes;
    }

    private void addKeyInfo(Element encryptedKey, byte[] recipSki) {
        Document soapMsg = encryptedKey.getOwnerDocument();
        String wsseNs = encryptedKey.getParentNode().getNamespaceURI();
        String wssePrefix = encryptedKey.getParentNode().getPrefix();

        Element keyInfo = soapMsg.createElementNS(SoapUtil.DIGSIG_URI, "KeyInfo");
        keyInfo.setPrefix("dsig");
        keyInfo.setAttribute("xmlns:dsig", SoapUtil.DIGSIG_URI);
        encryptedKey.appendChild(keyInfo);

        Element securityTokenRef = soapMsg.createElementNS(wsseNs, SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        securityTokenRef.setPrefix(wssePrefix);
        keyInfo.appendChild(securityTokenRef);

        Element keyId = soapMsg.createElementNS(wsseNs, SoapUtil.KEYIDENTIFIER_EL_NAME);
        keyId.setPrefix(wssePrefix);
        securityTokenRef.appendChild(keyId);

        keyId.setAttribute("ValueType", KEYID_VALUETYPE_SKI);
        String recipSkiB64 = HexUtils.encodeBase64(recipSki, true);
        keyId.appendChild(soapMsg.createTextNode(recipSkiB64));
    }

    /**
     * Generate a wsu:Id for the specified element, adds it to the element, and returns it.
     * @param c
     * @param element
     * @return
     */
    private String createWsuId(Context c, Element element) {
        byte[] randbytes = new byte[16];
        c.rand.nextBytes(randbytes);
        String id = element.getLocalName() + "-" + c.count++ + "-" + HexUtils.hexDump(randbytes);

        if (c.idToElementCache.get(id) != null)
            throw new IllegalStateException("Duplicate wsu:ID generated: " + id); // can't happen

        c.idToElementCache.put(id, element);

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
