package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.*;
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
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.Cipher;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * @throws InvalidDocumentFormatException if there is a problem with the document format that can't be ignored
     * @throws GeneralSecurityException if there is a problem with a key or certificate
     * @throws ProcessorException in case of some other problem
     */
    public WssProcessor.ProcessorResult undecorateMessage(Document soapMsg,
                                                          X509Certificate recipientCert,
                                                          PrivateKey recipientKey)
            throws WssProcessor.ProcessorException, InvalidDocumentFormatException, GeneralSecurityException
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
        cntx.releventSecurityHeader = SoapUtil.getSecurityElement(cntx.processedDocument);

        // maybe there are no security headers at all in which case, there is nothing to process
        if (cntx.releventSecurityHeader == null) {
            logger.finer("No relevent security header found.");
            return produceResult(cntx);
        }

        // Process elements one by one
        Element securityChildToProcess = XmlUtil.findFirstChildElement(cntx.releventSecurityHeader);
        while (securityChildToProcess != null) {
            if (securityChildToProcess.getLocalName().equals(SoapUtil.ENCRYPTEDKEY_EL_NAME)) {
                processEncryptedKey(securityChildToProcess, recipientKey,
                                    recipientCert, cntx);
                // if this element is processed BEFORE the signature validation, it should be removed
                // for the signature to validate properly
                securityChildToProcess.getParentNode().removeChild(securityChildToProcess);
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.TIMESTAMP_EL_NAME)) {
                processTimestamp(cntx, securityChildToProcess);
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.BINARYSECURITYTOKEN_EL_NAME)) {
                processBinarySecurityToken(securityChildToProcess, cntx);
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SIGNATURE_EL_NAME)) {
                processSignature(securityChildToProcess, cntx);
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.USERNAME_TOK_EL_NAME)) {
                processUsernameToken(securityChildToProcess, cntx);
            } else {
                // Unhandled child elements of the Security Header
                String mu = securityChildToProcess.getAttributeNS(currentSoapNamespace,
                                                                  SoapUtil.MUSTUNDERSTAND_ATTR_NAME).trim();
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
            Node nextSibling = securityChildToProcess.getNextSibling();
            while (nextSibling != null && nextSibling.getNodeType() != Node.ELEMENT_NODE) {
                nextSibling = nextSibling.getNextSibling();
            }
            if (nextSibling != null && nextSibling.getNodeType() == Node.ELEMENT_NODE) {
                securityChildToProcess = (Element)nextSibling;
            } else securityChildToProcess = null;
        }

        // Backward compatibility - if we didn't see a timestamp in the security header, look up in the soap header
        Element header = (Element)cntx.releventSecurityHeader.getParentNode();
        if (cntx.timestamp == null) {
            // (header can't be null or we wouldn't be here)
            Element timestamp = XmlUtil.findFirstChildElementByName(header,
                                                                    SoapUtil.WSU_URIS_ARRAY,
                                                                    SoapUtil.TIMESTAMP_EL_NAME);
            if (timestamp != null)
                processTimestamp(cntx, timestamp);
        }

        // Remember the WSA stuff in the header if present
        Element msgIdEl = XmlUtil.findFirstChildElementByName(header,
                                                              new String[] {SoapUtil.WSA_NAMESPACE,
                                                                            SoapUtil.WSA_NAMESPACE2},
                                                              "MessageID");
        if (msgIdEl != null) {
            cntx.wsaMessageId = XmlUtil.getTextValue(msgIdEl);
        }
        Element relatesToEl = XmlUtil.findFirstChildElementByName(header,
                                                                  new String[] {SoapUtil.WSA_NAMESPACE,
                                                                                SoapUtil.WSA_NAMESPACE2},
                                                                  "RelatesTo");
        if (relatesToEl != null) {
            cntx.wsaRelatesTo = XmlUtil.getTextValue(relatesToEl);
        }

        // remove Security element altogether
        cntx.releventSecurityHeader.getParentNode().removeChild(cntx.releventSecurityHeader);

        // if there were other security headers and one with a special actor set by the agent, we
        // want to change the actor here to set it back to default value
        // in the case of multiple wrapped actors, the one with the higest value must be stripped
        Element secHeaderDeservingPromotion = null;
        List remainingSecurityHeaders = SoapUtil.getSecurityElements(cntx.processedDocument);
        long currentGen = -1;
        for (Iterator secIt = remainingSecurityHeaders.iterator(); secIt.hasNext();) {
            Element secHeader = (Element)secIt.next();
            String actor = secHeader.getAttributeNS(currentSoapNamespace, SoapUtil.ACTOR_ATTR_NAME);
            if (actor.startsWith(SoapUtil.ACTOR_LAYER7_WRAPPED)) {
                long generationOfWrappedSecHeader = 0;
                if (actor.length() > SoapUtil.ACTOR_LAYER7_WRAPPED.length()) {
                    generationOfWrappedSecHeader = Long.parseLong(actor.substring(SoapUtil.ACTOR_LAYER7_WRAPPED.length()));
                }
                if (secHeaderDeservingPromotion == null) {
                    secHeaderDeservingPromotion = secHeader;
                    currentGen = generationOfWrappedSecHeader;
                } else {
                    // remember this one only if it has a higher value
                    if (currentGen < generationOfWrappedSecHeader) {
                        currentGen = generationOfWrappedSecHeader;
                        secHeaderDeservingPromotion = secHeader;
                    }
                }
            }
        }
        if (secHeaderDeservingPromotion != null) {
            logger.info("Unwraping wrapped security header");
            secHeaderDeservingPromotion.removeAttributeNS(currentSoapNamespace, SoapUtil.ACTOR_ATTR_NAME);
        }
        return produceResult(cntx);
    }

    private void processUsernameToken(final Element usernameTokenElement, ProcessingStatusHolder cntx)
                                        throws ProcessorException, InvalidDocumentFormatException {
        String applicableWsseNS = usernameTokenElement.getNamespaceURI();
        // Get the Username child element
        Element usernameEl = XmlUtil.findOnlyOneChildElementByName(usernameTokenElement,
                                                                   applicableWsseNS,
                                                                   SoapUtil.UNTOK_USERNAME_EL_NAME);
        if (usernameEl == null) {
            throw new InvalidDocumentFormatException("The usernametoken element does not contain a username element");
        }
        String username = XmlUtil.getTextValue(usernameEl).trim();
        if (username.length() < 1) {
            throw new InvalidDocumentFormatException("The usernametoken has an empty username element");
        }
        // Get the password element
        Element passwdEl = XmlUtil.findOnlyOneChildElementByName(usernameTokenElement,
                                                                 applicableWsseNS,
                                                                 SoapUtil.UNTOK_PASSWORD_EL_NAME);
        if (passwdEl == null) {
            throw new InvalidDocumentFormatException("The usernametoken element does not contain a password element");
        }
        String passwd = XmlUtil.getTextValue(passwdEl).trim();
        if (passwd.length() < 1) {
            throw new InvalidDocumentFormatException("The usernametoken has an empty password element");
        }
        // Verify the password type to be supported
        String passwdType = passwdEl.getAttribute(SoapUtil.UNTOK_PSSWD_TYPE_ATTR_NAME).trim();
        if (passwdType.length() > 0) {
            if (!passwdType.endsWith("PasswordText")) {
                throw new ProcessorException("This username token password type is not supported: " + passwdType);
            }
        }
        // Remember this as a security token
        final LoginCredentials creds = new LoginCredentials(username, passwd.getBytes());
        WssProcessor.SecurityToken rememberedSecToken = new WssProcessor.SecurityToken() {
            public Object asObject() {
                return creds;
            }
            public Element asElement() {
                return usernameTokenElement;
            }

            public String asXmlString() {
                try {
                    return XmlUtil.nodeToString(usernameTokenElement);
                } catch (IOException e) {
                    return e.getMessage();
                }
            }
        };
        cntx.securityTokens.add(rememberedSecToken);
    }

    private void processEncryptedKey(Element encryptedKeyElement,
                                     PrivateKey recipientKey,
                                     X509Certificate recipientCert,
                                     ProcessingStatusHolder cntx)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException
    {
        logger.finest("Processing EncryptedKey");

        // If there's a KeyIdentifier, log whether it's talking about our key
        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
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
                    String valueType = ki.getAttribute("ValueType");
                    String keyIdentifierValue = XmlUtil.getTextValue(ki);
                    byte[] keyIdValueBytes = new byte[0];
                    try {
                        keyIdValueBytes = HexUtils.decodeBase64(keyIdentifierValue, true);
                    } catch (IOException e) {
                        throw new InvalidDocumentFormatException("Unable to parse base64 Key Identifier", e);
                    }

                    if (keyIdValueBytes != null) {
                        if (valueType.length() < 1 || valueType.equals(SoapUtil.VALUETYPE_SKI)) {
                            // If not typed, assume it's a ski
                            byte[] ski = recipientCert.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
                            if (ski == null) {
                                logger.warning("This EncryptedKey has a KeyInfo that apparently requests a specific SKI, " +
                                            "but our certificate does not have a SKI.  Will try to decrypt anyway.");
                                /* FALLTHROUGH */
                            } else {
                                // trim if necessary
                                byte[] ski2 = ski;
                                if (ski.length > keyIdValueBytes.length) {
                                    ski2 = new byte[keyIdValueBytes.length];
                                    System.arraycopy(ski, ski.length-keyIdValueBytes.length,
                                                     ski2, 0, keyIdValueBytes.length);
                                }
                                if (Arrays.equals(keyIdValueBytes, ski2)) {
                                    logger.fine("the Key SKI is recognized");
                                    /* FALLTHROUGH */
                                } else {
                                    logger.warning("This EncryptedKey has a KeyInfo that apparently requests a specific SKI, " +
                                                "but our certificate's SKI does not match.  Will try to decrypt anyway.");
                                    /* FALLTHROUGH */
                                }
                            }
                        } else if (valueType.equals(SoapUtil.VALUETYPE_X509)) {
                            // It seems to be a complete certificate
                            X509Certificate referencedCert = (X509Certificate)CertificateFactory.getInstance("X.509").
                                                                generateCertificate(new ByteArrayInputStream(keyIdValueBytes));
                            if (recipientCert.equals(referencedCert)) {
                                logger.fine("the Key recipient cert is recognized");
                                /* FALLTHROUGH */

                            } else {
                                logger.warning("This EncryptedKey has a KeyInfo that apparently requests a specific cert, " +
                                            "but our certificate does not match.  Will try to decrypt anyway.");
                                /* FALLTHROUGH */
                            }
                        } else
                            throw new InvalidDocumentFormatException("The EncryptedKey's KeyInfo uses an unsupported " +
                                                                     "ValueType: " + valueType);
                    }
                }
            }
        }


        // verify that the algo is supported
        Element encryptionMethodEl = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                           SoapUtil.XMLENC_NS,
                                                                           "EncryptionMethod");
        if (encryptionMethodEl != null) {
            String encMethodValue = encryptionMethodEl.getAttribute("Algorithm");
            if (encMethodValue == null || encMethodValue.length() < 1) {
                logger.warning("Algorithm not specified in EncryptionMethod element");
                return;
            } else if (!encMethodValue.equals(SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO)) {
                logger.warning("Algorithm not supported " + encMethodValue);
                return;
            }
        }

        // get the xenc:CipherValue
        Element cipherValue = null;
        Element cipherData = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                   SoapUtil.XMLENC_NS,
                                                                   "CipherData");
        if (cipherData != null) {
            cipherValue = XmlUtil.findOnlyOneChildElementByName(cipherData, SoapUtil.XMLENC_NS, "CipherValue");
        }
        if (cipherValue == null) {
            logger.warning("element missing CipherValue");
            return;
        }
        // we got the value, decrypt it
        String value = XmlUtil.getTextValue(cipherValue);
        byte[] encryptedKeyBytes = new byte[0];
        try {
            encryptedKeyBytes = HexUtils.decodeBase64(value);
        } catch (IOException e) {
            throw new InvalidDocumentFormatException("Unable to parse base64 EncryptedKey CipherValue", e);
        }
        Cipher rsa = null;
        rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.DECRYPT_MODE, recipientKey);

        byte[] decryptedPadded = rsa.doFinal(encryptedKeyBytes);
        // unpad
        byte[] unencryptedKey = null;
        try {
            unencryptedKey = unPadRSADecryptedSymettricKey(decryptedPadded);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "The key could not be unpadded", e);
            throw new ProcessorException(e);
        }

        // We got the key. Get the list of elements to decrypt.
        Element refList = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                SoapUtil.XMLENC_NS,
                                                                "ReferenceList");
        try {
            decryptReferencedElements(new AesKey(unencryptedKey, unencryptedKey.length*8), refList, cntx);
        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
        } catch (IOException e) {
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
            ProcessorException
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
            Element encryptedDataElement = SoapUtil.getElementByWsuId(refList.getOwnerDocument(), dataRefUri);
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
            String dateString = XmlUtil.getTextValue(element);
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
                return XmlUtil.nodeToString(element);
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    }

    private void processTimestamp(ProcessingStatusHolder ctx, final Element timestampElement)
            throws InvalidDocumentFormatException
    {
        logger.finest("Processing Timestamp");
        final Element created = XmlUtil.findOnlyOneChildElementByName(timestampElement,
                                                                      SoapUtil.WSU_URIS_ARRAY,
                                                                      SoapUtil.CREATED_EL_NAME);
        final Element expires = XmlUtil.findOnlyOneChildElementByName(timestampElement,
                                                                      SoapUtil.WSU_URIS_ARRAY,
                                                                      SoapUtil.EXPIRES_EL_NAME);

        final TimestampDate createdTimestampDate;
        final TimestampDate expiresTimestampDate;
        try {
            createdTimestampDate = created == null ? null : new TimestampDate(created);
            expiresTimestampDate = expires == null ? null : new TimestampDate(expires);
        } catch (ParseException e) {
            throw new InvalidDocumentFormatException("Unable to parse Timestamp", e);
        }

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
                    return XmlUtil.nodeToString(timestampElement);
                } catch (IOException e) {
                    throw new RuntimeException(e); // can't happen
                }
            }
        };
    }

    private void processBinarySecurityToken(final Element binarySecurityTokenElement,
                                            ProcessingStatusHolder cntx)
            throws ProcessorException, GeneralSecurityException, InvalidDocumentFormatException
    {
        logger.finest("Processing BinarySecurityToken");

        // assume that this is a b64ed binary x509 cert, get the value
        String valueType = binarySecurityTokenElement.getAttribute("ValueType");
        String encodingType = binarySecurityTokenElement.getAttribute("EncodingType");
        
        // todo use proper qname comparator rather than this hacky suffix check
        if (!valueType.endsWith("X509v3"))
            throw new ProcessorException("BinarySecurityToken has unsupported ValueType " + valueType);
        if (!encodingType.endsWith("Base64Binary"))
            throw new ProcessorException("BinarySecurityToken has unsupported EncodingType " + encodingType);

        String value = XmlUtil.getTextValue(binarySecurityTokenElement);
        if (value == null || value.length() < 1) {
            String msg = "The " + binarySecurityTokenElement.getLocalName() + " does not contain a value.";
            logger.warning(msg);
            throw new ProcessorException(msg);
        }

        byte[] decodedValue = new byte[0]; // must strip whitespace or base64 decoder misbehaves
        try {
            decodedValue = HexUtils.decodeBase64(value, true);
        } catch (IOException e) {
            throw new InvalidDocumentFormatException("Unable to parse base64 BinarySecurityToken", e);
        }
        // create the x509 binary cert based on it
        X509Certificate referencedCert = (X509Certificate)CertificateFactory.getInstance("X.509").
                                            generateCertificate(new ByteArrayInputStream(decodedValue));

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
                    return XmlUtil.nodeToString(binarySecurityTokenElement);
                } catch (IOException e) {
                    return e.getMessage();
                }
            }
        };
        cntx.securityTokens.add(rememberedSecToken);
    }

    private X509Certificate resolveCertByRef(final Element parentElement, ProcessingStatusHolder cntx) {

        // Looking for reference to a wsse:BinarySecurityToken
        // 1. look for a wsse:SecurityTokenReference element
        List secTokReferences = XmlUtil.findChildElementsByName(parentElement,
                                                          SoapUtil.SECURITY_URIS_ARRAY,
                                                          "SecurityTokenReference");
        if (secTokReferences.size() > 0) {
            // 2. Resolve the child reference
            Element securityTokenReference = (Element)secTokReferences.get(0);
            List references = XmlUtil.findChildElementsByName(securityTokenReference,
                                                              SoapUtil.SECURITY_URIS_ARRAY,
                                                              "Reference");
            if (references.size() > 0) {
                // get the URI
                Element reference = (Element)references.get(0);
                String uriAttr = reference.getAttribute("URI");
                if (uriAttr == null || uriAttr.length() < 1) {
                    // not the food additive
                    String msg = "The Key info contains a reference but the URI attribute cannot be obtained";
                    logger.warning(msg);

                }
                if (uriAttr.charAt(0) == '#') {
                    uriAttr = uriAttr.substring(1);
                }
                // look for previous sec tokens with that same id
                for (Iterator i = cntx.securityTokens.iterator(); i.hasNext();) {
                    WssProcessor.SecurityToken token = (WssProcessor.SecurityToken)i.next();
                    if (uriAttr.equals(SoapUtil.getElementWsuId(token.asElement()))) {
                        if (token.asObject() instanceof X509Certificate) {
                            return (X509Certificate)token.asObject();
                        } else {
                            logger.warning("the corresponding token was found but is not a cert");
                        }
                    }
                }
            } else {
                logger.warning("SecurityTokenReference does not contain any References");
            }
        }
        return null;
    }

    private X509Certificate resolveEmbeddedCert(final Element parentElement) {
        // Attempt to get the cert directly from the KeyInfo element
        KeyInfo keyInfo = null;
        try {
            keyInfo = new KeyInfo(parentElement);
        } catch (XSignatureException e) {
            // dont throw here on purpose, this should return null if not sucessful to give a chance to
            // alternate methods to get the cert
            logger.log(Level.FINE, "could not construct key info element from the parent element", e);
            return null;
        }
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();
        if (x509DataArray != null && x509DataArray.length > 0) {
            KeyInfo.X509Data x509Data = x509DataArray[0];
            X509Certificate[] certs = x509Data.getCertificates();
            // according to javadoc, this can be null
            if (certs == null || certs.length < 1) {
                return null;
            }
            return certs[0];
        }
        return null;
    }

    private void processSignature(final Element sigElement, ProcessingStatusHolder cntx) throws ProcessorException {
        logger.finest("Processing Signature");

        // 1st, process the KeyInfo
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) {
            throw new ProcessorException("KeyInfo element not found in Signature Element");
        }

        // Try to resolve cert by reference
        X509Certificate signingCert = resolveCertByRef(keyInfoElement, cntx);
        // Try to resolve embedded cert
        if (signingCert == null) {
            signingCert = resolveEmbeddedCert(keyInfoElement);
        }

        if (signingCert == null) throw new ProcessorException("no cert to verify signature against.");

        // Validate signature
        PublicKey pubKey = signingCert.getPublicKey();
        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
                                   public Element resolveID(Document doc, String s) {
                                       return SoapUtil.getElementByWsuId(doc, s);
                                   }
                               });
        Validity validity = sigContext.verify(sigElement, pubKey);

        if (!validity.getCoreValidity()) {
            StringBuffer msg = new StringBuffer("Validity not achieved. " + validity.getSignedInfoMessage());
            for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                msg.append("\n\tElement " + validity.getReferenceURI(i) + ": " + validity.getReferenceMessage(i));
            }
            logger.warning(msg.toString());
            throw new ProcessorException(msg.toString());
        }

        // Remember which elements were covered
        for (int i = 0; i < validity.getNumberOfReferences(); i++) {
            // Resolve each elements one by one. Use original document as the elements signed might no longer exist
            // once the document is completly processed (signature might cover elements in the Security header that
            // will later be purged).
            String elementCoveredURI = validity.getReferenceURI(i);
            Element elementCovered = SoapUtil.getElementByWsuId(cntx.originalDocument, elementCoveredURI);
            if (elementCovered == null) {
                // i guess the element might be in the processed document (something decrypted was later signed)
                elementCovered = SoapUtil.getElementByWsuId(sigElement.getOwnerDocument(), elementCoveredURI);
            }
            if (elementCovered == null) {
                String msg = "Element covered by signature cannot be found in original document nor in " +
                             "processed document. URI: " + elementCoveredURI;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }
            // make reference to this element
            cntx.elementsThatWereSigned.add(elementCovered);
        }
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

            public String getWsaMessageId() {
                return cntx.wsaMessageId;
            }

            public String getWsaRelatesTo() {
                return cntx.wsaRelatesTo;
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
        String wsaMessageId = null;
        String wsaRelatesTo = null;
    }
}
