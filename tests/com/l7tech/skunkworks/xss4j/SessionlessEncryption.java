/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.xss4j;

import com.ibm.dom.util.XPathCanonicalizer;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.EncryptionContext;
import com.ibm.xml.enc.KeyInfoResolver;
import com.ibm.xml.enc.type.*;
import com.l7tech.common.security.xml.SecurityContextTokenHandler;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.Key;
import java.security.Security;

/**
 * @author mike
 */
public class SessionlessEncryption {
    static {
        Security.addProvider(new BouncyCastleProvider());   // Can remove if BC registered in java.security
    }

    // ID for EncryptedData element
    private static final String id = "bodyId";
    // Namespace constants
    private static String soapNS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String wsseNS = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    private static final String xencNS = "http://www.w3.org/2001/04/xmlenc#";
    private static final String dsNS = "http://www.w3.org/2000/09/xmldsig#";

    private static final Xss4jWrapper xutil = new Xss4jWrapper();

    public static void main(String[] args) throws Exception {
        Document soapMsg = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        soapNS = soapMsg.getDocumentElement().getNamespaceURI();
        System.out.println("Using soap namespace: " + soapNS);
        Element bodyEl = (Element) soapMsg.getElementsByTagNameNS(soapNS, "Body").item(0);

        SecurityContextTokenHandler tokenHandler = SecurityContextTokenHandler.getInstance();
        byte[] sessionId = tokenHandler.generateNewSessionId();
        String sessionIdString = tokenHandler.sessionIdToURI(sessionId);

        // Create a template EncryptedData subtree. This contains KeyInfo, EncryptedKey, and CipherData elements
        // that hold the encrypted key.
        // It also includes a CipherData element for the actual encrypted message body.
        // As an alternative, we could read this template as a Document from a file.
        CipherData cipherDataInner = new CipherData();
        CipherValue cipherValueInner = new CipherValue();
        cipherDataInner.setCipherValue(cipherValueInner);
        KeyName keyName = new KeyName();
        keyName.setName(sessionIdString);
        KeyInfo keyInfoInner = new KeyInfo();
        keyInfoInner.addKeyName(keyName);

        EncryptionMethod encryptionMethodKey = new EncryptionMethod();
        encryptionMethodKey.setAlgorithm(EncryptionMethod.RSA_1_5);
        EncryptedKey encryptedKey = new EncryptedKey();
        encryptedKey.setEncryptionMethod(encryptionMethodKey);
        encryptedKey.setKeyInfo(keyInfoInner);
        encryptedKey.setCipherData(cipherDataInner);

        AlgorithmFactoryExtn algFact = new AlgorithmFactoryExtn();
        // Need KeyStoreKeyInfoResolver so encryptor can retrieve keys from keystore. Could similarily write
        // our own KeyInfoResolver that worked from a cert or public key directly. Needs more research
        KeyInfoResolver kiRes = new KeyInfoResolver() {
            public void setOperationMode(int i) {
            }

            public Key resolve(KeyInfo keyInfo, EncryptionMethod encryptionMethod) {
                try {
                    if (encryptionMethod.getAlgorithm().matches(".*tripledes.*")) {
                        return getTripleDesKey();
                    } else if (encryptionMethod.getAlgorithm().matches(".*aes.*")) {
                        return xutil.getSecretKey();
                    } else if (encryptionMethod.getAlgorithm().matches(".*rsa.*")) {
                        return xutil.getClientCertPrivateKey();
                    } else
                        throw new RuntimeException("unrecognized encryption method");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        kiRes.setOperationMode(KeyInfoResolver.ENCRYPT_MODE);

        EncryptionContext ecInner = new EncryptionContext();
        ecInner.setAlgorithmFactory(algFact);
        ecInner.setKeyInfoResolver(kiRes);
        ecInner.setData(getTripleDesKey());
        final Element encryptedKeyEl = encryptedKey.createElement(soapMsg, true);
        ecInner.setEncryptedType(encryptedKeyEl, EncryptedData.CONTENT, null, null);

        ecInner.encrypt();
        encryptedKey.setCipherData(new CipherData((Element) encryptedKeyEl.getElementsByTagName("CipherData").item(0)));
        //ecInner.replace();
        //encryptedKey.setCipherData();

        CipherData cipherDataOuter = new CipherData();
        cipherDataOuter.setCipherValue(new CipherValue());
        KeyInfo keyInfoOuter = new KeyInfo();
        keyInfoOuter.addEncryptedKey(encryptedKey);

        EncryptionMethod encryptionMethodData = new EncryptionMethod();
        encryptionMethodData.setAlgorithm(EncryptionMethod.AES128_CBC);
        //encryptionMethodData.setAlgorithm(EncryptionMethod.TRIPLEDES_CBC);
        EncryptedData encryptedData = new EncryptedData();
        encryptedData.setId(id);    // Id="bodyId" attribute in SOAP <Body> element. Used when decrypting
        encryptedData.setEncryptionMethod(encryptionMethodData);
        encryptedData.setKeyInfo(keyInfoOuter);
        encryptedData.setCipherData(cipherDataOuter);
        Element encryptedDataEl = encryptedData.createElement(soapMsg, true);

        // Create encryption context and encrypt the body subtree
        EncryptionContext ec = new EncryptionContext();
        ec.setAlgorithmFactory(algFact);
        ec.setKeyInfoResolver(kiRes);
        ec.setData(bodyEl);
        ec.setEncryptedType(encryptedDataEl, EncryptedData.CONTENT, null, null);

        ec.encrypt();
        ec.replace();
        // Insert a WSS style header with a ReferenceList refering to the EncryptedData element
        addWSSHeader(soapMsg);

        // Output encrypted to console. Demo alternative output method.
        XmlUtil.documentToOutputStream(soapMsg, System.out);

        // Output to file
        Writer wr = new OutputStreamWriter(System.out, "UTF-8");
        XPathCanonicalizer.serializeAll(soapMsg, true, wr);
        wr.close();
        System.out.println("Encryption of header element done.");

    }

    private static SecretKey getTripleDesKey() {
        final byte[] keyBytes = new byte[24];
        System.arraycopy(xutil.getSymmetricKeyBytes(), 0, keyBytes, 0, 24);
        return new SecretKey() {
            public byte[] getEncoded() {
                return keyBytes;
            }

            public String getAlgorithm() {
                return "DESede";
            }

            public String getFormat() {
                return "RAW";
            }

        };
    }

    public static void addWSSHeader(Document soapMsg) {
        // Add new namespaces to Envelope element, as per spec.
        Element rootEl = soapMsg.getDocumentElement();
        rootEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:wsse", wsseNS);
        rootEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xenc", xencNS);
        rootEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ds", dsNS);

        // Add Security element to header, referencing the encrypted body
        Element dataRefEl = soapMsg.createElementNS(xencNS, "xenc:DataReference");
        dataRefEl.setAttribute("URI", id);

        Element refEl = soapMsg.createElementNS(xencNS, "xenc:ReferenceList");
        refEl.appendChild(dataRefEl);

        Element securityEl = soapMsg.createElementNS(wsseNS, "wsse:Security");
        securityEl.appendChild(refEl);

        Element hdrEl = (Element) soapMsg.getElementsByTagNameNS(soapNS, "Header").item(0);
        if (hdrEl == null) {
            // No header yet, create one
            hdrEl = soapMsg.createElementNS(soapNS, "Header");
            Element bodyEl = (Element) soapMsg.getElementsByTagNameNS(soapNS, "Body").item(0);
            if (bodyEl == null)
                soapMsg.getDocumentElement().appendChild(hdrEl);
            else
                soapMsg.getDocumentElement().insertBefore(hdrEl, bodyEl);
        }
        hdrEl.appendChild(securityEl);
    }

}
