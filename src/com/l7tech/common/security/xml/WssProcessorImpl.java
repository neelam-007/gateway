package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.EncryptedData;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
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

    private static class ProcessorException extends com.l7tech.common.security.xml.ProcessorException {
        public ProcessorException(Throwable cause) {
            super();
            initCause(cause);
        }

        public ProcessorException(String message) {
            super();
            initCause(new IllegalArgumentException(message));
        }
    }

    private static class ParsedElementImpl implements ParsedElement {
        private final Element element;

        public ParsedElementImpl(Element element) {
            this.element = element;
        }

        public Element asElement() {
            return element;
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
     * @throws BadContextException if the message contains a WS-SecureConversation SecurityContextToken, but the securityContextFinder has no record of that session.
     */
    public WssProcessor.ProcessorResult undecorateMessage(Document soapMsg,
                                                          X509Certificate recipientCert,
                                                          PrivateKey recipientKey,
                                                          SecurityContextFinder securityContextFinder)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, BadContextException
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
        cntx.originalDocumentSecurityHeader = SoapUtil.getSecurityElement(cntx.originalDocument);
        cntx.releventSecurityHeader = SoapUtil.getSecurityElement(cntx.processedDocument);

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
                    processEncryptedKey(securityChildToProcess, recipientKey,
                                        recipientCert, cntx);
                    // if this element is processed BEFORE the signature validation, it should be removed
                    // for the signature to validate properly
                    removeProcessedElement = true;
                } else {
                    logger.info("Encountered EncryptedKey element but not of right namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.TIMESTAMP_EL_NAME)) {
                if (elementHasNamespace(securityChildToProcess, SoapUtil.WSU_URIS_ARRAY)) {
                    processTimestamp(cntx, securityChildToProcess);
                } else {
                    logger.info("Encountered Timestamp element but not of right namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.BINARYSECURITYTOKEN_EL_NAME)) {
                if (elementHasNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processBinarySecurityToken(securityChildToProcess, cntx);
                } else {
                    logger.info("Encountered BinarySecurityToken element but not of right namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SIGNATURE_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.DIGSIG_URI)) {
                    processSignature(securityChildToProcess, cntx);
                } else {
                    logger.info("Encountered Signature element but not of right namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.USERNAME_TOK_EL_NAME)) {
                if (elementHasNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processUsernameToken(securityChildToProcess, cntx);
                } else {
                    logger.info("Encountered UsernameToken element but not of expected namespace (" +
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
                            throw new BadContextException(identifier);
                        }
                        SecurityContextTokenImpl secConTok = new SecurityContextTokenImpl(secContext,
                                                                                          secConTokEl,
                                                                                          identifier);
                        cntx.securityTokens.add(secConTok);
                    }
                } else {
                    logger.info("Encountered SecurityContextToken element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.WSSC_DK_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.WSSC_NAMESPACE)) {
                    processDerivedKey(securityChildToProcess, cntx);
                } else {
                    logger.info("Encountered DerivedKey element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.REFLIST_EL_NAME)) {
                // In the case of a Secure Conversation the reference list is declared outside
                // of the DerivedKeyToken
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.XMLENC_NS)) {
                    processReferenceList(securityChildToProcess, cntx);
                } else {
                    logger.info("Encountered ReferenceList element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
            }  else if (securityChildToProcess.getLocalName().equals(SamlConstants.ELEMENT_ASSERTION)) {
                if ( securityChildToProcess.getNamespaceURI().equals(SamlConstants.NS_SAML) ) {
                    processSamlSecurityToken(securityChildToProcess, cntx);
                } else {
                    logger.info("Encountered SAML Assertion element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ")");
                }
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
            if (removeProcessedElement) {
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

        // remove Security element altogether
        Element soapHeader = (Element) cntx.releventSecurityHeader.getParentNode();
        soapHeader.removeChild(cntx.releventSecurityHeader);

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

        // If our work has left behind an empty SOAP Header, remove it too
        if (XmlUtil.elementIsEmpty(soapHeader))
            soapHeader.getParentNode().removeChild(soapHeader);

        return produceResult(cntx);
    }

    private void processReferenceList(Element referenceListEl, ProcessingStatusHolder cntx) throws ProcessorException, InvalidDocumentFormatException {
        // get each element one by one
        List dataRefEls = XmlUtil.findChildElementsByName(referenceListEl, SoapUtil.XMLENC_NS, SoapUtil.DATAREF_EL_NAME);
        if ( dataRefEls == null || dataRefEls.isEmpty() ) {
            logger.warning("ReferenceList is present, but is empty");
            return;
        }

        for (Iterator j = dataRefEls.iterator(); j.hasNext();) {
            Element dataRefEl = (Element)j.next();
            String dataRefUri = dataRefEl.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);
            Element encryptedDataElement = SoapUtil.getElementByWsuId(referenceListEl.getOwnerDocument(), dataRefUri);
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
                throw new InvalidDocumentFormatException("The DataReference's KeyInfo did not refer to a DerivedKey");
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
        final LoginCredentials creds = new LoginCredentials(username, passwd.toCharArray(), null);
        WssProcessor.SecurityToken rememberedSecToken = new UsernameToken() {
            public Element asElement() {
                return usernameTokenElement;
            }

            public String getUsername() {
                return creds.getLogin();
            }

            public LoginCredentials asLoginCredentials() {
                return creds;
            }

            public String getElementId() {
                return SoapUtil.getElementWsuId(usernameTokenElement);
            }

            public String toString() {
                return "UsernameToken: " + creds.getLogin();
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
        XencUtil.checkKeyInfo(encryptedKeyElement, recipientCert);

        // verify that the algo is supported
        XencUtil.checkEncryptionMethod(encryptedKeyElement);

        // Extract the encrypted key
        byte[] unencryptedKey = XencUtil.decryptKey(encryptedKeyElement, recipientKey);

        // We got the key. Get the list of elements to decrypt.
        Element refList = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                SoapUtil.XMLENC_NS,
                                                                SoapUtil.REFLIST_EL_NAME);
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
                   ProcessorException, InvalidDocumentFormatException
    {
        List dataRefEls = XmlUtil.findChildElementsByName(refList, SoapUtil.XMLENC_NS, SoapUtil.DATAREF_EL_NAME);
        if ( dataRefEls == null || dataRefEls.isEmpty() ) {
            logger.warning("EncryptedData is present, but contains at least one empty ReferenceList");
            return;
        }

        for (Iterator j = dataRefEls.iterator(); j.hasNext();) {
            Element dataRefEl = (Element)j.next();
            String dataRefUri = dataRefEl.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);
            Element encryptedDataElement = SoapUtil.getElementByWsuId(refList.getOwnerDocument(), dataRefUri);
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
        Element parentElement = (Element) encryptedDataElement.getParentNode();

        // See if the parent element contains nothing else except attributes and this EncryptedData element
        // (and possibly a whitespace node before and after it)
        // TODO trim() throws out all CTRL characters along with whitespace.  Need to think about this.
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
            logger.info("All of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            cntx.elementsThatWereEncrypted.add(new ParsedElementImpl(parentElement));
        } else {
            // There was unencrypted stuff mixed in with the EncryptedData, so we can only record elements as
            // encrypted that were actually wholly inside the EncryptedData.
            // TODO: In this situation, no note is taken of any encrypted non-Element nodes (such as text nodes)
            // This sucks, but at lesat this will err on the side of safety.
            logger.info("Only some of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            for (int i = 0; i < dataList.getLength(); i++) {
                Node node = dataList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    cntx.elementsThatWereEncrypted.add(new ParsedElementImpl((Element)node));
                }
            }
        }
    }

    private static class TimestampDate extends ParsedElementImpl implements WssProcessor.TimestampDate {
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
        final X509Certificate finalcert = referencedCert;
        WssProcessor.SecurityToken rememberedSecToken = new X509SecurityTokenImpl(finalcert,
                                                                                  binarySecurityTokenElement);
        cntx.securityTokens.add(rememberedSecToken);
        cntx.x509TokensById.put(wsuId, rememberedSecToken);
    }

    private void processSamlSecurityToken( Element securityTokenElement, ProcessingStatusHolder context )
            throws InvalidDocumentFormatException
    {
        logger.finest("Processing saml:Assertion XML SecurityToken");
        SamlSecurityToken samlToken = new SamlSecurityTokenImpl(securityTokenElement);
        context.securityTokens.add(samlToken);
        context.x509TokensById.put(samlToken.getElementId(), samlToken);

        // TODO verify sender-vouches + proof-of-possession    (?? please explain?  -lyonsm)
    }

    private SigningSecurityTokenImpl resolveCertByRef(final Element parentElement, ProcessingStatusHolder cntx) {
        // TODO SAML
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
                final SigningSecurityTokenImpl token = (SigningSecurityTokenImpl)cntx.x509TokensById.get(uriAttr);
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

    private void processSignature(final Element sigElement, ProcessingStatusHolder cntx)
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
        final SigningSecurityTokenImpl signingCertToken = resolveCertByRef(keyInfoElement, cntx);

        if (signingCertToken != null) {
            signingCert = signingCertToken.getCertificate();
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
            String msg = "Was not able to get cert or derived key from signature's keyinfo";
            logger.warning(msg);
            throw new InvalidDocumentFormatException(msg);
        }

        if (signingCert != null) {
            try {
                CertUtils.checkValidity(signingCert);
            } catch ( CertificateExpiredException e ) {
                logger.log( Level.WARNING, "Signing certificate expired " + signingCert.getNotAfter(), e );
                throw new ProcessorException(e);
            } catch ( CertificateNotYetValidException e ) {
                logger.log( Level.WARNING, "Signing certificate is not valid until " + signingCert.getNotBefore(), e );
                throw new ProcessorException(e);
            }
        }

        // Validate signature
        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
                                   public Element resolveID(Document doc, String s) {
                                       return SoapUtil.getElementByWsuId(doc, s);
                                   }
                               });
        Validity validity = sigContext.verify(sigElement, signingKey);

        if (!validity.getCoreValidity()) {
            StringBuffer msg = new StringBuffer("Validity not achieved. " + validity.getSignedInfoMessage());
            for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                msg.append("\n\tElement " + validity.getReferenceURI(i) + ": " + validity.getReferenceMessage(i));
            }
            logger.warning(msg.toString());
            throw new ProcessorException(msg.toString());
        }

        // This certificate successfully validated a signature.  Consider proof-of-possession of private key
        // to have been successful.
        if (signingCertToken != null) {
            signingCertToken.onPossessionProved();
        }
        if (dkt != null)
            dkt.getSecurityContextToken().onPossessionProved();            

        // Remember which elements were covered
        for (int i = 0; i < validity.getNumberOfReferences(); i++) {
            // Resolve each elements one by one.
            String elementCoveredURI = validity.getReferenceURI(i);
            Element elementCovered = SoapUtil.getElementByWsuId(sigElement.getOwnerDocument(), elementCoveredURI);
            //elementCovered = SoapUtil.getElementByWsuId(cntx.originalDocument, elementCoveredURI);
            if (elementCovered == null) {
                String msg = "Element covered by signature cannot be found in original document nor in " +
                             "processed document. URI: " + elementCoveredURI;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }

            // make reference to this element
            final Element finalElementCovered = elementCovered;
            if (signingCertToken != null) {
                cntx.elementsThatWereSigned.add(new SignedElement() {
                    public SecurityToken getSigningSecurityToken() {
                        return signingCertToken;
                    }
                    public Element asElement() {
                        return finalElementCovered;
                    }
                });
            } else if (dkt != null) {
                cntx.elementsThatWereSigned.add(new SignedElement() {
                    public SecurityToken getSigningSecurityToken() {
                        return dkt.getSecurityContextToken();
                    }
                    public Element asElement() {
                        return finalElementCovered;
                    }
                });
            }

            // if this is a timestamp in the security header, note that it was signed
            if (SoapUtil.WSU_URIS.contains(elementCovered.getNamespaceURI()) &&
                SoapUtil.TIMESTAMP_EL_NAME.equals(elementCovered.getLocalName()) &&
                cntx.releventSecurityHeader == elementCovered.getParentNode())
            {
                // Make sure we've seen this timestamp
                // TODO: would be very, very good to verify here that elementCovered == cntx.timestamp.asElement()
                //       Unfortunately they are in different documents :(
                //       It looks like we are safe, though: we only allow 1 timestamp (which we look for first
                //       in the Security header), and elementCovered is verified as being in the Security header
                if (cntx.timestamp == null)
                    throw new InvalidDocumentFormatException("Timestamp's Signature encountered before Timestamp element");

                // Update timestamp with signature information
                if (signingCertToken != null) {
                    cntx.timestamp.setSigningSecurityToken(signingCertToken);
                } else if (dkt != null) {
                    cntx.timestamp.setSigningSecurityToken(dkt.getSecurityContextToken());
                }
            }
        }
    }

    private WssProcessor.ProcessorResult produceResult(final ProcessingStatusHolder cntx) {
        return new WssProcessor.ProcessorResult() {
            public Document getUndecoratedMessage() {
                return cntx.processedDocument;
            }

            public SignedElement[] getElementsThatWereSigned() {
                return (WssProcessor.SignedElement[]) cntx.elementsThatWereSigned.toArray(PROTOTYPE_SIGNEDELEMENT_ARRAY);
            }

            public ParsedElement[] getElementsThatWereEncrypted() {
                return (ParsedElement[])cntx.elementsThatWereEncrypted.toArray(PROTOTYPE_ELEMENT_ARRAY);
            }

            public WssProcessor.SecurityToken[] getSecurityTokens() {
                return (WssProcessor.SecurityToken[])cntx.securityTokens.toArray(PROTOTYPE_SECURITYTOKEN_ARRAY);
            }

            public WssProcessor.Timestamp getTimestamp() {
                return cntx.timestamp;
            }

            public String getSecurityNS() {
                if (cntx.releventSecurityHeader != null) {
                    return cntx.releventSecurityHeader.getNamespaceURI();
                }
                return null;
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
        final Collection derivedKeyTokens = new ArrayList();
        TimestampImpl timestamp = null;
        Element releventSecurityHeader = null;
        Map x509TokensById = new HashMap();
        Element originalDocumentSecurityHeader = null;
    }

    private static class X509SecurityTokenImpl extends SigningSecurityTokenImpl implements X509SecurityToken {
        private final X509Certificate finalcert;
        private final Element binarySecurityTokenElement;

        public X509SecurityTokenImpl(X509Certificate finalcert, Element binarySecurityTokenElement) {
            super(binarySecurityTokenElement);
            this.finalcert = finalcert;
            this.binarySecurityTokenElement = binarySecurityTokenElement;
        }

        public String getElementId() {
            return SoapUtil.getElementWsuId(binarySecurityTokenElement);
        }

        protected X509Certificate getCertificate() {
            return finalcert;
        }

        public Element asElement() {
            return binarySecurityTokenElement;
        }

        public X509Certificate asX509Certificate() {
            return finalcert;
        }

        public String toString() {
            return "X509SecurityToken: " + finalcert.toString();
        }

    }

    private static abstract class SigningSecurityTokenImpl extends ParsedElementImpl implements SecurityToken {
        protected abstract X509Certificate getCertificate();

        public boolean isPossessionProved() {
            return possessionProved;
        }

        protected void onPossessionProved() {
            possessionProved = true;
        }

        private boolean possessionProved = false;

        protected SigningSecurityTokenImpl(Element element) {
            super(element);
        }
    }

    private static class SamlSecurityTokenImpl extends SigningSecurityTokenImpl implements SamlSecurityToken {
        SamlHolderOfKeyAssertion assertion;

        public SamlSecurityTokenImpl(Element element) throws InvalidDocumentFormatException {
            super(element);
            try {
                this.assertion = new SamlHolderOfKeyAssertion(element);
            } catch (SAXException e) {
                throw new InvalidDocumentFormatException(e);
            }
        }

        /**
         * May be null if the assertion has no SubjectConfirmation with a KeyInfo block inside.
         * @return the X509Certificate from the SAML Assertion's //SubjectConfirmation/KeyInfo. May be null.
         */
        public X509Certificate getCertificate() {
            return assertion.getSubjectCertificate();
        }

        public SamlAssertion asSamlAssertion() {
            return assertion;
        }

        public X509Certificate getSubjectCertificate() {
            return assertion.getSubjectCertificate();
        }

        public String getElementId() {
            return assertion.getAssertionId();
        }

        public String toString() {
            return "SamlSecurityToken: " + assertion.toString();
        }
    }


    private static class TimestampImpl extends ParsedElementImpl implements WssProcessor.Timestamp {
        private final TimestampDate createdTimestampDate;
        private final TimestampDate expiresTimestampDate;
        private SecurityToken signingToken = null;

        public TimestampImpl(TimestampDate createdTimestampDate, TimestampDate expiresTimestampDate, Element timestampElement) {
            super(timestampElement);
            this.createdTimestampDate = createdTimestampDate;
            this.expiresTimestampDate = expiresTimestampDate;
        }

        public WssProcessor.TimestampDate getCreated() {
            return createdTimestampDate;
        }

        public WssProcessor.TimestampDate getExpires() {
            return expiresTimestampDate;
        }

        public boolean isSigned() {
            return signingToken != null;
        }

        public WssProcessor.SecurityToken getSigningSecurityToken() {
            return signingToken;
        }

        private void setSigningSecurityToken(SecurityToken token) {
            this.signingToken = token;
        }
    }

    private static final ParsedElement[] PROTOTYPE_ELEMENT_ARRAY = new ParsedElement[0];
    private static final SignedElement[] PROTOTYPE_SIGNEDELEMENT_ARRAY = new SignedElement[0];
    private static final WssProcessor.SecurityToken[] PROTOTYPE_SECURITYTOKEN_ARRAY = new WssProcessor.SecurityToken[0];

    private static class DerivedKeyTokenImpl extends ParsedElementImpl implements SecurityToken {
        private final Key finalKey;
        private final SecurityContextTokenImpl sct;
        private final String elementWsuId;

        public DerivedKeyTokenImpl(Element dktel, Key finalKey, SecurityContextTokenImpl sct) {
            super(dktel);
            this.finalKey = finalKey;
            this.sct = sct;
            elementWsuId = SoapUtil.getElementWsuId(dktel);
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

    private static class SecurityContextTokenImpl extends ParsedElementImpl implements WssProcessor.SecurityContextToken {
        private final WssProcessor.SecurityContext secContext;
        private final String identifier;
        private final String elementWsuId;

        private boolean possessionProved = false;

        public SecurityContextTokenImpl(WssProcessor.SecurityContext secContext, Element secConTokEl, String identifier) {
            super(secConTokEl);
            this.secContext = secContext;
            this.identifier = identifier;
            this.elementWsuId = SoapUtil.getElementWsuId(secConTokEl);
        }

        public WssProcessor.SecurityContext getSecurityContext() {
            return secContext;
        }

        public String getElementId() {
            return elementWsuId;
        }

        public String getContextIdentifier() {
            return identifier;
        }

        public boolean isPossessionProved() {
            return possessionProved;
        }

        void onPossessionProved() {
            possessionProved = true;
        }

        public String toString() {
            return "SecurityContextToken: " + secContext.toString();
        }
    }
}
