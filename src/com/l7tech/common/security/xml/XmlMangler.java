/*
* Copyright (C) 2003 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.XSignatureException;
import com.ibm.xml.enc.*;
import com.ibm.xml.enc.type.*;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.crypto.Cipher;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that encrypts and decrypts XML documents.
 * User: mike
 * Date: Aug 26, 2003
 * Time: 11:07:41 AM
 */
public class XmlMangler {
    static {
        JceProvider.init();
    }
    
    /** nice try asshole */
    private XmlMangler() { }

    // ID for EncryptedData element
    private static final String id = "bodyId";

    static final String NS_ENC_URI = "http://www.w3.org/2000/xmlns/";

    private static class XmlManglerException extends GeneralSecurityException {
        public XmlManglerException() {
        }

        public XmlManglerException(String msg) {
            super(msg);
        }

        public XmlManglerException(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }

        public XmlManglerException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    /**
     * In-place encrypt of the specified SOAP document's body element.  The body element will be encrypted with AES128
     * using the specified AES128 key, which will be tagged with the specified KeyName.
     * 
     * @param soapMsg  the SOAP document whose body to encrypt
     * @param keyBytes the 16 byte AES128 symmetric key to use to encrypt it
     * @param keyName  an identifier for this Key that will be meaningful to a consumer of the encrypted message
     * @throws IllegalArgumentException if the key was not an array of exactly 16 bytes of key material
     * @throws GeneralSecurityException if there was a problem with a key or a crypto provider
     * @throws IOException              if there was a problem reading or writing a key or a bit of xml
     */
    public static void encryptXml(Document soapMsg, byte[] keyBytes, String keyName)
            throws GeneralSecurityException, IOException, IllegalArgumentException, SoapUtil.MessageNotSoapException
    {
        encryptXml(soapMsg.getDocumentElement(), keyBytes, keyName, id);
    }


    /**
     * In-place encrypt of the specified SOAP document's element.  The element will be encrypted with AES128
     * using the specified AES128 key, which will be tagged with the specified KeyName.
     * 
     * @param element  the element to encrypt
     * @param keyBytes the 16 byte AES128 symmetric key to use to encrypt it
     * @param keyName  an identifier for this Key that will be meaningful to a consumer of the encrypted message
     * @param referenceId  an identifier for the encrypted element
     * @throws IllegalArgumentException if the key was not an array of exactly 16 bytes of key material
     * @throws GeneralSecurityException if there was a problem with a key or a crypto provider
     * @throws IOException              if there was a problem reading or writing a key or a bit of xml
     */
    public static void encryptXml(Element element, byte[] keyBytes, String keyName, String referenceId)
            throws GeneralSecurityException, IOException, IllegalArgumentException, SoapUtil.MessageNotSoapException {
        Document soapMsg = element.getOwnerDocument();
        if (keyBytes.length < 16)
            throw new IllegalArgumentException("keyBytes must be at least 16 bytes long for AES128");


        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());
        KeyInfo keyInfo = new KeyInfo();
        KeyName kn = new KeyName();
        kn.setName(keyName);
        keyInfo.addKeyName(kn);
        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(EncryptionMethod.AES128_CBC);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setKeyInfo(keyInfo);
        encData.setId(referenceId);
        Element encDataElement = null;
        try {
            encDataElement = encData.createElement(soapMsg, true);
        } catch (StructureException e) {
            throw new XmlManglerException(e);
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
            throw new XmlManglerException(e);
        } catch (StructureException e) {
            throw new XmlManglerException(e);
        }

        // Insert a WSS style header with a ReferenceList refering to the EncryptedData element
        addWssHeader(soapMsg.getDocumentElement(), referenceId);
    }

    /**
     * Update the document WSS header with the encrypted element reference info.
     * 
     * @param element     the document element
     * @param referenceId the element reference id
     */
    private static void addWssHeader(Element element, String referenceId) throws SoapUtil.MessageNotSoapException {
        Document document = element.getOwnerDocument();
        // Add new namespaces to Envelope element, as per spec.

        Element rootElement = document.getDocumentElement();
        if (rootElement.getAttributeNodeNS(NS_ENC_URI, "xenc") == null) {
            rootElement.setAttributeNS(NS_ENC_URI, "xmlns:xenc", SoapUtil.XMLENC_NS);
        }

        // Add Security element to header, referencing the encrypted body
        Element dataRefEl = document.createElementNS(SoapUtil.XMLENC_NS, "xenc:DataReference");
        dataRefEl.setAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME, referenceId);

        Element refEl = document.createElementNS(SoapUtil.XMLENC_NS, "xenc:ReferenceList");
        refEl.appendChild(dataRefEl);

