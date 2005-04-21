package com.l7tech.common.security.xml.processor;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.EncryptedData;
import com.l7tech.common.message.Message;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.saml.SamlException;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.SecureConversationKeyDeriver;
import com.l7tech.common.security.xml.SecurityActor;
import com.l7tech.common.security.xml.UnexpectedKeyInfoException;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.UnsupportedDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the WssProcessor for use in both the SSG and the SSA.
 * <p/>
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

    public ProcessorResult undecorateMessage(Message message,
                                             X509Certificate recipientCert,
                                             PrivateKey recipientKey,
                                             SecurityContextFinder securityContextFinder)
      throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, BadSecurityContextException, SAXException, IOException
    {
        // Reset all potential outputs
        Document soapMsg = message.getXmlKnob().getDocumentReadOnly();
        ProcessingStatusHolder cntx = new ProcessingStatusHolder(message, soapMsg);
        cntx.elementsThatWereSigned.clear();
        cntx.elementsThatWereEncrypted.clear();
        cntx.securityTokens.clear();
        cntx.timestamp = null;
        cntx.releventSecurityHeader = null;
        cntx.elementsByWsuId = SoapUtil.getElementByWsuIdMap(soapMsg);

        String currentSoapNamespace = soapMsg.getDocumentElement().getNamespaceURI();

        // Resolve the relevent Security header
        Element l7secheader = SoapUtil.getSecurityElement(cntx.processedDocument, SecurityActor.L7ACTOR.getValue());
        Element noactorsecheader = SoapUtil.getSecurityElement(cntx.processedDocument);
        if (l7secheader != null) {
            cntx.releventSecurityHeader = l7secheader;
            cntx.secHeaderActor = SecurityActor.L7ACTOR;
        } else {
            cntx.releventSecurityHeader = noactorsecheader;
            if (cntx.releventSecurityHeader != null) {
                cntx.secHeaderActor = SecurityActor.NOACTOR;
            }
        }

        // maybe there are no security headers at all in which case, there is nothing to process
        if (cntx.releventSecurityHeader == null) {
            logger.finer("No relevent security header found.");
            return produceResult(cntx);
        }

        // Process elements one by one
        Element securityChildToProcess = XmlUtil.findFirstChildElement(cntx.releventSecurityHeader);
        while (securityChildToProcess != null) {
            boolean removeProcessedElement = false;

            if (securityChildToProcess.getLocalName().equals(SoapUtil.ENCRYPTEDKEY_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.XMLENC_NS)) {
                    // http://www.w3.org/2001/04/xmlenc#
                    boolean res = processEncryptedKey(securityChildToProcess, recipientKey,
                        recipientCert, cntx);
                    // if this element is processed BEFORE the signature validation, it should be removed
                    // for the signature to validate properly
                    // fla added (but only if the ec was actually processed)
                    if (res) {
                        removeProcessedElement = true;
                    }
                } else {
                    logger.finer("Encountered EncryptedKey element but not of right namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.TIMESTAMP_EL_NAME)) {
                if (elementHasNamespace(securityChildToProcess, SoapUtil.WSU_URIS_ARRAY)) {
                    processTimestamp(cntx, securityChildToProcess);
                } else {
                    logger.fine("Encountered Timestamp element but not of right namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.BINARYSECURITYTOKEN_EL_NAME)) {
                if (elementHasNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processBinarySecurityToken(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered BinarySecurityToken element but not of right namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SIGNATURE_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.DIGSIG_URI)) {
                    processSignature(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered Signature element but not of right namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.USERNAME_TOK_EL_NAME)) {
                if (elementHasNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processUsernameToken(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered UsernameToken element but not of expected namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SECURITY_CONTEXT_TOK_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.WSSC_NAMESPACE)) {
                    final Element secConTokEl = securityChildToProcess;
                    String identifier = extractIdentifierFromSecConTokElement(secConTokEl);
                    if (identifier == null) {
                        throw new InvalidDocumentFormatException("SecurityContextToken element found, " +
                          "but its identifier was not extracted.");
                    } else {
                        if (securityContextFinder == null)
                            throw new ProcessorException("SecurityContextToken element found in message, but caller " +
                              "did not provide a SecurityContextFinder");
                        final SecurityContext secContext = securityContextFinder.getSecurityContext(identifier);
                        if (secContext == null) {
                            throw new BadSecurityContextException(identifier);
                        }
                        SecurityContextTokenImpl secConTok = new SecurityContextTokenImpl(secContext,
                            secConTokEl,
                            identifier);
                        cntx.securityTokens.add(secConTok);
                    }
                } else {
                    logger.fine("Encountered SecurityContextToken element but not of expected namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.WSSC_DK_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.WSSC_NAMESPACE)) {
                    processDerivedKey(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered DerivedKey element but not of expected namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.REFLIST_EL_NAME)) {
                // In the case of a Secure Conversation the reference list is declared outside
                // of the DerivedKeyToken
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.XMLENC_NS)) {
                    processReferenceList(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered ReferenceList element but not of expected namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SamlConstants.ELEMENT_ASSERTION)) {
                if (securityChildToProcess.getNamespaceURI().equals(SamlConstants.NS_SAML)) {
                    processSamlSecurityToken(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered SAML Assertion element but not of expected namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SECURITYTOKENREFERENCE_EL_NAME)) {
                if (elementHasNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processSecurityTokenReference(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered SecurityTokenReference element but not of expected namespace (" +
                      securityChildToProcess.getNamespaceURI() + ")");
                }
            } else {
                // Unhandled child elements of the Security Header
                String mu = securityChildToProcess.getAttributeNS(currentSoapNamespace,
                    SoapUtil.MUSTUNDERSTAND_ATTR_NAME).trim();
                if ("1".equals(mu) || "true".equalsIgnoreCase(mu)) {
                    String msg = "Unrecognized element in Security header: " +
                      securityChildToProcess.getNodeName() +
                      " with mustUnderstand=\"" + mu + "\"; rejecting message";
                    logger.warning(msg);
                    throw new ProcessorException(msg);
                } else {
                    logger.finer("Unknown element in security header: " + securityChildToProcess.getNodeName());
                }
            }
            Node nextSibling = securityChildToProcess.getNextSibling();
            if (removeProcessedElement) {
                cntx.setDocumentModified();
                securityChildToProcess.getParentNode().removeChild(securityChildToProcess);
            }
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

        // NOTE fla, we used to remove the Security header altogether but we now leave this up to the policy
        // NOTE lyonsm we don't remove the mustUnderstand attr here anymore either, since it changes the request
        // possibly-needlessly

        // If our work has left behind an empty SOAP Header, remove it too
        Element soapHeader = (Element)cntx.releventSecurityHeader.getParentNode();
        if (XmlUtil.elementIsEmpty(soapHeader)) {
            cntx.setDocumentModified(); // no worries -- empty sec header can only mean we made at least 1 change already
            soapHeader.getParentNode().removeChild(soapHeader);
        }

        return produceResult(cntx);
    }

    /**
     * Process the SecurityTokeReference. different mechanisms for referencing security tokens using the
     * <wsse:SecurityTokenReference> exist, and currently the supported are:
     * <ul>
     * <li> Key Identifier <wsse:KeyIdentifier>
     * <li> Security Token Reference <wsse:Reference>
     * </ul>
     * The usupported SecurityTokeReference types are:
     * <ul>
     * <li> X509 issuer name and issuer serial <ds:X509IssuerName>,  <ds:X509SerialNumber>
     * <li> Embedded token <wsse:Embedded>
     * <li> Key Name <ds:KeyName>
     * </ul>
     * This is as per <i>Web Services Security: SOAP Message Security 1.0 (WS-Security 2004) OASIS standard</i>
     *
     * @param str  the SecurityTokeReference element
     * @param cntx thge processing status holder/accumulator
     * @throws InvalidDocumentFormatException
     */
    private void processSecurityTokenReference(Element str, ProcessingStatusHolder cntx) throws InvalidDocumentFormatException {
        String id = SoapUtil.getElementWsuId(str);
        if (id == null || id.length() < 1) {
            logger.warning("Ignoring SecurityTokenReference with no wsu:Id");
            return;
        }

        Element keyid = XmlUtil.findFirstChildElementByName(str, str.getNamespaceURI(), "KeyIdentifier");
        String value = null;
        if (keyid == null) {
            keyid = XmlUtil.findFirstChildElementByName(str, str.getNamespaceURI(), "Reference");
            if (keyid == null) { //try reference
                logger.warning("Ignoring SecurityTokenReference ID=" + id + " with no KeyIdentifier or Reference");
                return;
            } else {
                value = keyid.getAttribute("URI");
                if (value != null && value.charAt(0) == '#') {
                    value = value.substring(1);
                }
            }
        } else {
            value = XmlUtil.getTextValue(keyid).trim();
            String encodingType = keyid.getAttribute("EncodingType");
            if (encodingType != null && encodingType.length() > 0) {
                logger.warning("Ignoring SecurityTokenReference ID=" + id + " with non-empty KeyIdentifier/@EncodingType=" + encodingType);
                return;
            }
        }
        if (value == null) {
            final String msg = "Rejecting SecurityTokenReference ID=" + id + " as the target reference ID is missing or could not be determined.";
            logger.warning(msg);
            throw new InvalidDocumentFormatException(msg);
        }

        String valueType = keyid.getAttribute("ValueType");
        if (SoapUtil.isValueTypeSaml(valueType)) {
            Element target = (Element)cntx.elementsByWsuId.get(value);
            if (target == null || !target.getLocalName().equals("Assertion") || !target.getNamespaceURI().equals(SamlConstants.NS_SAML)) {
                final String msg = "Rejecting SecurityTokenReference ID=" + id + " with ValueType of " + valueType + " because its target is either missing or not a SAML assertion";
                logger.warning(msg); // TODO remove redundant logging after debugging complete
                throw new InvalidDocumentFormatException(msg);
            }
            logger.finest("Remembering SecurityTokenReference ID=" + id + " pointing at SAML assertion " + value);
            cntx.securityTokenReferenceElementToTargetElement.put(str, target);
        } else {
            logger.warning("Ignoring SecurityTokenReference ID=" + id + " with unsupported ValueType of " + valueType);
            return;
        }
    }

    private void processReferenceList(Element referenceListEl, ProcessingStatusHolder cntx) throws ProcessorException, InvalidDocumentFormatException {
        // get each element one by one
        List dataRefEls = XmlUtil.findChildElementsByName(referenceListEl, SoapUtil.XMLENC_NS, SoapUtil.DATAREF_EL_NAME);
        if (dataRefEls == null || dataRefEls.isEmpty()) {
            logger.warning("ReferenceList is present, but is empty");
            return;
        }

        for (Iterator j = dataRefEls.iterator(); j.hasNext();) {
            Element dataRefEl = (Element)j.next();
            String dataRefUri = dataRefEl.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);
            Element encryptedDataElement = (Element)cntx.elementsByWsuId.get(dataRefUri);
            if (encryptedDataElement == null) // TODO can omit this second search if encrypted data blocks never overlap
                encryptedDataElement = SoapUtil.getElementByWsuId(referenceListEl.getOwnerDocument(), dataRefUri);
            if (encryptedDataElement == null) {
                String msg = "cannot resolve encrypted data element " + dataRefUri;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }
            // get the reference to the derived key token from the http://www.w3.org/2000/09/xmldsig#:KeyInfo element
            Element keyInfo = XmlUtil.findFirstChildElementByName(encryptedDataElement, SoapUtil.DIGSIG_URI, SoapUtil.KINFO_EL_NAME);
            if (keyInfo == null) {
                throw new InvalidDocumentFormatException("The DataReference here should contain a KeyInfo child");
            }
            DerivedKeyTokenImpl dktok = resolveDerivedKeyByRef(keyInfo, cntx);
            if (dktok == null) {
                // there are some keyinfo formats that we do not support. in that case, we should see if
                // the message can possibly just passthrough
                logger.info("The DataReference's KeyInfo did not refer to a DerivedKey." +
                            "This element will not be decrypted.");
                cntx.encryptionIgnored = true;
                return;
            }
            try {
                decryptElement(encryptedDataElement, dktok.getComputedDerivedKey(), cntx);
            } catch (GeneralSecurityException e) {
                throw new ProcessorException(e);
            } catch (ParserConfigurationException e) {
                throw new ProcessorException(e);
            } catch (IOException e) {
                throw new ProcessorException(e);
            } catch (SAXException e) {
                throw new ProcessorException(e);
            }
        }
    }

    private void processDerivedKey(Element derivedKeyEl, ProcessingStatusHolder cntx) throws InvalidDocumentFormatException {
        // get corresponding shared secret reference wsse:SecurityTokenReference
        Element sTokrefEl = XmlUtil.findFirstChildElementByName(derivedKeyEl,
            SoapUtil.SECURITY_URIS_ARRAY,
            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        if (sTokrefEl == null) throw new InvalidDocumentFormatException("DerivedKeyToken should " +
                                 "contain a SecurityTokenReference");
        Element refEl = XmlUtil.findFirstChildElementByName(sTokrefEl,
            SoapUtil.SECURITY_URIS_ARRAY,
            SoapUtil.REFERENCE_EL_NAME);
        if (refEl == null) throw new InvalidDocumentFormatException("SecurityTokenReference should " +
                             "contain a Reference");
        String refUri = refEl.getAttribute("URI");
        SecurityContextTokenImpl sct = null;
        for (Iterator i = cntx.securityTokens.iterator(); i.hasNext();) {
            Object maybeSecToken = i.next();
            if (maybeSecToken instanceof SecurityContextTokenImpl) {
                sct = (SecurityContextTokenImpl)maybeSecToken;
                String thisId = SoapUtil.getElementWsuId(sct.asElement());
                // todo, match this against the id in case this refers to more than one context (unlikely)
                break;
            }
        }
        if (sct == null) {
            throw new InvalidDocumentFormatException("could not find a security context token for this derived key");
        }
        SecureConversationKeyDeriver keyDeriver = new SecureConversationKeyDeriver();
        Key resultingKey = null;
        try {
            resultingKey = keyDeriver.derivedKeyTokenToKey(derivedKeyEl,
                sct.getSecurityContext().getSharedSecret().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDocumentFormatException(e);
        }
        final Element dktel = derivedKeyEl;
        final Key finalKey = resultingKey;
        DerivedKeyTokenImpl rememberedKeyToken = new DerivedKeyTokenImpl(dktel, finalKey, sct);
        // remember this symmetric key so it can later be used to process the signature
        // or the encryption
        cntx.derivedKeyTokens.add(rememberedKeyToken);
    }

    private String extractIdentifierFromSecConTokElement(Element secConTokEl) {
        // look for the wssc:Identifier child
        Element id = XmlUtil.findFirstChildElementByName(secConTokEl,
            SoapUtil.WSSC_NAMESPACE,
            SoapUtil.WSSC_ID_EL_NAME);
        if (id == null) return null;
        return XmlUtil.getTextValue(id);
    }

    /**
     * checks that the namespace of the passed element is one of the namespaces
     * passed
     */
    private boolean elementHasNamespace(Element el, String[] possibleNamespaces) {
        String ns = el.getNamespaceURI();
        for (int i = 0; i < possibleNamespaces.length; i++) {
            if (ns.equals(possibleNamespaces[i])) return true;
        }
        return false;
    }

    private void processUsernameToken(final Element usernameTokenElement, ProcessingStatusHolder cntx)
      throws InvalidDocumentFormatException
    {
        UsernameTokenImpl rememberedSecToken = null;
        try {
            rememberedSecToken = new UsernameTokenImpl(usernameTokenElement);
            cntx.securityTokens.add(rememberedSecToken);
        } catch (UnsupportedDocumentFormatException e) {
            // if the format is not supported, we should ignore it completly
            logger.log(Level.INFO, "A usernametoken element was encountered but we dont support the format.", e);
            return;
        }
    }

    /**
         * @return true it the encryptedKey was processed, false if the encryptedKey was ignored (intended
         *         for a downstream recipient)
         */
    private boolean processEncryptedKey(Element encryptedKeyElement,
                                        PrivateKey recipientKey,
                                        X509Certificate recipientCert,
                                        ProcessingStatusHolder cntx)
      throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException
    {
        logger.finest("Processing EncryptedKey");

        // If there's a KeyIdentifier, log whether it's talking about our key
        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
        try {
            XencUtil.checkKeyInfo(encryptedKeyElement, recipientCert);
        } catch (UnexpectedKeyInfoException e) {
            if (cntx.secHeaderActor == SecurityActor.L7ACTOR) {
                logger.warning("We do not appear to be the intended recipient for this EncryptedKey however the " +
                  "security header is clearly addressed to us");
                throw e;
            } else if (cntx.secHeaderActor == SecurityActor.NOACTOR) {
                logger.log(Level.INFO, "We do not appear to be the intended recipient for this " +
                  "EncryptedKey. Will leave it alone since the security header is not " +
                  "explicitely addressed to us.", e);
                cntx.encryptionIgnored = true;
                return false;
            }
        }

        // verify that the algo is supported
        XencUtil.checkEncryptionMethod(encryptedKeyElement);

        // Extract the encrypted key
        byte[] unencryptedKey = XencUtil.decryptKey(encryptedKeyElement, recipientKey);

        // We got the key. Get the list of elements to decrypt.
        Element refList = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
            SoapUtil.XMLENC_NS,
            SoapUtil.REFLIST_EL_NAME);
        try {
            decryptReferencedElements(new AesKey(unencryptedKey, unencryptedKey.length * 8), refList, cntx);
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
        return true;
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
     *                if the message contains an encryptedkey
     * @throws java.security.GeneralSecurityException
     *                                  if there was a problem with a key or crypto provider
     * @throws javax.xml.parsers.ParserConfigurationException
     *                                  if there was a problem with the XML parser
     * @throws IOException              if there was an IO error while reading the document or a key
     * @throws org.xml.sax.SAXException if there was a problem parsing the document
     */
    public void decryptReferencedElements(Key key, Element refList, ProcessingStatusHolder cntx)
      throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
      ProcessorException, InvalidDocumentFormatException
    {
        List dataRefEls = XmlUtil.findChildElementsByName(refList, SoapUtil.XMLENC_NS, SoapUtil.DATAREF_EL_NAME);
        if (dataRefEls == null || dataRefEls.isEmpty()) {
            logger.warning("EncryptedData is present, but contains at least one empty ReferenceList");
            return;
        }

        for (Iterator j = dataRefEls.iterator(); j.hasNext();) {
            Element dataRefEl = (Element)j.next();
            String dataRefUri = dataRefEl.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);
            Element encryptedDataElement = (Element)cntx.elementsByWsuId.get(dataRefUri);
            if (encryptedDataElement == null) // TODO can omit this second search if encrypted sections never overlap
                encryptedDataElement = SoapUtil.getElementByWsuId(refList.getOwnerDocument(), dataRefUri);
            if (encryptedDataElement == null) {
                String msg = "cannot resolve encrypted data element " + dataRefUri;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }

            decryptElement(encryptedDataElement, key, cntx);
        }
    }

    private void decryptElement(Element encryptedDataElement, Key key, ProcessingStatusHolder cntx)
      throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
      ProcessorException, InvalidDocumentFormatException
    {
        Node parent = encryptedDataElement.getParentNode();
        if (parent == null || !(parent instanceof Element))
            throw new InvalidDocumentFormatException("Root of document is encrypted"); // sanity check, can't happen
        Element parentElement = (Element)encryptedDataElement.getParentNode();

        // See if the parent element contains nothing else except attributes and this EncryptedData element
        // (and possibly a whitespace node before and after it)
        // TODO trim() throws out all CTRL characters along with whitespace.  Need to think about this.
        cntx.setDocumentModified();
        Node nextWhitespace = null;
        Node nextSib = encryptedDataElement.getNextSibling();
        if (nextSib != null && nextSib.getNodeType() == Node.TEXT_NODE && nextSib.getNodeValue().trim().length() < 1)
            nextWhitespace = nextSib;
        Node prevWhitespace = null;
        Node prevSib = encryptedDataElement.getPreviousSibling();
        if (prevSib != null && prevSib.getNodeType() == Node.TEXT_NODE && prevSib.getNodeValue().trim().length() < 1)
            prevWhitespace = prevSib;

        boolean onlyChild = true;
        NodeList sibNodes = parentElement.getChildNodes();
        for (int i = 0; i < sibNodes.getLength(); ++i) {
            Node node = sibNodes.item(i);
            if (node == null || node.getNodeType() == Node.ATTRIBUTE_NODE)
                continue; // not relevant
            if (node == nextWhitespace || node == prevWhitespace)
                continue; // ignore
            if (node == encryptedDataElement)
                continue; // this is the encrypteddata element itself

            // we've found a relevant sibling, proving that not all of parentElement's non-attribute content
            // is encrypted within this EncryptedData
            onlyChild = false;
            break;
        }

        // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
        // soapMsg document
        DecryptionContext dc = new DecryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        af.setProvider(JceProvider.getSymmetricJceProvider().getName());
        dc.setAlgorithmFactory(af);
        dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT,
          null, null);
        dc.setKey(key);
        NodeList dataList;
        try {
            // do the actual decryption
            dc.decrypt();
            dc.replace();
            // remember encrypted element
            dataList = dc.getDataAsNodeList();
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

        // Now record the fact that some data was encrypted.
        // Did the parent element contain any non-attribute content other than this EncryptedData
        // (and possibly some whitespace before and after)?
        if (onlyChild) {
            // All relevant content of the parent node was encrypted.
            logger.finer("All of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            cntx.elementsThatWereEncrypted.add(new ParsedElementImpl(parentElement));
        } else {
            // There was unencrypted stuff mixed in with the EncryptedData, so we can only record elements as
            // encrypted that were actually wholly inside the EncryptedData.
            // TODO: In this situation, no note is taken of any encrypted non-Element nodes (such as text nodes)
            // This sucks, but at lesat this will err on the side of safety.
            logger.finer("Only some of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            for (int i = 0; i < dataList.getLength(); i++) {
                Node node = dataList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    cntx.elementsThatWereEncrypted.add(new ParsedElementImpl((Element)node));
                }
            }
        }
    }

    private static class TimestampDate extends ParsedElementImpl implements com.l7tech.common.security.xml.processor.WssTimestampDate {
        Date date;

        TimestampDate(Element createdOrExpiresElement) throws ParseException {
            super(createdOrExpiresElement);
            String dateString = XmlUtil.getTextValue(createdOrExpiresElement);
            date = ISO8601Date.parse(dateString);
        }

        public Date asDate() {
            return date;
        }
    }

    private void processTimestamp(ProcessingStatusHolder ctx, final Element timestampElement)
      throws InvalidDocumentFormatException
    {
        logger.finest("Processing Timestamp");
        if (ctx.timestamp != null)
            throw new InvalidDocumentFormatException("More than one Timestamp element was encountered in the Security header");

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

        ctx.timestamp = new TimestampImpl(createdTimestampDate, expiresTimestampDate, timestampElement);
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
        X509Certificate referencedCert = CertUtils.decodeCert(decodedValue);

        // remember this cert
        final String wsuId = SoapUtil.getElementWsuId(binarySecurityTokenElement);
        if (wsuId == null) {
            logger.warning("This BinarySecurityToken does not have a recognized wsu:Id and may not be " +
              "referenced properly by a subsequent signature.");
        }
        final X509Certificate finalcert = referencedCert;
        SecurityToken rememberedSecToken = new X509BinarySecurityTokenImpl(finalcert,
            binarySecurityTokenElement);
        cntx.securityTokens.add(rememberedSecToken);
        cntx.x509TokensById.put(wsuId, rememberedSecToken);
    }

    private void processSamlSecurityToken(final Element securityTokenElement, ProcessingStatusHolder context)
      throws InvalidDocumentFormatException
    {
        logger.finest("Processing saml:Assertion XML SecurityToken");
        try {
            final SamlAssertion samlToken = new SamlAssertion(securityTokenElement);
            if (samlToken.hasEmbeddedIssuerSignature()) {
                samlToken.verifyEmbeddedIssuerSignature();

                class EmbeddedSamlSignatureToken implements X509SecurityToken {
                    public X509Certificate asX509Certificate() {
                        return samlToken.getIssuerCertificate();
                    }

                    /**
                     * @return true if the sender has proven its possession of the private key corresponding to this security token.
                     *         This is done by signing one or more elements of the message with it.
                     */
                    public boolean isPossessionProved() {
                        return false;
                    }

                    /**
                     * @return An array of elements signed by this signing security token.  May be empty but never null.
                     */
                    public SignedElement[] getSignedElements() {
                        SignedElement se = new SignedElement() {
                            public SigningSecurityToken getSigningSecurityToken() {
                                return EmbeddedSamlSignatureToken.this;
                            }

                            public Element asElement() {
                                return samlToken.asElement();
                            }
                        };

                        return new SignedElement[]{se};
                    }

                    public SecurityTokenType getType() {
                        return SecurityTokenType.X509;
                    }

                    public String getElementId() {
                        return samlToken.getElementId();
                    }

                    public Element asElement() {
                        return samlToken.asElement();
                    }
                }

                // Add the fake X509SecurityToken that signed the assertion
                final EmbeddedSamlSignatureToken samlSignatureToken = new EmbeddedSamlSignatureToken();
                context.securityTokens.add(samlSignatureToken);

                final SignedElement signedElement = new SignedElement() {
                    public SigningSecurityToken getSigningSecurityToken() {
                        return samlSignatureToken;
                    }

                    public Element asElement() {
                        return securityTokenElement;
                    }
                };

                context.elementsThatWereSigned.add(signedElement);
            }

            // Add the assertion itself
            context.securityTokens.add(samlToken);
            context.x509TokensById.put(samlToken.getElementId(), samlToken);
        } catch (SAXException e) {
            throw new InvalidDocumentFormatException(e);
        } catch (SamlException e) {
            throw new InvalidDocumentFormatException(e);
        } catch (SignatureException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    private MutableX509SigningSecurityToken resolveCertByRef(final Element parentElement, ProcessingStatusHolder cntx) {
        // TODO SAML Assertion reference by URI (Bug #1434)
        // Looking for reference to a wsse:BinarySecurityToken or to a derived key
        // 1. look for a wsse:SecurityTokenReference element
        List secTokReferences = XmlUtil.findChildElementsByName(parentElement,
            SoapUtil.SECURITY_URIS_ARRAY,
            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        if (secTokReferences.size() > 0) {
            // 2. Resolve the child reference
            Element securityTokenReference = (Element)secTokReferences.get(0);
            List references = XmlUtil.findChildElementsByName(securityTokenReference,
                SoapUtil.SECURITY_URIS_ARRAY,
                SoapUtil.REFERENCE_EL_NAME);
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
                // try to see if this reference matches a previously parsed SigningSecurityToken
                final MutableX509SigningSecurityToken token = (MutableX509SigningSecurityToken)cntx.x509TokensById.get(uriAttr);
                if (token != null) {
                    logger.finest("The keyInfo referred to a previously parsed Security Token '" + uriAttr + "'");
                    return token;
                } else {
                    logger.fine("The reference " + uriAttr + " did not point to a X509Cert.");
                }
            } else {
                logger.warning("SecurityTokenReference does not contain any References");
            }
        }
        return null;
    }

    private DerivedKeyTokenImpl resolveDerivedKeyByRef(final Element parentElement, ProcessingStatusHolder cntx) {

        // Looking for reference to a a derived key token
        // 1. look for a wsse:SecurityTokenReference element
        List secTokReferences = XmlUtil.findChildElementsByName(parentElement,
            SoapUtil.SECURITY_URIS_ARRAY,
            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        if (secTokReferences.size() > 0) {
            // 2. Resolve the child reference
            Element securityTokenReference = (Element)secTokReferences.get(0);
            List references = XmlUtil.findChildElementsByName(securityTokenReference,
                SoapUtil.SECURITY_URIS_ARRAY,
                SoapUtil.REFERENCE_EL_NAME);
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
                for (Iterator i = cntx.derivedKeyTokens.iterator(); i.hasNext();) {
                    Object maybeDerivedKey = i.next();
                    if (maybeDerivedKey instanceof DerivedKeyTokenImpl) {
                        if (((DerivedKeyTokenImpl)maybeDerivedKey).getElementId().equals(uriAttr)) {
                            return (DerivedKeyTokenImpl)maybeDerivedKey;
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

    private void processSignature(final Element sigElement, final ProcessingStatusHolder cntx)
      throws ProcessorException, InvalidDocumentFormatException
    {
        logger.finest("Processing Signature");

        // 1st, process the KeyInfo
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) {
            throw new ProcessorException("KeyInfo element not found in Signature Element");
        }

        X509Certificate signingCert = null;
        Key signingKey = null;
        // Try to find ref to derived key
        final DerivedKeyTokenImpl dkt = resolveDerivedKeyByRef(keyInfoElement, cntx);
        // Try to resolve cert by reference
        final MutableX509SigningSecurityToken signingCertToken = resolveCertByRef(keyInfoElement, cntx);

        if (signingCertToken != null) {
            signingCert = signingCertToken.getMessageSigningCertificate();
        }
        if (signingCert == null) { //try to resolve it as embedded
            signingCert = resolveEmbeddedCert(keyInfoElement);
        }

        if (signingCert == null && dkt != null) {
            signingKey = dkt.getComputedDerivedKey();
        } else if (signingCert != null) {
            signingKey = signingCert.getPublicKey();
        }

        if (signingKey == null) {
            // some toolkits can base their signature on a usernametoken
            // although the ssg does not support that, we should not throw
            // but just ignore this signature because the signature may be
            // useful for a downstream service (see bugzilla #1585)
            String msg = "Was not able to get cert or derived key from signature's keyinfo. ignoring this signature";
            logger.info(msg);
            // dont throw, just ignore signature!
            return;
        }

        if (signingCert != null) {
            try {
                CertUtils.checkValidity(signingCert);
            } catch (CertificateExpiredException e) {
                logger.log(Level.WARNING, "Signing certificate expired " + signingCert.getNotAfter(), e);
                throw new ProcessorException(e);
            } catch (CertificateNotYetValidException e) {
                logger.log(Level.WARNING, "Signing certificate is not valid until " + signingCert.getNotBefore(), e);
                throw new ProcessorException(e);
            }
        }

        // Validate signature
        SignatureContext sigContext = new SignatureContext();
        sigContext.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws IOException {
                // TODO this works but SAXException doesn't... I guess XSS4J uses SAXException internally to signal some normal condition.
                throw new IOException("References to external resources are not permitted");
            }
        });
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                Element found = (Element)cntx.elementsByWsuId.get(s);
                if (found != null)
                    return found;

                // TODO we can return null here, and be faster, as long as no signed elements were hidden under an encrypted section
                return SoapUtil.getElementByWsuId(doc, s);
            }
        });
        sigContext.setAlgorithmFactory(new AlgorithmFactoryExtn() {
            public Transform getTransform(String s) throws NoSuchAlgorithmException {
                if (SoapUtil.TRANSFORM_STR.equals(s)) {
                    return new Transform() {
            public String getURI() {
                return SoapUtil.TRANSFORM_STR;
            }

            public void transform(TransformContext c) throws TransformException {
                Node source = c.getNode();
                if (source == null) throw new TransformException("Source node is null");
                final Node result = (Node)cntx.securityTokenReferenceElementToTargetElement.get(source);
                if (result == null) throw new TransformException("Unable to check signature of element signed indirectly through SecurityTokenReference transform: the referenced SecurityTokenReference has not yet been seen");
                ExclusiveC11r canon = new ExclusiveC11r();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                try {
                    canon.canonicalize(result, bo);
                } catch (IOException e) {
                    throw (TransformException)new TransformException().initCause(e);
                }
                c.setContent(bo.toByteArray(), "UTF-8");
            }
        };
                } else
                    return super.getTransform(s);
            }
        });
        Validity validity = sigContext.verify(sigElement, signingKey);

        if (!validity.getCoreValidity()) {
            // if the signature does not match but an encrypted key was previously ignored,
            // it is likely that this is caused by the fact that decryption did not occur.
            // this is perfectly legal in wss passthrough mechanisms, we therefore ignores this
            // signature altogether
            if (cntx.encryptionIgnored) {
                logger.info("the validity of a signature could not be computed however an " +
                  "encryption element was previously ignored for passthrough " +
                  "purposes. this signature will therefore be ignored.");
                return;
            }
            StringBuffer msg = new StringBuffer("Validity not achieved. " + validity.getSignedInfoMessage());
            for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                msg.append("\n\tElement " + validity.getReferenceURI(i) + ": " + validity.getReferenceMessage(i));
            }
            logger.warning(msg.toString());
            throw new ProcessorException(msg.toString());
        }

        // Remember which elements were covered
        final int numberOfReferences = validity.getNumberOfReferences();
        for (int i = 0; i < numberOfReferences; i++) {
            // Resolve each elements one by one.
            String elementCoveredURI = validity.getReferenceURI(i);
            Element elementCovered = (Element)cntx.elementsByWsuId.get(elementCoveredURI);
            if (elementCovered == null)
                elementCovered = SoapUtil.getElementByWsuId(sigElement.getOwnerDocument(), elementCoveredURI);
            if (elementCovered == null) {
                String msg = "Element covered by signature cannot be found in original document nor in " +
                  "processed document. URI: " + elementCoveredURI;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }
            // check whether this is a token reference
            Element targetElement = (Element)cntx.securityTokenReferenceElementToTargetElement.get(elementCovered);
            if (targetElement != null) {
               elementCovered = targetElement;
            }
            // make reference to this element
            final Element finalElementCovered = elementCovered;
            if (signingCertToken != null) {
                final SignedElement signedElement = new SignedElement() {
                    public SigningSecurityToken getSigningSecurityToken() {
                        return signingCertToken;
                    }

                    public Element asElement() {
                        return finalElementCovered;
                    }
                };
                cntx.elementsThatWereSigned.add(signedElement);
                signingCertToken.addSignedElement(signedElement);
                signingCertToken.onPossessionProved();
            } else if (dkt != null) {
                final SignedElement signedElement = new SignedElement() {
                    public SigningSecurityToken getSigningSecurityToken() {
                        return dkt.getSecurityContextToken();
                    }

                    public Element asElement() {
                        return finalElementCovered;
                    }
                };
                cntx.elementsThatWereSigned.add(signedElement);
                dkt.getSecurityContextToken().addSignedElement(signedElement);
                dkt.getSecurityContextToken().onPossessionProved();
            } else
                throw new RuntimeException("No signing security token found");

            // if this is a timestamp in the security header, note that it was signed
            if (SoapUtil.WSU_URIS.contains(elementCovered.getNamespaceURI()) &&
                  SoapUtil.TIMESTAMP_EL_NAME.equals(elementCovered.getLocalName()) &&
                  cntx.releventSecurityHeader == elementCovered.getParentNode()) {
                // Make sure we've seen this timestamp
                // TODO: would be very, very good to verify here that elementCovered == cntx.timestamp.asElement()
                //       Unfortunately they are in different documents :(
                //       It looks like we are safe, though: we only allow 1 timestamp (which we look for first
                //       in the Security header), and elementCovered is verified as being in the Security header
                if (cntx.timestamp == null)
                    throw new InvalidDocumentFormatException("Timestamp's Signature encountered before Timestamp element");

                // Update timestamp with signature information
                if (signingCertToken != null) {
                    cntx.timestamp.addSigningSecurityToken(signingCertToken);
                } else if (dkt != null) {
                    cntx.timestamp.addSigningSecurityToken(dkt.getSecurityContextToken());
                }
            }
        }
    }

    private ProcessorResult produceResult(final ProcessingStatusHolder cntx) {
        return new ProcessorResult() {
            public SignedElement[] getElementsThatWereSigned() {
                return (SignedElement[])cntx.elementsThatWereSigned.toArray(PROTOTYPE_SIGNEDELEMENT_ARRAY);
            }

            public ParsedElement[] getElementsThatWereEncrypted() {
                return (ParsedElement[])cntx.elementsThatWereEncrypted.toArray(PROTOTYPE_ELEMENT_ARRAY);
            }

            public SecurityToken[] getSecurityTokens() {
                return (SecurityToken[])cntx.securityTokens.toArray(PROTOTYPE_SECURITYTOKEN_ARRAY);
            }

            public WssTimestamp getTimestamp() {
                return cntx.timestamp;
            }

            public String getSecurityNS() {
                if (cntx.releventSecurityHeader != null) {
                    return cntx.releventSecurityHeader.getNamespaceURI();
                }
                return null;
            }

            public String getWSUNS() {
                // look for the wsu namespace somewhere
                if (cntx.timestamp != null && cntx.timestamp.asElement() != null) {
                    return cntx.timestamp.asElement().getNamespaceURI();
                } else if (cntx.securityTokens != null && !cntx.securityTokens.isEmpty()) {
                    for (Iterator i = cntx.securityTokens.iterator(); i.hasNext();) {
                        SecurityToken token = (SecurityToken)i.next();
                        NamedNodeMap attributes = token.asElement().getAttributes();
                        for (int ii = 0; ii < attributes.getLength(); ii++) {
                            Attr n = (Attr)attributes.item(ii);
                            if (n.getLocalName().equals("Id") &&
                                  n.getNamespaceURI() != null &&
                                  n.getNamespaceURI().length() > 0) {
                                return n.getNamespaceURI();
                            }
                        }

                    }
                }
                return null;
            }

            public SecurityActor getProcessedActor() {
                return cntx.secHeaderActor;
            }

            /**
             * @param element the element to find the signing tokens for
             * @return the array if tokens that signed the element or empty array if none
             */
            public SigningSecurityToken[] getSigningTokens(Element element) {
                if (element == null) {
                    throw new IllegalArgumentException();
                }

                Collection tokens = new ArrayList();
                if (cntx.processedDocument != element.getOwnerDocument()) {
                    throw new IllegalArgumentException("This element does not belong to the same document as processor result!");
                }

                Iterator it = cntx.securityTokens.iterator();
                while (it.hasNext()) {
                    Object o = it.next();
                    if (o instanceof SigningSecurityToken) {
                        SigningSecurityToken signingSecurityToken = (SigningSecurityToken)o;
                        final SignedElement[] signedElements = signingSecurityToken.getSignedElements();
                        for (int i = signedElements.length - 1; i >= 0; i--) {
                            SignedElement signedElement = signedElements[i];
                            if (element.equals(signedElement.asElement())) {
                                tokens.add(o);
                            }
                        }
                    }
                }
                return (SigningSecurityToken[])tokens.toArray(new SigningSecurityToken[]{});
            }
        };
    }

    private static final Logger logger = Logger.getLogger(WssProcessorImpl.class.getName());

    private class ProcessingStatusHolder {
        final Message message;
        final Document processedDocument;
        final Collection elementsThatWereSigned = new ArrayList();
        final Collection elementsThatWereEncrypted = new ArrayList();
        final Collection securityTokens = new ArrayList();
        final Collection derivedKeyTokens = new ArrayList();
        Map elementsByWsuId = null;
        TimestampImpl timestamp = null;
        Element releventSecurityHeader = null;
        Map x509TokensById = new HashMap();
        Map securityTokenReferenceElementToTargetElement = new HashMap();
        SecurityActor secHeaderActor;
        boolean documentModified = false;
        boolean encryptionIgnored = false;

        public ProcessingStatusHolder(Message message, Document processedDocument) {
            this.message = message;
            this.processedDocument = processedDocument;
        }

        /**
         * Call this before modifying processedDocument in any way.  This will upgrade the document to writable,
         * which will set various flags inside the Message
         * (for reserializing the document later, and possibly building a new TarariMessageContext), and will
         * possibly cause a copy of the current document to be cloned and saved.
         */
        void setDocumentModified() {
            if (documentModified)
                return;
            documentModified = true;
            try {
                message.getXmlKnob().getDocumentWritable();
            } catch (SAXException e) {
                throw new CausedIllegalStateException(e); // can't happen anymore
            } catch (IOException e) {
                throw new CausedIllegalStateException(e); // can't happen anymore
            }
        }
    }

    private static class X509BinarySecurityTokenImpl extends MutableX509SigningSecurityToken implements X509SecurityToken {
        private final X509Certificate finalcert;

        public X509BinarySecurityTokenImpl(X509Certificate finalcert, Element binarySecurityTokenElement) {
            super(binarySecurityTokenElement);
            this.finalcert = finalcert;
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.X509;
        }

        public String getElementId() {
            return SoapUtil.getElementWsuId(asElement());
        }

        public X509Certificate getMessageSigningCertificate() {
            return finalcert;
        }

        public X509Certificate asX509Certificate() {
            return finalcert;
        }

        public String toString() {
            return "X509SecurityToken: " + finalcert.toString();
        }
    }

    private static class TimestampImpl extends ParsedElementImpl implements WssTimestamp {
        private final TimestampDate createdTimestampDate;
        private final TimestampDate expiresTimestampDate;
        private List signingSecurityTokens = new ArrayList();

        public TimestampImpl(TimestampDate createdTimestampDate, TimestampDate expiresTimestampDate, Element timestampElement) {
            super(timestampElement);
            this.createdTimestampDate = createdTimestampDate;
            this.expiresTimestampDate = expiresTimestampDate;
        }

        public com.l7tech.common.security.xml.processor.WssTimestampDate getCreated() {
            return createdTimestampDate;
        }

        public com.l7tech.common.security.xml.processor.WssTimestampDate getExpires() {
            return expiresTimestampDate;
        }

        public boolean isSigned() {
            return !signingSecurityTokens.isEmpty();
        }

        public SecurityToken[] getSigningSecurityTokens() {
            return (SecurityToken[])signingSecurityTokens.toArray(new SecurityToken[0]);
        }

        private void addSigningSecurityToken(SecurityToken token) {
            signingSecurityTokens.add(token);
        }
    }

    private static final ParsedElement[] PROTOTYPE_ELEMENT_ARRAY = new ParsedElement[0];
    private static final SignedElement[] PROTOTYPE_SIGNEDELEMENT_ARRAY = new SignedElement[0];
    private static final SecurityToken[] PROTOTYPE_SECURITYTOKEN_ARRAY = new SecurityToken[0];

    private static class DerivedKeyTokenImpl extends ParsedElementImpl implements DerivedKeyToken {
        private final Key finalKey;
        private final SecurityContextTokenImpl sct;
        private final String elementWsuId;

        public DerivedKeyTokenImpl(Element dktel, Key finalKey, SecurityContextTokenImpl sct) {
            super(dktel);
            this.finalKey = finalKey;
            this.sct = sct;
            elementWsuId = SoapUtil.getElementWsuId(dktel);
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.WSSC_DERIVED_KEY;
        }

        public String getElementId() {
            return elementWsuId;
        }

        Key getComputedDerivedKey() {
            return finalKey;
        }

        SecurityContextTokenImpl getSecurityContextToken() {
            return sct;
        }

        public String toString() {
            return "DerivedKeyToken: " + finalKey.toString();
        }
    }

    private static class SecurityContextTokenImpl extends MutableSigningSecurityToken implements SecurityContextToken {
        private final SecurityContext secContext;
        private final String identifier;
        private final String elementWsuId;

        public SecurityContextTokenImpl(SecurityContext secContext, Element secConTokEl, String identifier) {
            super(secConTokEl);
            this.secContext = secContext;
            this.identifier = identifier;
            this.elementWsuId = SoapUtil.getElementWsuId(secConTokEl);
        }

        public SecurityContext getSecurityContext() {
            return secContext;
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.WSSC_CONTEXT;
        }

        public String getElementId() {
            return elementWsuId;
        }

        public String getContextIdentifier() {
            return identifier;
        }

        public String toString() {
            return "SecurityContextToken: " + secContext.toString();
        }
    }

}
