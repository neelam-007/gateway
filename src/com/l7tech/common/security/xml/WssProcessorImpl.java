package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.Key;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.io.IOException;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.dsig.XSignatureException;

import javax.crypto.Cipher;
import javax.xml.parsers.ParserConfigurationException;

/**
 * An implementation of the WssProcessor for use in both the SSG and the SSA.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 5, 2004<br/>
 * $Id$<br/>
 */
public class WssProcessorImpl implements WssProcessor {

    static {
        JceProvider.init();
    }

    /**
     * This processes a soap message. That is, the contents of the Header/Security are processed as per the WSS rules.
     * Do not use same instances across threads (not thread safe).
     *
     * @param soapMsg the xml document containing the soap message. this document may be modified on exit
     * @param recipientCert the recipient's cert to which encrypted keys may be encoded for
     * @param recipientKey the private key corresponding to the recipientCertificate used to decypher the encrypted keys
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws ProcessorException
     */
    public WssProcessor.ProcessorResult undecorateMessage(Document soapMsg,
                                                          X509Certificate recipientCert,
                                                          PrivateKey recipientKey) throws WssProcessor.ProcessorException {
        // Reset all potential outputs
        processedDocument = (Document)soapMsg.cloneNode(true);
        originalDocument = soapMsg;
        elementsThatWereSigned.clear();
        elementsThatWereEncrypted.clear();
        securityTokens.clear();
        timestamp = null;
        releventSecurityHeader = null;

        // Resolve the relevent Security header
        Element[] securityHeaders = SoapUtil.getSecurityElements(processedDocument);
        // find the relevent security header. that is the one with no actor
        String currentSoapNamespace = soapMsg.getDocumentElement().getNamespaceURI();
        for (int i = 0; i < securityHeaders.length; i++) {
            String thisActor = securityHeaders[i].getAttributeNS(currentSoapNamespace, "actor");
            if (thisActor == null || thisActor.length() < 1) {
                releventSecurityHeader = securityHeaders[i];
                break;
            }
        }
        // maybe there are no security headers at all in which case, there is nothing to process
        if (releventSecurityHeader == null) {
            logger.finer("No relevent security header found.");
            return produceResult();
        }

        // Process elements one by one
        NodeList securityChildren = releventSecurityHeader.getElementsByTagName("*");
        for (int i = 0; i < securityChildren.getLength(); i++) {
            Element securityChildToProcess = (Element)securityChildren.item(i);

            if (securityChildToProcess.getLocalName().equals("EncryptedKey")) {
                processEncryptedKey(securityChildToProcess, recipientKey, recipientCert.getExtensionValue("2.5.29.14"));
            } else if (securityChildToProcess.getLocalName().equals("Timestamp")) {
                processTimestamp(securityChildToProcess);
            } else if (securityChildToProcess.getLocalName().equals("BinarySecurityToken")) {
                processBinarySecurityToken(securityChildToProcess);
            } else if (securityChildToProcess.getLocalName().equals("Signature")) {
                processSignature(securityChildToProcess);
            } else {
                logger.finer("Unknown element in security header: " + securityChildToProcess.getNodeName());
            }
        }

        // remove Security element altogether
        releventSecurityHeader.getParentNode().removeChild(releventSecurityHeader);

        return produceResult();
    }

