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
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    private static final Logger logger = Logger.getLogger(WssDecorator.class.getName());

    public static final int TIMESTAMP_TIMOUT_SEC = 300;

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

    /**
     * Decorate a soap message with WSS style security.
     * @param message the soap message to decorate
     */
    public void decorateMessage(Document message, DecorationRequirements decorationRequirements)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException
    {
        Context c = new Context();

        Element securityHeader = createSecurityHeader(message);

        Element timestamp = addTimestamp(securityHeader);
        if (decorationRequirements.isSignTimestamp()) {
            Set signList = decorationRequirements.getElementsToSign();
            signList.add(timestamp);
        }

        if (decorationRequirements.getUsernameTokenCredentials() != null &&
            decorationRequirements.getUsernameTokenCredentials().getFormat() == CredentialFormat.CLEARTEXT) {
            createUsernameToken(securityHeader, decorationRequirements.getUsernameTokenCredentials());
        }

        Element bst = null;
        if (decorationRequirements.getSenderCertificate() != null)
            bst = addX509BinarySecurityToken(securityHeader, decorationRequirements.getSenderCertificate());

        Element signature = null;
        if (decorationRequirements.getElementsToSign().size() > 0) {
            if (decorationRequirements.getSenderPrivateKey() == null)
                throw new IllegalArgumentException("Signing is requested, but senderPrivateKey is null");
            if (bst == null)
                throw new IllegalArgumentException("Signing is requested, but no senderCertificate was supplied");            
            signature = addSignature(c,
                                     decorationRequirements.getSenderPrivateKey(),
                                     (Element[])(decorationRequirements.getElementsToSign().toArray(new Element[0])),
                                     securityHeader,
                                     bst);
        }
        
        if (decorationRequirements.getElementsToEncrypt().size() > 0) {
            if (decorationRequirements.getRecipientCertificate() == null)
                throw new IllegalArgumentException("Encryption is requested, but recipientCertificate is null");
            addEncryptedKey(c,
                            securityHeader,
                            decorationRequirements.getRecipientCertificate(),
                            (Element[])(decorationRequirements.getElementsToEncrypt().toArray(new Element[0])),
                            signature);
        }

    }

    // todo: replace use of this method with the effectively-identical XmlUtil.isElementAncestor()
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

    private Element addSignature(final Context c, PrivateKey senderPrivateKey,
                                 Element[] elementsToSign, Element securityHeader,
                                 Element binarySecurityToken) throws DecoratorException {

        if (elementsToSign == null || elementsToSign.length < 1) return null;

        // make sure all elements already have an id
        String[] sigedIds = new String[elementsToSign.length];
        for (int i = 0; i < elementsToSign.length; i++) {
            sigedIds[i] = getOrCreateWsuId(c, elementsToSign[i], null);
        }

        String signaturemethod = null;
        if (senderPrivateKey instanceof RSAPrivateKey)
            signaturemethod = SignatureMethod.RSA;
        else if (senderPrivateKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else {
            throw new CausedDecoratorException("Private Key type not supported " +
                                               senderPrivateKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementsToSign[0].getOwnerDocument(),
                                                           XSignature.SHA1, Canonicalizer.EXCLUSIVE, signaturemethod);
        template.setPrefix("ds");
        for (int i = 0; i < elementsToSign.length; i++) {
            Reference ref = template.createReference("#" + sigedIds[i]);
            if (isWrappingOrSame(elementsToSign[i], securityHeader)) {
                ref.addTransform(Transform.ENVELOPED);
            }
            ref.addTransform(Transform.C14N_EXCLUSIVE);
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
        String bstId = getOrCreateWsuId(c, binarySecurityToken, null);
        String wssePrefix = securityHeader.getPrefix();
        Element keyInfoEl = securityHeader.getOwnerDocument().createElementNS(emptySignatureElement.getNamespaceURI(),
                                                                              "KeyInfo");
        keyInfoEl.setPrefix("ds");
        Element secTokRefEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
                                                                                "SecurityTokenReference");
        secTokRefEl.setPrefix(wssePrefix);
        Element refEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
                                                                          "Reference");
        refEl.setPrefix(wssePrefix);
        secTokRefEl.appendChild(refEl);
        keyInfoEl.appendChild(secTokRefEl);
        refEl.setAttribute("URI", "#" + bstId);
        refEl.setAttribute("ValueType", SoapUtil.VALUETYPE_X509);



        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                Element e = (Element)c.idToElementCache.get(s);
                if (e != null)
                    return e;
                e = SoapUtil.getElementByWsuId(doc, s);
                if (e != null)
                    c.idToElementCache.put(s, e);
                return e;
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

    private Element createUsernameToken(Element securityHeader, LoginCredentials usernameTokenCredentials) {
        // What this element looks like:
        // <wsse:UsernameToken>
        //    <wsse:Username>username</wsse:Username>
        //    <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">password</wsse:Password>
        // </wsse:UsernameToken>
        // create elements
        Document doc = securityHeader.getOwnerDocument();
        Element untokEl = doc.createElementNS(securityHeader.getNamespaceURI(), "UsernameToken");
        untokEl.setPrefix(securityHeader.getPrefix());
        Element usernameEl = doc.createElementNS(securityHeader.getNamespaceURI(), "Username");
        usernameEl.setPrefix(untokEl.getPrefix());
        Element passwdEl = doc.createElementNS(securityHeader.getNamespaceURI(), "Password");
        passwdEl.setPrefix(untokEl.getPrefix());
        // attach them
        securityHeader.appendChild(untokEl);
        untokEl.appendChild(usernameEl);
        untokEl.appendChild(passwdEl);
        // fill in username value
        Text txtNode = doc.createTextNode(usernameTokenCredentials.getLogin());
        usernameEl.appendChild(txtNode);
        // fill in password value and type
        txtNode = doc.createTextNode(new String(usernameTokenCredentials.getCredentials()));
        passwdEl.appendChild(txtNode);
        passwdEl.setAttribute("Type",
                              "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
        return untokEl;
    }

    private Element addEncryptedKey(Context c,
                                    Element securityHeader,
                                    X509Certificate recipientCertificate,
                                    Element[] elementsToEncrypt,
                                    Element desiredNextSibling)
            throws GeneralSecurityException, CausedDecoratorException
    {
        Document soapMsg = securityHeader.getOwnerDocument();

        byte[] keyBytes = new byte[16];
        c.rand.nextBytes(keyBytes);

        String xencNs = SoapUtil.XMLENC_NS;

        // Put the encrypted key in the right place
        Element encryptedKey;
        if (desiredNextSibling == null) {
            encryptedKey = XmlUtil.createAndAppendElementNS(securityHeader,
                                                            SoapUtil.ENCRYPTEDKEY_EL_NAME,
                                                            xencNs, "xenc");
        } else {
            encryptedKey = XmlUtil.createAndInsertBeforeElementNS(desiredNextSibling,
                                                                  SoapUtil.ENCRYPTEDKEY_EL_NAME,
                                                                  xencNs, "xenc");
        }
        String xenc = encryptedKey.getPrefix();

        Element encryptionMethod = XmlUtil.createAndAppendElementNS(encryptedKey, "EncryptionMethod", xencNs, xenc);
        encryptionMethod.setAttribute("Algorithm", SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO);

        byte[] recipSki = recipientCertificate.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
        if (recipSki != null)
            addKeyInfo(encryptedKey, recipSki, SoapUtil.VALUETYPE_SKI);
        else
            addKeyInfo(encryptedKey, recipientCertificate.getEncoded(), SoapUtil.VALUETYPE_X509);

        Element cipherData = XmlUtil.createAndAppendElementNS(encryptedKey, "CipherData", xencNs, xenc);
        Element cipherValue = XmlUtil.createAndAppendElementNS(cipherData, "CipherValue", xencNs, xenc);
        final String base64 = encryptWithRsa(c, keyBytes, recipientCertificate.getPublicKey());
        cipherValue.appendChild(soapMsg.createTextNode(base64));
        Element referenceList = XmlUtil.createAndAppendElementNS(encryptedKey, "ReferenceList", xencNs, xenc);

        for (int i = 0; i < elementsToEncrypt.length; i++) {
            Element element = elementsToEncrypt[i];
            Element encryptedElement = encryptElement(element, keyBytes);

            Element dataReference = XmlUtil.createAndAppendElementNS(referenceList, "DataReference", xencNs, xenc);
            dataReference.setAttribute("URI", "#" + getOrCreateWsuId(c, encryptedElement, element.getLocalName()));
        }

        return encryptedKey;
    }

    /**
     * Encrypt the specified element.  Returns the new EncryptedData element.
     * @param element
     * @param keyBytes
     * @return the EncryptedData element that replaces the specified element.
     */
    private Element encryptElement(Element element, byte[] keyBytes)
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
     * @param basename  Optional.  If non-null, will be used as the start of the Id string
     * @return
     */
    private String getOrCreateWsuId(Context c, Element element, String basename) {
        String id = SoapUtil.getElementWsuId(element);
        if (id == null) {
            id = createWsuId(c, element, basename == null ? element.getLocalName() : basename);
        }
        return id;
    }

    private String encryptWithRsa(Context c, byte[] keyBytes, PublicKey publicKey) throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.ENCRYPT_MODE, publicKey);
        if (!(publicKey instanceof RSAPublicKey))
            throw new KeyException("Unable to encrypt -- unsupported recipient public key type " +
                                   publicKey.getClass().getName());
        final int modulusLength = ((RSAPublicKey)publicKey).getModulus().toByteArray().length;
        byte[] paddedKeyBytes = padSymmetricKeyForRsaEncryption(c, keyBytes, modulusLength);
        byte[] encrypted = rsa.doFinal(paddedKeyBytes);
        return HexUtils.encodeBase64(encrypted, true);
    }

    /**
     * This handles the padding of the encryption method designated by http://www.w3.org/2001/04/xmlenc#rsa-1_5.
     *
     * Exceprt from the spec:
     * the padding is of the following special form:
     * 02 | PS* | 00 | key
     * where "|" is concatenation, "02" and "00" are fixed octets of the corresponding hexadecimal value, PS is
     * a string of strong pseudo-random octets [RANDOM] at least eight octets long, containing no zero octets,
     * and long enough that the value of the quantity being CRYPTed is one octet shorter than the RSA modulus,
     * and "key" is the key being transported. The key is 192 bits for TRIPLEDES and 128, 192, or 256 bits for
     * AES. Support of this key transport algorithm for transporting 192 bit keys is MANDATORY to implement.
     *
     * @param keyBytes
     * @return
     * @throws KeyException if there are too many keyBytes to fit inside this modulus
     */
    private byte[] padSymmetricKeyForRsaEncryption(Context c, byte[] keyBytes, int modulusBytes) throws KeyException {
        int padbytes = modulusBytes - 3 - keyBytes.length;

        // Check just in case, although this should never happen in real life
        if (padbytes < 8)
            throw new KeyException("Recipient RSA public key has too few bits to encode this symmetric key");

        byte[] padded = new byte[modulusBytes - 1];
        int pos = 0;
        padded[pos++] = 2;
        while (padbytes > 0) {
            padded[pos++] = (byte)(c.rand.nextInt(255) + 1);
            padbytes--;
        }
        padded[pos++] = 0;
        System.arraycopy(keyBytes, 0, padded, pos, keyBytes.length);



        return padded;
    }

    private void addKeyInfo(Element encryptedKey, byte[] idBytes, String valueType) {
        Document soapMsg = encryptedKey.getOwnerDocument();
        String wsseNs = encryptedKey.getParentNode().getNamespaceURI();
        String wssePrefix = encryptedKey.getParentNode().getPrefix();

        Element keyInfo = XmlUtil.createAndAppendElementNS(encryptedKey, "KeyInfo", SoapUtil.DIGSIG_URI, "dsig");
        Element securityTokenRef = XmlUtil.createAndAppendElementNS(keyInfo, SoapUtil.SECURITYTOKENREFERENCE_EL_NAME,
                                                                    wsseNs, wssePrefix);
        Element keyId = XmlUtil.createAndAppendElementNS(securityTokenRef, SoapUtil.KEYIDENTIFIER_EL_NAME,
                                                         wsseNs, wssePrefix);
        keyId.setAttribute("ValueType", valueType);
        String recipSkiB64 = HexUtils.encodeBase64(idBytes, true);
        keyId.appendChild(soapMsg.createTextNode(recipSkiB64));
    }

    /**
     * Generate a wsu:Id for the specified element, adds it to the element, and returns it.
     * Uses the specified basename as the start of the Id.
     * @param c
     * @param element
     * @return
     */
    private String createWsuId(Context c, Element element, String basename) {
        byte[] randbytes = new byte[16];
        c.rand.nextBytes(randbytes);
        String id = basename + "-" + c.count++ + "-" + HexUtils.hexDump(randbytes);

        if (c.idToElementCache.get(id) != null)
            throw new IllegalStateException("Duplicate wsu:ID generated: " + id); // can't happen

        c.idToElementCache.put(id, element);

        // Check for special handling of dsig and xenc Ids
        String ns = element.getNamespaceURI();
        if (SoapUtil.DIGSIG_URI.equals(ns) || SoapUtil.XMLENC_NS.equals(ns)) {
            // hack hack hack - xenc and dsig elements aren't allowed to use wsu:Id, due to their inflexible schemas.
            // WSSE says they we required to (ab)use local namespace Id instead.
            element.setAttribute("Id", id);
        } else {
            // do normal handling
            String wsuPrefix = XmlUtil.getOrCreatePrefixForNamespace(element, SoapUtil.WSU_NAMESPACE, "wsu");
            element.setAttributeNS(SoapUtil.WSU_NAMESPACE, wsuPrefix + ":Id", id);
        }

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
        element.appendChild(factory.createTextNode(HexUtils.encodeBase64(certificate.getEncoded(), true)));
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