        Element securityEl = SoapUtil.getOrMakeSecurityElement(document);
        if ( securityEl == null ) {
            throw new SoapUtil.MessageNotSoapException("Can't add WS-Security header to non-SOAP message");
        }
        securityEl.appendChild(refEl);
    }

    /**
     * Determine the KeyName used to encrypt the specified encrypted SOAP document.  Callers might
     * use the returned key name to decide which PrivateKey to use with a subsequent call to decryptXml().
     * 
     * @param soapMsg The encrypted SOAP document to examine.
     * @return the value of the KeyName node.
     */
    public static String getKeyName(Document soapMsg) {
        // Derive the key alias from KeyInfo
        Element keyNameEl = (Element)soapMsg.getElementsByTagName("KeyName").item(0);
        Text keyNameText = (Text)keyNameEl.getFirstChild();
        String alias = keyNameText.getNodeValue();
        return alias;
    }

    /**
     * In-place decrypt of the specified encrypted element.  Caller is responsible for ensuring that the
     * correct key is used for the document, of the proper format for the encryption scheme it used.  Caller can use
     * getKeyName() to help decide which Key to use to decrypt the document.  Caller is responsible for
     * cleaning out any empty reference lists, security headers, or soap headers after all encrypted
     * elements have been processed.
     * <p/>
     * If this method returns normally the specified element will have been successfully decrypted.
     *
     * @param messagePartElement The Element to decrypt.
     * @param key     The key to use to decrypt it. If the document was encrypted with
     *                a call to encryptXml(), the Key will be a 16 byte AES128 symmetric key.
     * @param keyKefList the ReferenceList element associated with the key. this is optional and only make sense 
     *                   if the message contains an encryptedkey
     * @throws GeneralSecurityException     if there was a problem with a key or crypto provider
     * @throws ParserConfigurationException if there was a problem with the XML parser
     * @throws IOException                  if there was an IO error while reading the document or a key
     * @throws SAXException                 if there was a problem parsing the document
     */
    public static void decryptElement(Element messagePartElement, Key key, Element keyKefList)
            throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
            XMLSecurityElementNotFoundException
    {
        Document soapMsg = messagePartElement.getOwnerDocument();
        Element encryptedDataElement = XmlUtil.findFirstChildElementByName(messagePartElement,
                                                                           SoapUtil.XMLENC_NS,
                                                                           "EncryptedData");
        if (encryptedDataElement == null)
            throw new XMLSecurityElementNotFoundException("No EncryptedData found inside the 'encrypted' message part");

        String messagePartId = SoapUtil.getElementId(encryptedDataElement);
        Element header = SoapUtil.getHeaderElement(soapMsg);
        if (header == null)
            throw new XMLSecurityElementNotFoundException("EncryptedData is present, but there is no SOAP header");

        Element security = SoapUtil.getSecurityElement(header);
        if ( security == null ) throw new XMLSecurityElementNotFoundException("Can't get Security header for non-SOAP message");

        List referenceListList = null;
        if (keyKefList != null) {
            referenceListList = new ArrayList();
            referenceListList.add(keyKefList);
        } else {
            logger.finest("Looking for reference lists.");
            // find reference lists
            referenceListList = XmlUtil.findChildElementsByName(security, SoapUtil.XMLENC_NS, "ReferenceList");
        }
        if (referenceListList == null || referenceListList.isEmpty())
            throw new XMLSecurityElementNotFoundException("EncryptedData is present, but there is no ReferenceList");
        for (Iterator i = referenceListList.iterator(); i.hasNext();) {
            Element referenceList = (Element)i.next();

            List dataRefEls = XmlUtil.findChildElementsByName(referenceList, SoapUtil.XMLENC_NS, "DataReference");
            if ( dataRefEls == null || dataRefEls.isEmpty() ) {
                logger.warning("EncryptedData is present, but contains at least one empty ReferenceList");
                continue;
            }

            for (Iterator j = dataRefEls.iterator(); j.hasNext();) {
                Element dataRefEl = (Element)j.next();
                String dataRefUri = dataRefEl.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);
                if (dataRefUri.charAt(0) == '#') dataRefUri = dataRefUri.substring(1);
                if ( dataRefUri != null && dataRefUri.equals(messagePartId ) ) {
                    // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
                    // soapMsg document
                    DecryptionContext dc = new DecryptionContext();
                    AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
                    af.setProvider(JceProvider.getSymmetricJceProvider().getName());
                    dc.setAlgorithmFactory(af);
                    dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT, null, null);
                    dc.setKey(key);

                    try {
                        dc.decrypt();
                        dc.replace();
                    } catch (XSignatureException e) {
                        throw new XmlManglerException(e);
                    } catch (StructureException e) {
                        throw new XmlManglerException(e);
                    } catch (KeyInfoResolvingException e) {
                        throw new XmlManglerException(e);
                    }

                    referenceList.removeChild(dataRefEl);
                    if (!referenceList.hasChildNodes())
                        referenceList.getParentNode().removeChild(referenceList);
                    return;
                }
            }
        }
        throw new XMLSecurityElementNotFoundException("EncryptedData is present, but there are no matching DataReference tags");
    }

    /**
     * In-place decrypt of the specified document's document element.  Caller is responsible for ensuring that the
     * correct key is used for the document, of the proper format for the encryption scheme it used.  Caller can use
     * getKeyName() to help decide which Key to use to decrypt the document.  Caller is responsible for
     * cleaning out any empty reference lists, security headers, or soap headers after all encrypted
     * elements have been processed.
     * <p/>
     * If this method returns normally the specified element will have been successfully decrypted.
     *
     * @param soapMsg The SOAP document to decrypt.
     * @param key     The key to use to decrypt it. If the document was encrypted with
     *                a call to encryptXml(), the Key will be a 16 byte AES128 symmetric key.
     * @throws GeneralSecurityException     if there was a problem with a key or crypto provider
     * @throws ParserConfigurationException if there was a problem with the XML parser
     * @throws IOException                  if there was an IO error while reading the document or a key
     * @throws SAXException                 if there was a problem parsing the document
     */
    public static void decryptDocument(Document soapMsg, Key key)
            throws GeneralSecurityException, XMLSecurityElementNotFoundException, IOException,
            ParserConfigurationException, SAXException
    {
        decryptElement(soapMsg.getDocumentElement(), key, null);
    }

    public static class ProcessedEncryptedKey {
        public Key decryptedKey;
        public Element referenceList;
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
     * @param paddedKey the decrpted but still padded key
     * @return the unpadded decrypted key
     */
    private static byte[] unPadRSADecryptedSymettricKey(byte[] paddedKey) throws IllegalArgumentException {
        // the first byte should be 02
        if (paddedKey[0] != 2) throw new IllegalArgumentException("paddedKey has wrong format");
        // traverse the next series of byte until we get to the first 00
        int pos = 0;
        for (pos = 0; pos < paddedKey.length; pos++) {
            if (paddedKey[pos] == 0) {
                break;
            }
        }
        // the remainder is the key to return
        int keylength = paddedKey.length - 1 - pos;
        if (keylength < 16) {
            throw new IllegalArgumentException("key length " + keylength + "is not a valid length");
        }
        byte[] output = new byte[keylength];
        System.arraycopy(paddedKey, pos+1, output, 0, keylength);
        return output;
    }

    /**
     * Decrypts all EncryptedKey elements contained in this message. That is, all EncryptedKey elements
     * that can be cedrypted with the passed recipientPrivateKey.
     * @param soapMsg the soap document to look for encrypted keys for
     * @param recipientPrivateKey the private key used to decypher the encrypted key
     * @param ski the subject key identifier used to check if an encrypted key is destined for the associated private key
     * @return never null;
     */
    public static ProcessedEncryptedKey[] getEncryptedKeyFromMessage(Document soapMsg, PrivateKey recipientPrivateKey, byte[] ski) {
        ArrayList output = new ArrayList();

        // look for the Header/Security/EncryptedKey element
        NodeList encryptedKeyElements = soapMsg.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedKey");
        for (int i = 0; i < encryptedKeyElements.getLength(); i++) {
            Element encryptedKey = (Element)encryptedKeyElements.item(i);
            // check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
            if (ski != null) {
                try {
                    Element kinfo = XmlUtil.findOnlyOneChildElementByName(encryptedKey, SoapUtil.DIGSIG_URI, "KeyInfo");
                    if (kinfo != null) {
                        Element str = XmlUtil.findOnlyOneChildElementByName(kinfo,
                                                                            SoapUtil.SECURITY_URIS_ARRAY,
                                                                            "SecurityTokenReference");
                        if (str != null) {
                            Element ki = XmlUtil.findOnlyOneChildElementByName(str,
                                                                               SoapUtil.SECURITY_URIS_ARRAY,
                                                                               "KeyIdentifier");
                            if (ki != null) {
                                String keyIdentifierValue = XmlUtil.getTextValue(ki);
                                byte[] keyIdValueBytes = null;
                                try {
                                    keyIdValueBytes = HexUtils.decodeBase64(keyIdentifierValue);
                                } catch (IOException e) {
                                    logger.log(Level.WARNING, "could not decode " + keyIdentifierValue, e);
                                }
                                if (keyIdValueBytes != null) {
                                    // trim if necessary
                                    byte[] ski2 = ski;
                                    if (ski.length > keyIdValueBytes.length) {
                                        ski2 = new byte[keyIdValueBytes.length];
                                        System.arraycopy(ski, ski.length-keyIdValueBytes.length, ski2, 0, keyIdValueBytes.length);
                                    }
                                    if (Arrays.equals(keyIdValueBytes, ski2)) {
                                        logger.fine("the Key SKI is recognized");
                                    } else {
                                        logger.fine("the ski does not match. looking for next encryptedkey");
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                } catch (XmlUtil.MultipleChildElementsException e) {
                    logger.log(Level.WARNING, "unexpected construction", e);
                    continue;
                }
            }
            // verify that the algo is supported
            Element encryptionMethodEl = null;
            try {
                encryptionMethodEl = XmlUtil.findOnlyOneChildElementByName(encryptedKey,
                                                                           SoapUtil.XMLENC_NS,
                                                                           "EncryptionMethod");
            } catch (XmlUtil.MultipleChildElementsException e) {
                logger.warning("EncryptedKey has more than one EncryptionMethod element");
            }
            if (encryptionMethodEl != null) {
                String encMethodValue = encryptionMethodEl.getAttribute("Algorithm");
                if (encMethodValue == null || encMethodValue.length() < 1) {
                    logger.warning("Algorithm not specified in EncryptionMethod element");
                    continue;
                } else if (!encMethodValue.equals("http://www.w3.org/2001/04/xmlenc#rsa-1_5")) {
                    logger.warning("Algorithm not supported " + encMethodValue);
                    continue;
                }
            }
            // get the xenc:CipherValue
            Element cipherValue = null;
            try {
                Element cipherData = XmlUtil.findOnlyOneChildElementByName(encryptedKey, SoapUtil.XMLENC_NS, "CipherData");
                if (cipherData != null) {
                    cipherValue = XmlUtil.findOnlyOneChildElementByName(cipherData, SoapUtil.XMLENC_NS, "CipherValue");
                }
            } catch (XmlUtil.MultipleChildElementsException e) {
                logger.warning("EncryptedKey has more than one CipherData or CipherValue");
            }
            if (cipherValue == null) {
                logger.warning("element missing CipherValue");
            } else {
                // we got the value, decrypt it
                String value = XmlUtil.getTextValue(cipherValue);
                byte[] encryptedKeyBytes = null;
                try {
                    encryptedKeyBytes = HexUtils.decodeBase64(value);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "cannot b64 decode CipherValue contents: " + value, e);
                }
                Cipher rsa = null;
                try {
                    //rsa = Cipher.getInstance("RSA/CBC/PKCS5Padding");
                    rsa = Cipher.getInstance("RSA");
                    rsa.init(Cipher.DECRYPT_MODE, recipientPrivateKey);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "init error", e);
                }

                byte[] decryptedPadded = new byte[0];
                try {
                    decryptedPadded = rsa.doFinal(encryptedKeyBytes);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "decryption error", e);
                }
                // unpad
                byte[] unencryptedKey = null;
                try {
                    unencryptedKey = unPadRSADecryptedSymettricKey(decryptedPadded);
                } catch (IllegalArgumentException e) {
                    logger.log(Level.WARNING, "The key could not be unpadded", e);
                    continue;
                }
                ProcessedEncryptedKey item = new ProcessedEncryptedKey();
                item.decryptedKey = new AesKey(unencryptedKey, unencryptedKey.length*8);
                try {
                    item.referenceList = XmlUtil.findOnlyOneChildElementByName(encryptedKey, SoapUtil.XMLENC_NS, "ReferenceList");
                } catch (XmlUtil.MultipleChildElementsException e) {
                    logger.warning("unexpected multiple reference list elements in encrypted key " + e.getMessage());
                }
                output.add(item);
            }
        }
        if (output.isEmpty()) {
            return new ProcessedEncryptedKey[0];
        } else {
            ProcessedEncryptedKey[] realoutput = new ProcessedEncryptedKey[output.size()];
            for (int i = 0; i < realoutput.length; i++) {
                realoutput[i] = (ProcessedEncryptedKey)output.get(i);
            }
            return realoutput;
        }
    }

    // Use a logger that will work inside either the Agent or the Gateway.
    private static final Logger logger = Logger.getLogger(XmlMangler.class.getName());
}