    private void processEncryptedKey(Element encryptedKeyElement, PrivateKey recipientKey, byte[] ski) {
        logger.finest("Processing EncryptedKey");

        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
        if (ski != null) {
            try {
                Element kinfo = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement, SoapUtil.DIGSIG_URI, "KeyInfo");
                if (kinfo != null) {
                    Element str = XmlUtil.findOnlyOneChildElementByName(kinfo,
                                                                        new String[] {SoapUtil.SECURITY_NAMESPACE,
                                                                                      SoapUtil.SECURITY_NAMESPACE2,
                                                                                      SoapUtil.SECURITY_NAMESPACE3},
                                                                        "SecurityTokenReference");
                    if (str != null) {
                        Element ki = XmlUtil.findOnlyOneChildElementByName(str,
                                                                           new String[] {SoapUtil.SECURITY_NAMESPACE,
                                                                                         SoapUtil.SECURITY_NAMESPACE2,
                                                                                         SoapUtil.SECURITY_NAMESPACE3},
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
                                    logger.info("the ski does not match. looking for next encryptedkey");
                                    return;
                                }
                            }
                        }
                    }
                }
            } catch (XmlUtil.MultipleChildElementsException e) {
                logger.log(Level.WARNING, "unexpected construction", e);
                // todo, throw some exception
            }
        }
        // verify that the algo is supported
        Element encryptionMethodEl = null;
        try {
            encryptionMethodEl = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                       SoapUtil.XMLENC_NS,
                                                                       "EncryptionMethod");
        } catch (XmlUtil.MultipleChildElementsException e) {
            logger.warning("EncryptedKey has more than one EncryptionMethod element");
        }
        if (encryptionMethodEl != null) {
            String encMethodValue = encryptionMethodEl.getAttribute("Algorithm");
            if (encMethodValue == null || encMethodValue.length() < 1) {
                logger.warning("Algorithm not specified in EncryptionMethod element");
                return;
            } else if (!encMethodValue.equals(SUPPORTED_ENCRYPTEDKEY_ALGO)) {
                logger.warning("Algorithm not supported " + encMethodValue);
                return;
            }
        }

        // get the xenc:CipherValue
        Element cipherValue = null;
        try {
            Element cipherData = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement, SoapUtil.XMLENC_NS, "CipherData");
            if (cipherData != null) {
                cipherValue = XmlUtil.findOnlyOneChildElementByName(cipherData, SoapUtil.XMLENC_NS, "CipherValue");
            }
        } catch (XmlUtil.MultipleChildElementsException e) {
            logger.warning("EncryptedKey has more than one CipherData or CipherValue");
        }
        if (cipherValue == null) {
            logger.warning("element missing CipherValue");
            return;
        }
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
            rsa = Cipher.getInstance("RSA");
            rsa.init(Cipher.DECRYPT_MODE, recipientKey);
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
            // todo, throw some exception
            return;
        }

        // We got the key. Get the list of elements to decrypt.
        Element refList = null;
        try {
            refList = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement, SoapUtil.XMLENC_NS, "ReferenceList");
        } catch (XmlUtil.MultipleChildElementsException e) {
            logger.warning("unexpected multiple reference list elements in encrypted key " + e.getMessage());
            // todo, throw some exception
            return;
        }
        try {
            decryptReferencedElements(new AesKey(unencryptedKey, unencryptedKey.length*8), refList);
        } catch (GeneralSecurityException e) {
            // todo, throw some exception
        } catch (ParserConfigurationException e) {
            // todo, throw some exception
        } catch (IOException e) {
            // todo, throw some exception
        } catch (SAXException e) {
            // todo, throw some exception
        } catch (XMLSecurityElementNotFoundException e) {
            // todo, throw some exception
        }
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
     * In-place decrypt of the specified encrypted element.  Caller is responsible for ensuring that the
     * correct key is used for the document, of the proper format for the encryption scheme it used.  Caller can use
     * getKeyName() to help decide which Key to use to decrypt the document.  Caller is responsible for
     * cleaning out any empty reference lists, security headers, or soap headers after all encrypted
     * elements have been processed.
     * <p/>
     * If this method returns normally the specified element will have been successfully decrypted.
     *
     * @param key     The key to use to decrypt it. If the document was encrypted with
     *                a call to encryptXml(), the Key will be a 16 byte AES128 symmetric key.
     * @param refList the ReferenceList element associated with the key. this is optional and only make sense
     *                   if the message contains an encryptedkey
     * @throws java.security.GeneralSecurityException     if there was a problem with a key or crypto provider
     * @throws javax.xml.parsers.ParserConfigurationException if there was a problem with the XML parser
     * @throws IOException                  if there was an IO error while reading the document or a key
     * @throws org.xml.sax.SAXException                 if there was a problem parsing the document
     */
    public void decryptReferencedElements(Key key, Element refList)
            throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
            XMLSecurityElementNotFoundException
    {
        List dataRefEls = XmlUtil.findChildElementsByName(refList, SoapUtil.XMLENC_NS, "DataReference");
        if ( dataRefEls == null || dataRefEls.isEmpty() ) {
            logger.warning("EncryptedData is present, but contains at least one empty ReferenceList");
            return;
        }

        for (Iterator j = dataRefEls.iterator(); j.hasNext();) {
            Element dataRefEl = (Element)j.next();
            String dataRefUri = dataRefEl.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);

            // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
            // soapMsg document
            DecryptionContext dc = new DecryptionContext();
            AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
            af.setProvider(JceProvider.getSymmetricJceProvider().getName());
            dc.setAlgorithmFactory(af);
            Element encryptedDataElement = SoapUtil.getElementById(refList.getOwnerDocument(), dataRefUri);
            if (encryptedDataElement == null) {
                logger.warning("cannot resolve encrypted data element " + dataRefUri);
                // todo throw some exception
                return;
            }
            dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT,
                                                        null, null);
            dc.setKey(key);
            try {
                dc.decrypt();
                dc.replace();
            } catch (XSignatureException e) {
                // todo, throw something here
            } catch (StructureException e) {
                // todo, throw something here
            } catch (KeyInfoResolvingException e) {
                // todo, throw something here
            }

            // remember encrypted element
            encryptedDataElement = SoapUtil.getElementById(originalDocument, dataRefUri);
            if (encryptedDataElement == null) {
                logger.warning("cannot resolve encrypted data element " + dataRefUri + " from original doc");
            } else elementsThatWereEncrypted.add(encryptedDataElement);
        }
    }

    private void processTimestamp(Element timestampElement) {
        logger.finest("Processing Timestamp");
        // todo
    }

    private void processBinarySecurityToken(Element binarySecurityTokenElement) {
        logger.finest("Processing BinarySecurityToken");
        // todo
    }

    private void processSignature(Element signatureElement) {
        logger.finest("Processing Signature");
        // todo
    }

    private WssProcessor.ProcessorResult produceResult() {
        return new WssProcessor.ProcessorResult() {
            public Document getUndecoratedMessage() {
                return processedDocument;
            }
            public Element[] getElementsThatWereSigned() {
                Element[] output = new Element[elementsThatWereSigned.size()];
                Iterator iter = elementsThatWereSigned.iterator();
                for (int i = 0; i < output.length; i++) {
                    output[i] = (Element)iter.next();
                }
                return output;
            }
            public Element[] getElementsThatWereEncrypted() {
                Element[] output = new Element[elementsThatWereEncrypted.size()];
                Iterator iter = elementsThatWereEncrypted.iterator();
                for (int i = 0; i < output.length; i++) {
                    output[i] = (Element)iter.next();
                }
                return output;
            }
            public WssProcessor.SecurityToken[] getSecurityTokens() {
                WssProcessor.SecurityToken[] output = new WssProcessor.SecurityToken[securityTokens.size()];
                Iterator iter = securityTokens.iterator();
                for (int i = 0; i < output.length; i++) {
                    output[i] = (WssProcessor.SecurityToken)iter.next();
                }
                return output;
            }
            public WssProcessor.Timestamp getTimestamp() {
                return timestamp;
            }
        };
    }

    private final Logger logger = Logger.getLogger(WssProcessorImpl.class.getName());

    private Document processedDocument = null;
    private Document originalDocument = null;
    private final Collection elementsThatWereSigned = new ArrayList();
    private final Collection elementsThatWereEncrypted = new ArrayList();
    private final Collection securityTokens = new ArrayList();
    private WssProcessor.Timestamp timestamp = null;
    private Element releventSecurityHeader = null;
    public static final String SUPPORTED_ENCRYPTEDKEY_ALGO = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
}
