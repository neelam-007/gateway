/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.EncryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.CipherData;
import com.ibm.xml.enc.type.CipherValue;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
                                boolean signTimestamp,
                                Element[] elementsToEncrypt,
                                Element[] elementsToSign)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException
    {
        Context c = new Context();

        Element securityHeader = createSecurityHeader(message);

        Element timestamp = addTimestamp(securityHeader);
        if (signTimestamp) {
            List signList = new ArrayList(Arrays.asList(elementsToSign));
            signList.add(0, timestamp);
            elementsToSign = (Element[])signList.toArray(new Element[0]);
        }

        Element bst = null;
        if (senderCertificate != null)
            bst = addX509BinarySecurityToken(securityHeader, senderCertificate);

        if (elementsToEncrypt.length > 0)
            addEncryptedKey(c, securityHeader, recipientCertificate, elementsToEncrypt);

        if (elementsToSign.length > 0)
            addSignature(c, senderCertificate, senderPrivateKey, elementsToSign, securityHeader, bst);
    }

    private boolean isWrappingOrSame(Element potentialParent, Element element) {
        if (potentialParent == element) return true;
        Element parent = (Element)element.getParentNode();
        while (parent != null) {
            if (potentialParent == parent) return true;
            if (!(parent.getParentNode() instanceof Element)) break;
            else parent = (Element)parent.getParentNode();
        }
        return false;
    }

    private Element addSignature(Context c, X509Certificate senderCertificate, PrivateKey senderPrivateKey,
                                 Element[] elementsToSign, Element securityHeader,
                                 Element binarySecurityToken) throws DecoratorException {

        if (elementsToSign == null || elementsToSign.length < 1) return null;

        // make sure all elements already have an id
        String[] sigedIds = new String[elementsToSign.length];
        for (int i = 0; i < elementsToSign.length; i++) {
            sigedIds[i] = getOrCreateWsuId(c, elementsToSign[i]);
        }

        String signaturemethod = null;
        if (senderPrivateKey instanceof RSAPrivateKey)
            signaturemethod = SignatureMethod.RSA;
        else if (senderPrivateKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else {
            throw new CausedDecoratorException("Private Key type not supported " + senderPrivateKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementsToSign[0].getOwnerDocument(),
                                                           XSignature.SHA1, Canonicalizer.W3C2, signaturemethod);
        template.setPrefix("ds");
        for (int i = 0; i < elementsToSign.length; i++) {
            Reference ref = template.createReference("#" + sigedIds[i]);
            if (isWrappingOrSame(elementsToSign[i], securityHeader)) {
                ref.addTransform(Transform.ENVELOPED);
            }
            ref.addTransform(Transform.W3CC14N2);
            template.addReference(ref);
        }
        Element emptySignatureElement = template.getSignatureElement();

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        // add following KeyInfo
        // <KeyInfo>
        //  <wsse:SecurityTokenReference>
        //      <wsse:Reference	URI="#bstId"
        //                      ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3" />
        //      </wsse:SecurityTokenReference>
        // </KeyInfo>
        // todo, fix hard coded strings
        String bstId = getOrCreateWsuId(c, binarySecurityToken);
        String wssePrefix = securityHeader.getPrefix();
        Element keyInfoEl = securityHeader.getOwnerDocument().createElement("KeyInfo");
        Element secTokRefEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
                                                                                "SecurityTokenReference");
        secTokRefEl.setPrefix(wssePrefix);
        Element refEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
                                                                          "Reference");
        refEl.setPrefix(wssePrefix);
        secTokRefEl.appendChild(refEl);
        keyInfoEl.appendChild(secTokRefEl);
        refEl.setAttribute("URI", "#" + bstId);
        refEl.setAttribute("ValueType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3");



        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                return SoapUtil.getElementByWsuId(doc, s);
            }
        });
        try {
            sigContext.sign(emptySignatureElement, senderPrivateKey);
        } catch (XSignatureException e) {
            throw new CausedDecoratorException(e);
        }

        Element signatureElement = (Element)securityHeader.appendChild(emptySignatureElement);
        signatureElement.appendChild(keyInfoEl);

        return signatureElement;
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

        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(EncryptionMethod.AES128_CBC);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
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

        Element encryptedData = ec.getEncryptedTypeAsElement();
        return encryptedData;
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

        String wsuPrefix = XmlUtil.getOrCreatePrefixForNamespace(element, SoapUtil.WSU_NAMESPACE, "wsu");
        element.setAttributeNS(SoapUtil.WSU_NAMESPACE, wsuPrefix + ":Id", id);

        return id;
    }

    private Element addTimestamp(Element securityHeader) {
        Document message = securityHeader.getOwnerDocument();
        Element timestamp = message.createElementNS(SoapUtil.WSU_NAMESPACE,
                                                    SoapUtil.TIMESTAMP_EL_NAME);
        securityHeader.appendChild(timestamp);
        timestamp.setPrefix(XmlUtil.getOrCreatePrefixForNamespace(timestamp, SoapUtil.WSU_NAMESPACE, "wsu"));

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

        Element securityHeader = SoapUtil.makeSecurityElement(message);
        // Make sure wsu is declared to save duplication
        XmlUtil.getOrCreatePrefixForNamespace(securityHeader, SoapUtil.WSU_NAMESPACE, "wsu");
        return securityHeader;
    }
}
