package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.XSignatureException;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.EncryptedData;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.Cipher;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.misc.BASE64Decoder;

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

    private static class ProcessorException extends WssProcessor.ProcessorException {
        public ProcessorException(Throwable cause) {
            super();
            initCause(cause);
        }

        public ProcessorException(String message) {
            super();
            initCause(new IllegalArgumentException(message));
        }
    }

    /**
     * This processes a soap message. That is, the contents of the Header/Security are processed as per the WSS rules.
     *
     * @param soapMsg the xml document containing the soap message. this document may be modified on exit
     * @param recipientCert the recipient's cert to which encrypted keys may be encoded for
     * @param recipientKey the private key corresponding to the recipientCertificate used to decypher the encrypted keys
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws ProcessorException
     */
    public WssProcessor.ProcessorResult undecorateMessage(Document soapMsg,
                                                          X509Certificate recipientCert,
                                                          PrivateKey recipientKey)
            throws WssProcessor.ProcessorException
    {
        try {
            return doUndecorateMessage(soapMsg, recipientCert, recipientKey);
        } catch (XmlUtil.MultipleChildElementsException e) {
            throw new ProcessorException(e);
        } catch (ParseException e) {
            throw new ProcessorException(e);
        }
    }

    private WssProcessor.ProcessorResult doUndecorateMessage(Document soapMsg,
                                                          X509Certificate recipientCert,
                                                          PrivateKey recipientKey)
            throws WssProcessor.ProcessorException, XmlUtil.MultipleChildElementsException, ParseException
    {
        // Reset all potential outputs
        ProcessingStatusHolder cntx = new ProcessingStatusHolder();
        cntx.processedDocument = (Document)soapMsg.cloneNode(true);
        cntx.originalDocument = soapMsg;
        cntx.elementsThatWereSigned.clear();
        cntx.elementsThatWereEncrypted.clear();
        cntx.securityTokens.clear();
        cntx.timestamp = null;
        cntx.releventSecurityHeader = null;

        String currentSoapNamespace = soapMsg.getDocumentElement().getNamespaceURI();

        // Resolve the relevent Security header
        try {
            cntx.releventSecurityHeader = SoapUtil.getSecurityElement(cntx.processedDocument);
        } catch (XmlUtil.MultipleChildElementsException e) {
            throw new ProcessorException(e);
        }

        // maybe there are no security headers at all in which case, there is nothing to process
        if (cntx.releventSecurityHeader == null) {
            logger.finer("No relevent security header found.");
            return produceResult(cntx);
        }

        // Process elements one by one
        NodeList securityChildren = cntx.releventSecurityHeader.getElementsByTagName("*");
        for (int i = 0; i < securityChildren.getLength(); i++) {
            Element securityChildToProcess = (Element)securityChildren.item(i);

            if (securityChildToProcess.getLocalName().equals(SoapUtil.ENCRYPTEDKEY_EL_NAME)) {
                processEncryptedKey(securityChildToProcess, recipientKey,
                                    recipientCert.getExtensionValue("2.5.29.14"), cntx);
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.TIMESTAMP_EL_NAME)) {
                processTimestamp(cntx, securityChildToProcess);
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.BINARYSECURITYTOKEN_EL_NAME)) {
                processBinarySecurityToken(securityChildToProcess, cntx);
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SIGNATURE_EL_NAME)) {
                processSignature(securityChildToProcess);
            } else {
                // Unhandled child elements of the Security Header
                String mu = securityChildToProcess.getAttributeNS(currentSoapNamespace, SoapUtil.MUSTUNDERSTAND_ATTR_NAME).trim();
                if ("1".equals(mu) || "true".equalsIgnoreCase(mu)) {
                    String msg = "Unrecognized element in default Security header: " +
                                 securityChildToProcess.getNodeName() +
                                 " with mustUnderstand=\"true\"; rejecting message";
                    logger.warning(msg);
                    throw new ProcessorException(msg);
                } else {
                    logger.finer("Unknown element in security header: " + securityChildToProcess.getNodeName());
                }
            }
        }

        // Backward compatibility - if we didn't see a timestamp in the security header, look up in the soap header
        if (cntx.timestamp == null) {
            Element header = XmlUtil.findOnlyOneChildElementByName(soapMsg.getDocumentElement(),
                                                                   currentSoapNamespace,
                                                                   SoapUtil.HEADER_EL_NAME);
            // (header can't be null or we woulnd't be here)
            Element timestamp = XmlUtil.findFirstChildElementByName(header,
                                                                    SoapUtil.WSU_URIS_ARRAY,
                                                                    SoapUtil.TIMESTAMP_EL_NAME);
            if (timestamp != null)
                processTimestamp(cntx, timestamp);

        }

        // remove Security element altogether
        cntx.releventSecurityHeader.getParentNode().removeChild(cntx.releventSecurityHeader);

        // if there were other security headers and one with a special actor set by the agent, we
        // want to change the actor here to set it back to default value todo

        return produceResult(cntx);
    }

    private void processEncryptedKey(Element encryptedKeyElement,
                                     PrivateKey recipientKey,
                                     byte[] ski,
                                     ProcessingStatusHolder cntx) throws ProcessorException {
        logger.finest("Processing EncryptedKey");

        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
        if (ski != null) {
            try {
                Element kinfo = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement, SoapUtil.DIGSIG_URI, "KeyInfo");
                if (kinfo != null) {
                    Element str = XmlUtil.findOnlyOneChildElementByName(kinfo,
                                                                        SoapUtil.SECURITY_URIS_ARRAY,
                                                                        SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
                    if (str != null) {
                        Element ki = XmlUtil.findOnlyOneChildElementByName(str,
                                                                           SoapUtil.SECURITY_URIS_ARRAY,
                                                                           SoapUtil.KEYIDENTIFIER_EL_NAME);
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
                throw new ProcessorException(e);
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
            throw new ProcessorException(e);
        }

        // We got the key. Get the list of elements to decrypt.
        Element refList = null;
        try {
            refList = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement, SoapUtil.XMLENC_NS, "ReferenceList");
        } catch (XmlUtil.MultipleChildElementsException e) {
            logger.warning("unexpected multiple reference list elements in encrypted key " + e.getMessage());
            throw new ProcessorException(e);
        }
        try {
            decryptReferencedElements(new AesKey(unencryptedKey, unencryptedKey.length*8), refList, cntx);
        } catch (GeneralSecurityException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
        } catch (XMLSecurityElementNotFoundException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
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
    public void decryptReferencedElements(Key key, Element refList, ProcessingStatusHolder cntx)
            throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
            XMLSecurityElementNotFoundException, ProcessorException
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
                String msg = "cannot resolve encrypted data element " + dataRefUri;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }
            dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT,
                                                        null, null);
            dc.setKey(key);
            try {
                // do the actual decryption
                dc.decrypt();
                dc.replace();
                // remember encrypted element
                NodeList dataList = dc.getDataAsNodeList();
                for (int i = 0; i < dataList.getLength(); i++) {
                    Node node = dataList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        cntx.elementsThatWereEncrypted.add(node);
                    }
                }
            } catch (XSignatureException e) {
                logger.log(Level.WARNING, "Error decrypting", e);
                throw new ProcessorException(e);
            } catch (StructureException e) {
                logger.log(Level.WARNING, "Error decrypting", e);
                throw new ProcessorException(e);
            } catch (KeyInfoResolvingException e) {
                logger.log(Level.WARNING, "Error decrypting", e);
                throw new ProcessorException(e);
            }
        }
    }

    private static class TimestampDate implements WssProcessor.TimestampDate {
        Element element;
        Date date;

        TimestampDate(Element createdOrExpiresElement) throws ParseException {
            element = createdOrExpiresElement;
            String dateString = XmlUtil.findFirstChildTextNode(element);
            DateFormat dateFormat = new SimpleDateFormat(SoapUtil.DATE_FORMAT_PATTERN);
            dateFormat.setTimeZone(SoapUtil.DATE_FORMAT_TIMEZONE);
            date = dateFormat.parse(dateString);
        }

        public Date asDate() {
            return date;
        }

        public Element asElement() {
            return element;
        }

        public String asXmlString() {
            try {
                return XmlUtil.elementToString(element);
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    }

    private void processTimestamp(ProcessingStatusHolder ctx, final Element timestampElement)
            throws XmlUtil.MultipleChildElementsException, ParseException
    {
        logger.finest("Processing Timestamp");
        final Element created = XmlUtil.findOnlyOneChildElementByName(timestampElement,
                                                                      SoapUtil.WSU_URIS_ARRAY,
                                                                      SoapUtil.CREATED_EL_NAME);
        final Element expires = XmlUtil.findOnlyOneChildElementByName(timestampElement,
                                                                      SoapUtil.WSU_URIS_ARRAY,
                                                                      SoapUtil.EXPIRES_EL_NAME);

        final TimestampDate createdTimestampDate = created == null ? null : new TimestampDate(created);
        final TimestampDate expiresTimestampDate = expires == null ? null : new TimestampDate(expires);

        ctx.timestamp = new Timestamp() {
            public WssProcessor.TimestampDate getCreated() {
                return createdTimestampDate;
            }

            public WssProcessor.TimestampDate getExpires() {
                return expiresTimestampDate;
            }

            public Element asElement() {
                return timestampElement;
            }

            public String asXmlString() {
                try {
                    return XmlUtil.elementToString(timestampElement);
                } catch (IOException e) {
                    throw new RuntimeException(e); // can't happen
                }
            }
        };
    }

    private void processBinarySecurityToken(final Element binarySecurityTokenElement,
                                            ProcessingStatusHolder cntx) throws ProcessorException {
        logger.finest("Processing BinarySecurityToken");

        // assume that this is a b64ed binary x509 cert, get the value
        // todo, look into the ValueType argument instead of assuming this
        String value = XmlUtil.getTextValue(binarySecurityTokenElement);
        if (value == null || value.length() < 1) {
            String msg = "The " + binarySecurityTokenElement.getLocalName() + " does not contain a value.";
            logger.warning(msg);
            throw new ProcessorException(msg);
        }
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] decodedValue = null;
        try {
            decodedValue = decoder.decodeBuffer(value);
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
        // create the x509 binary cert based on it
        X509Certificate referencedCert = null;
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            InputStream is = new ByteArrayInputStream(decodedValue);
            referencedCert = (X509Certificate)factory.generateCertificate(is);
        } catch (CertificateException e) {
            throw new ProcessorException(e);
        }
        // remember this cert
        final X509Certificate finalcert = referencedCert;
        WssProcessor.SecurityToken rememberedSecToken = new WssProcessor.SecurityToken() {
            public Object asObject() {
                return finalcert;
            }

            public Element asElement() {
                return binarySecurityTokenElement;
            }

            public String asXmlString() {
                try {
                    return XmlUtil.elementToString(binarySecurityTokenElement);
                } catch (IOException e) {
                    return e.getMessage();
                }
            }
        };
        cntx.securityTokens.add(rememberedSecToken);
    }

    private void processSignature(Element signatureElement) {
        logger.finest("Processing Signature");
        // todo
    }

    private WssProcessor.ProcessorResult produceResult(final ProcessingStatusHolder cntx) {
        return new WssProcessor.ProcessorResult() {
            public Document getUndecoratedMessage() {
                return cntx.processedDocument;
            }

            public Element[] getElementsThatWereSigned() {
                return (Element[])cntx.elementsThatWereSigned.toArray(new Element[0]);
            }

            public Element[] getElementsThatWereEncrypted() {
                return (Element[])cntx.elementsThatWereEncrypted.toArray(new Element[0]);
            }

            public WssProcessor.SecurityToken[] getSecurityTokens() {
                return (WssProcessor.SecurityToken[])cntx.securityTokens.toArray(new WssProcessor.SecurityToken[0]);
            }

            public WssProcessor.Timestamp getTimestamp() {
                return cntx.timestamp;
            }
        };
    }

    private static final Logger logger = Logger.getLogger(WssProcessorImpl.class.getName());

    private class ProcessingStatusHolder {
        Document processedDocument = null;
        Document originalDocument = null;
        final Collection elementsThatWereSigned = new ArrayList();
        final Collection elementsThatWereEncrypted = new ArrayList();
        final Collection securityTokens = new ArrayList();
        WssProcessor.Timestamp timestamp = null;
        Element releventSecurityHeader = null;
    }
    public static final String SUPPORTED_ENCRYPTEDKEY_ALGO = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
}
