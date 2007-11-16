package com.l7tech.common.security.xml.processor;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.*;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.FlexKey;
import com.l7tech.common.security.kerberos.KerberosConfigException;
import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.kerberos.KerberosUtils;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.UnsupportedDocumentFormatException;
import com.l7tech.common.xml.InvalidDocumentSignatureException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.PartInfo;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
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
 * @noinspection unchecked,ForLoopReplaceableByForEach,WhileLoopReplaceableByForEach
 */
public class WssProcessorImpl implements WssProcessor {
    static {
        JceProvider.init();
    }

    public ProcessorResult undecorateMessage(Message message,
                                             X509Certificate senderCertificate,
                                             SecurityContextFinder securityContextFinder,
                                             SecurityTokenResolver securityTokenResolver
    )
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, BadSecurityContextException, SAXException, IOException {
        // Reset all potential outputs
        Document soapMsg = message.getXmlKnob().getDocumentReadOnly();
        ProcessingStatusHolder cntx = new ProcessingStatusHolder(message, soapMsg);
        cntx.elementsThatWereSigned.clear();
        cntx.elementsThatWereEncrypted.clear();
        cntx.securityTokens.clear();
        cntx.timestamp = null;
        cntx.releventSecurityHeader = null;
        cntx.elementsByWsuId = SoapUtil.getElementByWsuIdMap(soapMsg);
        cntx.senderCertificate = senderCertificate;
        cntx.securityTokenResolver = securityTokenResolver;

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
            Element removeRefList = null;

            if (securityChildToProcess.getLocalName().equals(SoapUtil.ENCRYPTEDKEY_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.XMLENC_NS)) {
                    // http://www.w3.org/2001/04/xmlenc#
                    // if this element is processed BEFORE the signature validation, it should be removed
                    // for the signature to validate properly
                    // fla added (but only if the ec was actually processed)
                    // lyonsm: we now only remove the reference list, and only if we decrypted it.
                    //         The signature check will take care of removing any processed encrypted keys as well,
                    //         but only if needed to validate an enveloped signature.
                    removeRefList = processEncryptedKey(securityChildToProcess, cntx);
                } else {
                    logger.finer("Encountered EncryptedKey element but not of right namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.TIMESTAMP_EL_NAME)) {
                if (XmlUtil.elementInNamespace(securityChildToProcess, SoapUtil.WSU_URIS_ARRAY)) {
                    processTimestamp(cntx, securityChildToProcess);
                } else {
                    logger.fine("Encountered Timestamp element but not of right namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.BINARYSECURITYTOKEN_EL_NAME)) {
                if (XmlUtil.elementInNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processBinarySecurityToken(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered BinarySecurityToken element but not of right namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SIGNATURE_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals(SoapUtil.DIGSIG_URI)) {
                    processSignature(securityChildToProcess, securityContextFinder, cntx);
                } else {
                    logger.fine("Encountered Signature element but not of right namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.USERNAME_TOK_EL_NAME)) {
                if (XmlUtil.elementInNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processUsernameToken(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered UsernameToken element but not of expected namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SECURITY_CONTEXT_TOK_EL_NAME)) {
                if (XmlUtil.elementInNamespace(securityChildToProcess, SoapUtil.WSSC_NAMESPACE_ARRAY)) {
                    String identifier = extractIdentifierFromSecConTokElement(securityChildToProcess);
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
                                                                                          securityChildToProcess,
                                                                                          identifier);
                        cntx.securityTokens.add(secConTok);
                        logger.finest("SecurityContextToken (SecureConversation) added");
                    }
                } else {
                    logger.fine("Encountered SecurityContextToken element but not of expected namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.WSSC_DK_EL_NAME)) {
                if (XmlUtil.elementInNamespace(securityChildToProcess,SoapUtil.WSSC_NAMESPACE_ARRAY)) {
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
                if (securityChildToProcess.getNamespaceURI().equals(SamlConstants.NS_SAML) ||
                    securityChildToProcess.getNamespaceURI().equals(SamlConstants.NS_SAML2)) {
                    processSamlSecurityToken(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered SAML Assertion element but not of expected namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals(SoapUtil.SECURITYTOKENREFERENCE_EL_NAME)) {
                if (XmlUtil.elementInNamespace(securityChildToProcess, SoapUtil.SECURITY_URIS_ARRAY)) {
                    processSecurityTokenReference(securityChildToProcess, securityContextFinder, cntx);
                } else {
                    logger.fine("Encountered SecurityTokenReference element but not of expected namespace (" +
                            securityChildToProcess.getNamespaceURI() + ")");
                }
            } else if (securityChildToProcess.getLocalName().equals("SignatureConfirmation")) {
                if (XmlUtil.elementInNamespace(securityChildToProcess, new String[] { SoapUtil.SECURITY11_NAMESPACE } )) {
                    processSignatureConfirmation(securityChildToProcess, cntx);
                } else {
                    logger.fine("Encountered SignatureConfirmation element but not of expected namespace (" +
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
            if (removeRefList != null) {
                cntx.setDocumentModified();
                removeRefList.getParentNode().removeChild(removeRefList);
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
     * Set a limit on the maximum signed attachment size.
     *
     * @param size
     */
    public void setSignedAttachmentSizeLimit(final long size) {
        signedAttachmentSizeLimit = size;
    }

    private void processSignatureConfirmation(Element securityChildToProcess, ProcessingStatusHolder cntx) {
        cntx.isWsse11Seen = true;
        String value = securityChildToProcess.getAttribute("Value");
        if (value == null || value.length() < 1) {
            logger.fine("Ignoring empty SignatureConfirmation header");
            return;
        }
        cntx.lastSignatureConfirmation = value;
    }

    /**
     * Process a free-floating SecurityTokenReference. different mechanisms for referencing security tokens using the
     * <wsse:SecurityTokenReference> exist, and currently the supported are:
     * <ul>
     * <li> Key Identifier <wsse:KeyIdentifier></li>
     * <li> Security Token Reference <wsse:Reference></li>
     * </ul>
     * The unsupported SecurityTokeReference types are:
     * <ul>
     * <li> X509 issuer name and issuer serial <ds:X509IssuerName>,  <ds:X509SerialNumber></li>
     * <li> Embedded token <wsse:Embedded></li>
     * <li> Key Name <ds:KeyName></li>
     * </ul>
     * This is as per <i>Web Services Security: SOAP Message Security 1.0 (WS-Security 2004) OASIS standard</i>
     * TODO this should be merged into KeyInfoElement
     *
     * @param str  the SecurityTokenReference element
     * @param securityContextFinder the context finder to perform lookups with (may be null)
     * @param cntx the processing status holder/accumulator
     * @throws InvalidDocumentFormatException if STR is invalid format or points at something unsupported
     * @throws ProcessorException if a securityContextFinder is required to resolve this STR, but one was not provided
     */
    private void processSecurityTokenReference(Element str,
                                               SecurityContextFinder securityContextFinder,
                                               ProcessingStatusHolder cntx)
            throws InvalidDocumentFormatException, ProcessorException
    {
        // Get identifier
        String id = SoapUtil.getElementWsuId(str);
        boolean noId = false;
        if (id == null || id.length() < 1) {
            noId = true;
            id = "<noid>";
        }

        // Reference or KeyIdentifier values
        boolean isKeyIdentifier = false;
        boolean isReference = false;
        String value = null;
        String valueType = null;
        String encodingType = null;

        // Process KeyIdentifier
        Element keyIdentifierElement = XmlUtil.findFirstChildElementByName(str, str.getNamespaceURI(), "KeyIdentifier");
        if (keyIdentifierElement != null) {
            isKeyIdentifier = true;
            value = XmlUtil.getTextValue(keyIdentifierElement).trim();
            valueType = keyIdentifierElement.getAttribute("ValueType");
            encodingType = keyIdentifierElement.getAttribute("EncodingType");
        } else {
            // Process Reference
            Element referenceElement = XmlUtil.findFirstChildElementByName(str, str.getNamespaceURI(), "Reference");
            if (referenceElement != null) {
                isReference = true;
                value = referenceElement.getAttribute("URI");
                if (value != null && value.length()==0) {
                    value = null; // we want null not empty string for missing URI
                }
                if (value != null && value.charAt(0) == '#') {
                    value = value.substring(1);
                }
                valueType = referenceElement.getAttribute("ValueType");
            }
        }

        if(!(isReference || isKeyIdentifier)) {
            logger.warning("Ignoring SecurityTokenReference ID=" + id + " with no KeyIdentifier or Reference");
            return;
        }

        if (value == null) {
            String msg = "Rejecting SecurityTokenReference ID=" + id
                    + " as the target Reference ID/KeyIdentifier is missing or could not be determined.";
            logger.warning(msg);
            throw new InvalidDocumentFormatException(msg);
        }

        // Process KeyIdentifier or Reference
        if (SoapUtil.isValueTypeX509v3(valueType) && !isKeyIdentifier) {
            if(noId) {
                logger.warning("Ignoring SecurityTokenReference with no wsu:Id");
                return;
            }
            if (encodingType != null && encodingType.length() > 0) {
                logger.warning("Ignoring SecurityTokenReference ID='" + id
                        + "' with non-empty KeyIdentifier/@EncodingType='" + encodingType + "'.");
                return;
            }
            Element target = (Element)cntx.elementsByWsuId.get(value);
            if (target == null
                    || !target.getLocalName().equals("BinarySecurityToken")
                    || !ArrayUtils.contains(SoapUtil.SECURITY_URIS_ARRAY, target.getNamespaceURI())
                    || !SoapUtil.isValueTypeX509v3(target.getAttribute("ValueType"))) {
                String msg = "Rejecting SecurityTokenReference ID='" + id + "' with ValueType of '" + valueType +
                        "' because its target is either missing or not a BinarySecurityToken";
                logger.warning(msg);
                throw new InvalidDocumentFormatException(msg);
            }
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Remembering SecurityTokenReference ID=" + id + " pointing at X.509 BST " + value);
            cntx.securityTokenReferenceElementToTargetElement.put(str, target);
        } else if (SoapUtil.isValueTypeSaml(valueType)) {
            if(noId) {
                logger.warning("Ignoring SecurityTokenReference with no wsu:Id");
                return;
            }
            if (encodingType != null && encodingType.length() > 0) {
                logger.warning("Ignoring SecurityTokenReference ID='" + id
                        + "' with non-empty KeyIdentifier/@EncodingType='" + encodingType + "'.");
                return;
            }
            Element target = (Element)cntx.elementsByWsuId.get(value);
            if (target == null
                    || !target.getLocalName().equals("Assertion")
                    || (!target.getNamespaceURI().equals(SamlConstants.NS_SAML) &&
                        !target.getNamespaceURI().equals(SamlConstants.NS_SAML2))) {
                String msg = "Rejecting SecurityTokenReference ID='" + id + "' with ValueType of '" + valueType +
                        "' because its target is either missing or not a SAML assertion";
                logger.warning(msg); // TODO remove redundant logging after debugging complete
                throw new InvalidDocumentFormatException(msg);
            }
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Remembering SecurityTokenReference ID=" + id + " pointing at SAML assertion " + value);
            cntx.securityTokenReferenceElementToTargetElement.put(str, target);
        } else if (SoapUtil.isValueTypeKerberos(valueType) && isKeyIdentifier) {
            if (encodingType == null || !encodingType.equals(SoapUtil.ENCODINGTYPE_BASE64BINARY)) {
                logger.warning("Ignoring SecurityTokenReference ID=" + id +
                        " with missing or invalid KeyIdentifier/@EncodingType=" + encodingType);
                return;
            }

            if (securityContextFinder == null)
                throw new ProcessorException("Kerberos KeyIdentifier element found in message, but caller did not " +
                        "provide a SecurityContextFinder");

            String identifier = KerberosUtils.getSessionIdentifier(value);
            SecurityContext secContext = securityContextFinder.getSecurityContext(identifier);
            if (secContext != null) {
                SecurityContextTokenImpl secConTok = new SecurityContextTokenImpl(secContext,
                                                                                  keyIdentifierElement,
                                                                                  identifier);
                cntx.securityTokens.add(secConTok);
            }
            else {
                logger.warning("Could not find referenced Kerberos security token '"+value+"'.");
            }
        } else {
            logger.warning("Ignoring SecurityTokenReference ID=" + id + " with ValueType of " + valueType);
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
            if (dataRefUri.startsWith("#")) dataRefUri = dataRefUri.substring(1);
            Element encryptedDataElement = (Element)cntx.elementsByWsuId.get(dataRefUri);
            if (encryptedDataElement == null)
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
            SecretKeyToken dktok = resolveDerivedKeyByRef(keyInfo, cntx);
            try {
                if (dktok == null) {
                    SigningSecurityToken tok = resolveSigningTokenByRef(keyInfo, cntx);
                    if (tok instanceof EncryptedKey) {
                        dktok = (EncryptedKey)tok;
                    } else {
                        // there are some keyinfo formats that we do not support. in that case, we should see if
                        // the message can possibly just passthrough
                        logger.info("The DataReference's KeyInfo did not refer to a DerivedKey or previously-known EncryptedKey." +
                                "This element will not be decrypted.");
                        cntx.encryptionIgnored = true;
                        return;
                    }
                }
                decryptElement(encryptedDataElement, dktok.getSecretKey(), cntx);
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

    private void processDerivedKey(Element derivedKeyEl, ProcessingStatusHolder cntx)
            throws InvalidDocumentFormatException, ProcessorException, GeneralSecurityException {
        // get corresponding shared secret reference wsse:SecurityTokenReference
        Element sTokrefEl = XmlUtil.findFirstChildElementByName(derivedKeyEl,
                                                                SoapUtil.SECURITY_URIS_ARRAY,
                                                                SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        if (sTokrefEl == null) throw new InvalidDocumentFormatException("DerivedKeyToken should " +
                "contain a SecurityTokenReference");
        Element refEl = XmlUtil.findFirstChildElementByName(sTokrefEl,
                                                            SoapUtil.SECURITY_URIS_ARRAY,
                                                            SoapUtil.REFERENCE_EL_NAME);

        final XmlSecurityToken derivationSource;
        final String ref;
        if (refEl == null) {
            // Check for an EncryptedKeySHA1 reference
            Element keyIdEl = XmlUtil.findFirstChildElementByName(sTokrefEl,
                                                                  SoapUtil.SECURITY_URIS_ARRAY,
                                                                  "KeyIdentifier");
            ref = XmlUtil.getTextValue(keyIdEl);

            String valueType = keyIdEl.getAttribute("ValueType");
            if (valueType == null)
                throw new InvalidDocumentFormatException("DerivedKey SecurityTokenReference KeyIdentifier has no ValueType");

            if (SoapUtil.VALUETYPE_ENCRYPTED_KEY_SHA1.equals(valueType)) {
                derivationSource = resolveEncryptedKeyBySha1(cntx, ref);

            } else if (SoapUtil.VALUETYPE_KERBEROS_APREQ_SHA1.equals(valueType)) {
                if (cntx.securityTokenResolver == null)
                    throw new ProcessorException("Unable to process DerivedKeyToken - it references a Kerberosv5APREQSHA1, but no security token resolver is available");
                XmlSecurityToken xst = cntx.securityTokenResolver.getKerberosTokenBySha1(ref);

                if(xst==null) {
                    xst = findSecurityContextTokenBySessionId(cntx, KerberosUtils.getSessionIdentifier(ref));
                }

                derivationSource = xst;
            } else
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to unsupported ValueType " + valueType);

        } else {
            ref = refEl.getAttribute("URI");
            if (ref == null || ref.length() < 1)
                throw new InvalidDocumentFormatException("DerivedKeyToken's SecurityTokenReference lacks URI parameter");
            if (ref.startsWith("#"))
                derivationSource = findXmlSecurityTokenById(cntx, ref);
            else
                derivationSource = findSecurityContextTokenBySessionId(cntx, ref);
        }

        if(derivationSource==null) {
            logger.info("Invalid DerivedKeyToken reference target '" + ref + "', ignoring this derived key.");
            return;
        }

        if (derivationSource instanceof SecurityContextTokenImpl) {
            cntx.derivedKeyTokens.add(deriveKeyFromSecurityContext(derivedKeyEl,
                                                                   (SecurityContextTokenImpl)derivationSource));
            // We won't count this as having seen a derived key, since WS-SC has always used them, and older SSBs
            // won't understand them if we try to use them for a non-WS-SC response
        } else if (derivationSource instanceof EncryptedKey) {
            cntx.derivedKeyTokens.add(deriveKeyFromEncryptedKey(derivedKeyEl,
                                                                (EncryptedKey)derivationSource));
            cntx.isDerivedKeySeen = true;
        } else if (derivationSource instanceof KerberosSecurityToken) {
            cntx.derivedKeyTokens.add(deriveKeyFromKerberosToken(cntx, derivedKeyEl,
                                                                 (KerberosSecurityToken)derivationSource));
            cntx.isDerivedKeySeen = true;
        } else
            logger.info("Unsupported DerivedKeyToken reference target '" + derivationSource.getType() + "', ignoring this derived key.");
    }

    private XmlSecurityToken findSecurityContextTokenBySessionId(ProcessingStatusHolder cntx, String refUri) {
        Collection tokens = cntx.securityTokens;
        for (Iterator iterator = tokens.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if (o instanceof SecurityContextToken) {
                SecurityContextToken token = (SecurityContextToken)o;
                if (refUri.equals(token.getContextIdentifier()))
                    return token;
            }
        }
        return null;
    }

    // @return a new DerivedKeyToken.  Never null.
    private DerivedKeyToken deriveKeyFromEncryptedKey(Element derivedKeyEl, EncryptedKey ek) throws InvalidDocumentFormatException, GeneralSecurityException {
        try {
            SecureConversationKeyDeriver keyDeriver = new SecureConversationKeyDeriver();
            final byte[] resultingKey = keyDeriver.derivedKeyTokenToKey(derivedKeyEl, ek.getSecretKey());
            // remember this symmetric key so it can later be used to process the signature
            // or the encryption
            return new DerivedKeyTokenImpl(derivedKeyEl, resultingKey, ek);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    // @return a new DerivedKeyToken.  Never null.
    private DerivedKeyToken deriveKeyFromKerberosToken(ProcessingStatusHolder cntx,
                                                       Element derivedKeyEl,
                                                       KerberosSecurityToken kst)
            throws InvalidDocumentFormatException
    {
        assert derivedKeyEl != null;
        assert kst != null;
        assert cntx != null;
        try {
            SecureConversationKeyDeriver keyDeriver = new SecureConversationKeyDeriver();
            final byte[] resultingKey = keyDeriver.derivedKeyTokenToKey(derivedKeyEl,
                                                                           kst.getTicket().getServiceTicket().getKey());
            return new DerivedKeyTokenImpl(derivedKeyEl, resultingKey, kst);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    // @return a new DerivedKeyToken.  Never null.
    private DerivedKeyToken deriveKeyFromSecurityContext(Element derivedKeyEl, SecurityContextToken sct) throws InvalidDocumentFormatException {
        try {
            SecureConversationKeyDeriver keyDeriver = new SecureConversationKeyDeriver();
            final byte[] resultingKey = keyDeriver.derivedKeyTokenToKey(derivedKeyEl,
                                                           sct.getSecurityContext().getSharedSecret());
            // remember this symmetric key so it can later be used to process the signature
            // or the encryption
            return new DerivedKeyTokenImpl(derivedKeyEl, resultingKey, sct);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    /**
     * Locate an already-seen XmlSecurityToken in the specified context, by searching for the specified URI reference.
     *
     * @param cntx   the context to search.  Must not be null.
     * @param refUri  the URI reference, including initial "#" character.  Must not be null or non-empty.
     * @return the matching already-seen XmlSecurityToken, or null if none was found.
     * @throws InvalidDocumentFormatException if the URI reference is empty or does not begin with a hash mark
     */
    private XmlSecurityToken findXmlSecurityTokenById(ProcessingStatusHolder cntx, String refUri)
            throws InvalidDocumentFormatException
    {
        if (!refUri.startsWith("#"))
            throw new InvalidDocumentFormatException("SecurityTokenReference URI does not start with '#'");
        if (refUri.length() < 2)
            throw new InvalidDocumentFormatException("SecurityTokenReference URI is too short");
        refUri = refUri.substring(1);

        for (Iterator i = cntx.securityTokens.iterator(); i.hasNext();) {
            SecurityToken token = (SecurityToken)i.next();
            if (token instanceof XmlSecurityToken) {
                XmlSecurityToken xmlSecurityToken = (XmlSecurityToken)token;
                String thisId = xmlSecurityToken.getElementId();
                if (refUri.equals(thisId)) {
                    return xmlSecurityToken;
                }
            }
        }

        return null;
    }

    private String extractIdentifierFromSecConTokElement(Element secConTokEl) {
        // look for the wssc:Identifier child
        Element id = XmlUtil.findFirstChildElementByName(secConTokEl,
                                                         SoapUtil.WSSC_NAMESPACE_ARRAY,
                                                         SoapUtil.WSSC_ID_EL_NAME);
        if (id == null) return null;
        return XmlUtil.getTextValue(id);
    }

    private void processUsernameToken(final Element usernameTokenElement, ProcessingStatusHolder cntx)
            throws InvalidDocumentFormatException
    {
        final UsernameTokenImpl rememberedSecToken;
        try {
            rememberedSecToken = new UsernameTokenImpl(usernameTokenElement);
            cntx.securityTokens.add(rememberedSecToken);
        } catch (UnsupportedDocumentFormatException e) {
            // if the format is not supported, we should ignore it completly
            logger.log(Level.INFO, "A usernametoken element was encountered but we dont support the format.", e);
        }
    }

    // @return the ReferenceList element that was processed and decrypted, or null if
    //         the encryptedKey was ignored (intended for a downstream recipient) or if this EncryptedKey did not contain
    //         a reference list.
    //
    // If the encrypted key was addressed to us, it will have been added to the context ProcessedEncryptedKeys set.
    private Element processEncryptedKey(Element encryptedKeyElement,
                                        final ProcessingStatusHolder cntx)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing EncryptedKey");

        // If there's a KeyIdentifier, log whether it's talking about our key
        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
        try {
            KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyElement, cntx.securityTokenResolver, cntx.getMessageX509TokenResolver());
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
                return null;
            }
        }

        // verify that the algo is supported
        cntx.lastKeyEncryptionAlgorithm = XencUtil.checkEncryptionMethod(encryptedKeyElement);

        // We got the key. Get the list of elements to decrypt.
        Element refList = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                SoapUtil.XMLENC_NS,
                                                                SoapUtil.REFLIST_EL_NAME);

        final EncryptedKeyImpl ekTok;
        try {
            ekTok = new EncryptedKeyImpl(encryptedKeyElement, cntx.securityTokenResolver, cntx.getMessageX509TokenResolver());
            if (refList != null)
                decryptReferencedElements(ekTok.getSecretKey(), refList, cntx);
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

        String wsuId = SoapUtil.getElementWsuId(encryptedKeyElement);
        if (wsuId != null) cntx.encryptedKeyById.put(wsuId, ekTok);

        cntx.securityTokens.add(ekTok);
        cntx.addProcessedEncryptedKey(ekTok);
        return refList;
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
     * @param cntx  processing status holder
     * @throws java.security.GeneralSecurityException
     *                                  if there was a problem with a key or crypto provider
     * @throws javax.xml.parsers.ParserConfigurationException
     *                                  if there was a problem with the XML parser
     * @throws IOException              if there was an IO error while reading the document or a key
     * @throws org.xml.sax.SAXException if there was a problem parsing the document
     * @throws InvalidDocumentFormatException  if there is a problem decrypting an element due to message format
     * @throws ProcessorException  if an encrypted data element cannot be resolved
     */
    private void decryptReferencedElements(byte[] key, Element refList, ProcessingStatusHolder cntx)
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
            if (dataRefUri.startsWith("#"))
                dataRefUri = dataRefUri.substring(1);
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

    private void decryptElement(Element encryptedDataElement, byte[] key, ProcessingStatusHolder cntx)
            throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
            ProcessorException, InvalidDocumentFormatException
    {
        // Hacky for now -- we'll special case EncryptedHeader
        boolean wasEncryptedHeader = false;
        if ("EncryptedHeader".equals(encryptedDataElement.getLocalName())) {
            encryptedDataElement = XmlUtil.findFirstChildElementByName(encryptedDataElement,
                                                                       SoapUtil.XMLENC_NS,
                                                                       "EncryptedData");
            if (encryptedDataElement == null)
                throw new InvalidDocumentFormatException("EncryptedHeader did not contain EncryptedData");
            wasEncryptedHeader = true;
            cntx.isWsse11Seen = true;
        }

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
        final DecryptionContext dc = new DecryptionContext();
        final Collection algorithm = new ArrayList();

        // Support "flexible" answers to getAlgorithm() query when using 3des with HSM (Bug #3705)
        final FlexKey flexKey = new FlexKey(key);

        // override getEncryptionEngine to collect the encryptionmethod algorithm
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn() {
            public EncryptionEngine getEncryptionEngine(EncryptionMethod encryptionMethod)
                    throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, StructureException  {
                final String alguri = encryptionMethod.getAlgorithm();
                algorithm.add(alguri);
                try {
                    flexKey.setAlgorithm(XencUtil.getFlexKeyAlg(alguri));
                } catch (KeyException e) {
                    throw new NoSuchAlgorithmException("Unable to use algorithm " + alguri + " with provided key material", e);
                }
                return super.getEncryptionEngine(encryptionMethod);
            }
        };

        af.setProvider(JceProvider.getSymmetricJceProvider().getName());
        dc.setAlgorithmFactory(af);
        dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT,
                            null, null);
        dc.setKey(flexKey);
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
        // determine algorithm
        String algorithmName = XencAlgorithm.AES_128_CBC.getXEncName();
        if (!algorithm.isEmpty()) {
            if (algorithm.size() > 1)
                throw new ProcessorException("Multiple encryption algorithms found in element " + encryptedDataElement.getNodeName());
            algorithmName = algorithm.iterator().next().toString();
        }

        // Now record the fact that some data was encrypted.
        // Did the parent element contain any non-attribute content other than this EncryptedData
        // (and possibly some whitespace before and after)?
        if (wasEncryptedHeader) {
            // If this was an EncryptedHeader, do the special-case transformation to restore the plaintext header
            Element newHeader = XmlUtil.findOnlyOneChildElement(parentElement);
            Node newHeaderParent = parentElement.getParentNode();
            if (newHeaderParent == null || !(newHeaderParent instanceof Element))
                throw new InvalidDocumentFormatException("Root of document contained EncryptedHeader"); // sanity check, can't happen
            newHeaderParent.replaceChild(newHeader, parentElement); // promote decrypted header over top of EncryptedHeader
            logger.finer("All of encrypted header '" + newHeader.getLocalName() + "' was encrypted");
            cntx.elementsThatWereEncrypted.add(new EncryptedElementImpl(newHeader, algorithmName));
        } else if (onlyChild) {
            // All relevant content of the parent node was encrypted.
            logger.finer("All of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            cntx.elementsThatWereEncrypted.add(new EncryptedElementImpl(parentElement, algorithmName));
        } else {
            // There was unencrypted stuff mixed in with the EncryptedData, so we can only record elements as
            // encrypted that were actually wholly inside the EncryptedData.
            // TODO: In this situation, no note is taken of any encrypted non-Element nodes (such as text nodes)
            // This sucks, but at lesat this will err on the side of safety.
            logger.finer("Only some of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            for (int i = 0; i < dataList.getLength(); i++) {
                Node node = dataList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    cntx.elementsThatWereEncrypted.add(new EncryptedElementImpl((Element)node, algorithmName));
                }
            }
        }
    }

    private static class TimestampDate extends ParsedElementImpl implements WssTimestampDate {
        Date date;
        String dateString;

        TimestampDate(Element createdOrExpiresElement) throws ParseException {
            super(createdOrExpiresElement);
            dateString = XmlUtil.getTextValue(createdOrExpiresElement);
            date = ISO8601Date.parse(dateString);
        }

        public Date asDate() {
            return date;
        }

        public String asIsoString() {
            return dateString;
        }
    }

    private void processTimestamp(ProcessingStatusHolder ctx, final Element timestampElement)
            throws InvalidDocumentFormatException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing Timestamp");
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
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing BinarySecurityToken");

        // assume that this is a b64ed binary x509 cert, get the value
        String valueType = binarySecurityTokenElement.getAttribute("ValueType");
        String encodingType = binarySecurityTokenElement.getAttribute("EncodingType");

        // todo use proper qname comparator rather than this hacky suffix check
        if (!valueType.endsWith("X509v3") && !valueType.endsWith("GSS_Kerberosv5_AP_REQ"))
            throw new ProcessorException("BinarySecurityToken has unsupported ValueType " + valueType);
        if (!encodingType.endsWith("Base64Binary"))
            throw new ProcessorException("BinarySecurityToken has unsupported EncodingType " + encodingType);

        String value = XmlUtil.getTextValue(binarySecurityTokenElement);
        if (value == null || value.length() < 1) {
            String msg = "The " + binarySecurityTokenElement.getLocalName() + " does not contain a value.";
            logger.warning(msg);
            throw new ProcessorException(msg);
        }

        final byte[] decodedValue; // must strip whitespace or base64 decoder misbehaves
        try {
            decodedValue = HexUtils.decodeBase64(value, true);
        } catch (IOException e) {
            throw new InvalidDocumentFormatException("Unable to parse base64 BinarySecurityToken", e);
        }

        final String wsuId = SoapUtil.getElementWsuId(binarySecurityTokenElement);
        if(valueType.endsWith("X509v3")) {
            // create the x509 binary cert based on it
            X509Certificate referencedCert = CertUtils.decodeCert(decodedValue);

            // remember this cert
            if (wsuId == null) {
                logger.warning("This BinarySecurityToken does not have a recognized wsu:Id and may not be " +
                        "referenced properly by a subsequent signature.");
            }
            XmlSecurityToken rememberedSecToken = new X509BinarySecurityTokenImpl(referencedCert,
                                                                                  binarySecurityTokenElement);
            cntx.securityTokens.add(rememberedSecToken);
            cntx.x509TokensById.put(wsuId, rememberedSecToken);
        }
        else {
            try {
                cntx.securityTokens.add(new KerberosSecurityTokenImpl(new KerberosGSSAPReqTicket(decodedValue),
                                                                  wsuId,
                                                                  binarySecurityTokenElement));
            }
            catch(GeneralSecurityException gse) {
                if(ExceptionUtils.causedBy(gse, KerberosConfigException.class)) {
                    logger.info("Request contained Kerberos BinarySecurityToken but Kerberos is not configured.");
                }
                else {
                    logger.log(Level.WARNING, "Request contained Kerberos BinarySecurityToken that could not be processed.", gse);
                }
            }
        }
    }

    private void processSamlSecurityToken(final Element securityTokenElement, ProcessingStatusHolder context)
            throws InvalidDocumentFormatException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing saml:Assertion XML SecurityToken");
        try {
            final SamlAssertion samlToken = SamlAssertion.newInstance(securityTokenElement, context.securityTokenResolver);
            if (samlToken.hasEmbeddedIssuerSignature()) {
                samlToken.verifyEmbeddedIssuerSignature();

                class EmbeddedSamlSignatureToken extends SigningSecurityTokenImpl implements X509SecurityToken {
                    public EmbeddedSamlSignatureToken() {
                        super(null);
                    }

                    public X509Certificate getCertificate() {
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
                        return SecurityTokenType.WSS_X509_BST;
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
        } catch (SignatureException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    // TODO centralize this KeyInfo processing into the KeyInfoElement class somehow
    private SigningSecurityToken resolveSigningTokenByRef(final Element parentElement, ProcessingStatusHolder cntx) throws InvalidDocumentFormatException, GeneralSecurityException {
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
            List keyIdentifiers = XmlUtil.findChildElementsByName(securityTokenReference,
                                                                  SoapUtil.SECURITY_URIS_ARRAY,
                                                                  "KeyIdentifier");
            if (references.size() > 0) {
                // get the URI
                Element reference = (Element)references.get(0);
                String uriAttr = reference.getAttribute("URI");
                if (uriAttr == null || uriAttr.length() < 1) {
                    // not the food additive
                    String msg = "The Key info contains a reference but the URI attribute cannot be obtained";
                    logger.warning(msg);
                    return null;
                }
                if (uriAttr.charAt(0) == '#') {
                    uriAttr = uriAttr.substring(1);
                }
                // try to see if this reference matches a previously parsed SigningSecurityToken
                final X509SigningSecurityTokenImpl token = (X509SigningSecurityTokenImpl)cntx.x509TokensById.get(uriAttr);
                if (token != null) {
                    if(logger.isLoggable(Level.FINEST)) logger.finest("The keyInfo referred to a previously parsed Security Token '" + uriAttr + "'");
                    return token;
                }

                final EncryptedKey ekToken = (EncryptedKey)cntx.encryptedKeyById.get(uriAttr);
                if (ekToken != null) {
                    if(logger.isLoggable(Level.FINEST)) logger.finest("The KeyInfo referred to a previously decrypted EncryptedKey '" + uriAttr + "'");
                    return ekToken;
                }

                logger.fine("The reference " + uriAttr + " did not point to a X509Cert.");
            } else if (keyIdentifiers.size() > 0) {
                // TODO support multiple KeyIdentifier elements
                Element keyId = (Element)keyIdentifiers.get(0);
                String valueType = keyId.getAttribute("ValueType");
                String value = XmlUtil.getTextValue(keyId).trim();
                if (valueType != null && valueType.endsWith(SoapUtil.VALUETYPE_ENCRYPTED_KEY_SHA1_SUFFIX)) {
                    EncryptedKey found = resolveEncryptedKeyBySha1(cntx, value);
                    if (found != null) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("The KeyInfo referred to an already-known EncryptedKey token");
                        return found;
                    }
                } else if (valueType != null && valueType.endsWith(SoapUtil.VALUETYPE_X509_THUMB_SHA1_SUFFIX)) {
                    SigningSecurityToken token = (SigningSecurityToken)cntx.x509TokensByThumbprint.get(value);
                    if (token != null) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("The KeyInfo referred to a previously used X.509 token.");
                        return token;
                    }

                    if (cntx.securityTokenResolver == null) {
                        logger.warning("The KeyInfo referred to a ThumbprintSHA1, but no SecurityTokenResolver is available");
                    } else {
                        X509Certificate foundCert = cntx.securityTokenResolver.lookup(value);
                        if (foundCert == null) {
                            logger.info("The KeyInfo referred to a ThumbprintSHA1, but we were unable to locate a matching cert");
                        } else {
                            if(logger.isLoggable(Level.FINEST))
                                logger.finest("The KeyInfo referred to a recognized X.509 certificate by its thumbprint: " + foundCert.getSubjectDN().getName());
                            token = new X509BinarySecurityTokenImpl(foundCert, keyId);
                            cntx.securityTokens.add(token);
                            cntx.x509TokensByThumbprint.put(value, token);
                            return token;
                        }
                    }
                } else if (valueType != null && valueType.endsWith(SoapUtil.VALUETYPE_SKI_SUFFIX)) {
                    SigningSecurityToken token = (SigningSecurityToken)cntx.x509TokensBySki.get(value);
                    if (token != null) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("The KeyInfo referred to a previously used X.509 token.");
                        return token;
                    }

                    if (cntx.securityTokenResolver == null) {
                        logger.warning("The KeyInfo referred to a SKI, but no SecurityTokenResolver is available");
                    } else {
                        X509Certificate foundCert = cntx.securityTokenResolver.lookupBySki(value);
                        /*
                        this extra check may be useful if the resolver does not include the client cert
                        if (foundCert == null) {
                            if (cntx.senderCertificate != null) {
                                String senderSki = CertUtils.getSki(cntx.senderCertificate);
                                if (senderSki.equals(value)) {
                                    foundCert = cntx.senderCertificate;
                                }
                            }
                        }*/

                        if (foundCert == null) {
                            logger.info("The KeyInfo referred to a SKI (" + value + "), but we were unable to locate a matching cert");
                        } else {
                            if(logger.isLoggable(Level.FINEST))
                                logger.finest("The KeyInfo referred to a recognized X.509 certificate by its SKI: " + foundCert.getSubjectDN().getName());
                            token = new X509BinarySecurityTokenImpl(foundCert, keyId);
                            cntx.securityTokens.add(token);
                            cntx.x509TokensBySki.put(value, token);
                            return token;
                        }
                    }
                } else if (valueType != null && ArrayUtils.contains(SoapUtil.VALUETYPE_SAML_ASSERTIONID_ARRAY, valueType)) {
                    SigningSecurityToken token = (SigningSecurityToken) cntx.x509TokensById.get(value);
                    if (!(token instanceof SamlAssertion)) {
                        if(logger.isLoggable(Level.INFO))
                            logger.log(Level.INFO, "The KeyInfo referred to an unknown SAML token ''{0}''.", value);
                    }
                    return token;
                } else {
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("The KeyInfo used an unsupported KeyIdentifier ValueType: " + valueType);
                }
            } else {
                logger.warning("SecurityTokenReference does not contain any References");
            }
        }
        return null;
    }

    private EncryptedKey resolveEncryptedKeyBySha1(ProcessingStatusHolder cntx, String eksha1) throws InvalidDocumentFormatException, GeneralSecurityException {
        // We are trying to produce an EncryptedKey instance that matches this EncryptedKeySHA1 value.
        // If a SecurityTokenResolver exists and has already unwrapped a key with this EncryptedKeySHA1, then
        //    we'll reuse the already-unwrapped key.
        // If we have already seen an EncryptedKey in this request with a matching EncryptedKeySHA1, we'll return
        //    that token, after ensuring its key is unwrapped, reusing the cached key if possible.
        // If this request did not include a matching EncryptedKey, but we have a cached secret key matching
        //    this EncryptedKeySHA1, we'll create a new virtual EncryptedKey and add it to this request.

        SecurityTokenResolver resolver = cntx.securityTokenResolver;
        byte[] cachedSecretKey = resolver == null ? null : resolver.getSecretKeyByEncryptedKeySha1(eksha1);
        EncryptedKey found = findEncryptedKey(cntx.securityTokens, eksha1);

        if (found == null && cachedSecretKey == null) {
            // We've struck out completely.
            if (resolver == null)
                logger.warning("The KeyInfo referred to an EncryptedKey token, but no EncryptedKey was present with a matching EncryptedKeySHA1, and no SecurityTokenResovler is available");
            else
                logger.warning("The KeyInfo referred to an EncryptedKey token, but no EncryptedKey was known with a matching EncryptedKeySHA1");
            return null;
        }

        if (found == null) {
            // Make a new virtual token
            found = WssProcessorUtil.makeEncryptedKey(cntx.releventSecurityHeader.getOwnerDocument(), cachedSecretKey, eksha1);
            cntx.securityTokens.add(found);
        } else if (cachedSecretKey != null && !found.isUnwrapped() && found instanceof EncryptedKeyImpl) {
            EncryptedKeyImpl eki = (EncryptedKeyImpl)found;
            eki.setSecretKey(cachedSecretKey);
        }

        return found;
    }

    // @return the token in tokes that is an EncryptedKey with the specified EncryptedKeySHA1, or null
    private EncryptedKey findEncryptedKey(Collection tokes, String eksha1) {
        for (Iterator i = tokes.iterator(); i.hasNext();) {
            SecurityToken token = (SecurityToken)i.next();
            if (token instanceof EncryptedKey) {
                EncryptedKey ek = (EncryptedKey)token;
                if (eksha1.equals(ek.getEncryptedKeySHA1()))
                    return ek;
            }
        }
        return null;
    }

    // TODO merge this into KeyInfoElement class somehow
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
                    return null;
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
                logger.finest("SecurityTokenReference does not contain any References");
            }
        }
        return null;
    }

    private void processSignature(final Element sigElement,
                                  final SecurityContextFinder securityContextFinder,
                                  final ProcessingStatusHolder cntx)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, IOException {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing Signature");

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
        SigningSecurityToken signingToken = resolveSigningTokenByRef(keyInfoElement, cntx);
        X509SigningSecurityTokenImpl signingCertToken = null;
        if (signingToken instanceof X509SigningSecurityTokenImpl)
            signingCertToken = (X509SigningSecurityTokenImpl)signingToken;

        if (signingCertToken != null) {
            signingCert = signingCertToken.getMessageSigningCertificate();
        }
        // NOTE if we want to resolve embedded we need to set a signingCertToken as below
        // if (signingCert == null) { //try to resolve it as embedded
        //     signingCert = resolveEmbeddedCert(keyInfoElement);
        // }
        if (signingCert == null) { // last chance: see if we happen to recognize a SKI, perhaps because it is ours or theirs
            signingCert = resolveCertBySkiRef(cntx, keyInfoElement);
            if (signingCert != null) {
                // This dummy BST matches the required format for signing via an STR-Transform
                // for STR-Transform the prefix must match the one on the STR
                String wsseNs = cntx.releventSecurityHeader.getNamespaceURI();
                Element strEle = XmlUtil.findOnlyOneChildElementByName(keyInfoElement,
                                                            SoapUtil.SECURITY_URIS_ARRAY,
                                                            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
                final String wssePrefix;
                if (strEle == null) {
                    wssePrefix = cntx.releventSecurityHeader.getPrefix();
                } else {
                    wssePrefix = strEle.getPrefix();
                }
                final Element bst;
                if (wssePrefix == null) {
                    bst = sigElement.getOwnerDocument().createElementNS(wsseNs, "BinarySecurityToken");
                    bst.setAttribute("xmlns", wsseNs);
                } else {
                    bst = sigElement.getOwnerDocument().createElementNS(wsseNs, wssePrefix+":BinarySecurityToken");
                    bst.setAttribute("xmlns:"+wssePrefix, wsseNs);
                }
                bst.setAttribute("ValueType", SoapUtil.VALUETYPE_X509);
                XmlUtil.setTextContent(bst, HexUtils.encodeBase64(signingCert.getEncoded(), true));

                signingCertToken = new X509BinarySecurityTokenImpl(signingCert, bst);
                cntx.securityTokens.add(signingCertToken); // nasty, blah
            }
        }

        // Process any STR that is used within the signature
        Element keyInfoStr = XmlUtil.findFirstChildElementByName(keyInfoElement, SoapUtil.SECURITY_URIS_ARRAY, "SecurityTokenReference");
        if (keyInfoStr != null && SoapUtil.getElementWsuId(keyInfoStr)!=null) {
            processSecurityTokenReference(keyInfoStr, securityContextFinder, cntx);
        }

        if (signingCert == null && dkt != null) {
            signingKey = new SecretKeySpec(dkt.getComputedDerivedKey(), "SHA1");
        } else if (signingCert != null) {
            signingKey = signingCert.getPublicKey();
        } else if (signingToken instanceof EncryptedKey) {
            signingKey = new SecretKeySpec(((EncryptedKey)signingToken).getSecretKey(), "SHA1");
        }

        if (signingKey == null) {
            // some toolkits can base their signature on a usernametoken
            // although the ssg does not support that, we should not throw
            // but just ignore this signature because the signature may be
            // useful for a downstream service (see bugzilla #1585)
            String msg = "Was not able to get cert, derived key, or EncryptedKey from signature's keyinfo. ignoring this signature";
            logger.info(msg);
            // dont throw, just ignore signature!
            return;
        }

        if (signingCert != null) {
            try {
                signingCert.checkValidity();
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
        MimeKnob mimeKnob = (MimeKnob) cntx.message.getKnob(MimeKnob.class);
        PartIterator iterator = mimeKnob==null ? null : mimeKnob.getParts();
        Map<String,PartInfo> partMap = new HashMap();
        sigContext.setEntityResolver(new AttachmentEntityResolver(iterator, XmlUtil.getXss4jEntityResolver(), partMap, signedAttachmentSizeLimit));
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                Element found = (Element)cntx.elementsByWsuId.get(s);
                if (found != null) {
                    // See if we need to remove any processed encrypted key elements.  We'll need to
                    // remove the encrypted key elements if this signature covers the Envelope or the
                    // Security header.
                    if (found == cntx.processedDocument.getDocumentElement() || found == cntx.releventSecurityHeader) {
                        // It's an enveloped signature, so remove any already-processed EncryptedKey elements
                        // before computing this hash
                        Set keys = cntx.getProcessedEncryptedKeys();
                        for (Iterator i = keys.iterator(); i.hasNext();) {
                            Object o = i.next();
                            if (o instanceof EncryptedKey) {
                                EncryptedKey ek = (EncryptedKey)o;
                                Element element = ek.asElement();
                                Node parent = element.getParentNode();
                                if (parent != null) {
                                    parent.removeChild(element);
                                    i.remove();
                                }
                            }
                        }
                    }

                    return found;
                }

                return SoapUtil.getElementByWsuId(doc, s);
            }
        });
        final Map<Node, Node> strToTarget = cntx.securityTokenReferenceElementToTargetElement;
        sigContext.setAlgorithmFactory(new WssProcessorAlgorithmFactory(strToTarget));
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
            StringBuffer msg = new StringBuffer("Signature not valid. " + validity.getSignedInfoMessage());
            for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                msg.append("\n\tElement ").append(validity.getReferenceURI(i)).append(": ").append(validity.getReferenceMessage(i));
            }
            logger.warning(msg.toString());
            throw new InvalidDocumentSignatureException(msg.toString());
        }

        // Save the SignatureValue
        Element sigValueEl = XmlUtil.findOnlyOneChildElementByName(sigElement, sigElement.getNamespaceURI(), "SignatureValue");
        if (sigValueEl == null)
            throw new ProcessorException("Valid ds:Signature contained no ds:SignatureValue"); // can't happen
        cntx.lastSignatureValue = XmlUtil.getTextValue(sigValueEl);

        // Remember which elements were covered
        final int numberOfReferences = validity.getNumberOfReferences();
        for (int i = 0; i < numberOfReferences; i++) {
            // Resolve each elements one by one.
            String elementCoveredURI = validity.getReferenceURI(i);
            PartInfo partCovered = partMap.get(elementCoveredURI);
            Element elementCovered = null;

            if ( partCovered == null ) {
                if (elementCoveredURI!=null && elementCoveredURI.charAt(0) == '#') {
                    elementCoveredURI = elementCoveredURI.substring(1);
                }
                elementCovered = (Element)cntx.elementsByWsuId.get(elementCoveredURI);
                if (elementCovered == null)
                    elementCovered = SoapUtil.getElementByWsuId(sigElement.getOwnerDocument(), elementCoveredURI);
                if (elementCovered == null) {
                    String msg = "Element covered by signature cannot be found in original document nor in " +
                            "processed document. URI: " + elementCoveredURI;
                    logger.warning(msg);
                    throw new InvalidDocumentFormatException(msg);
                }
            }

            // find signing security token
            final SigningSecurityToken signingSecurityToken;
            if (signingCertToken != null) {
                signingSecurityToken = signingCertToken;
            } else if (dkt != null) {
                // If signed by a derived key token, credit the signature to the derivation source instead of the DKT
                XmlSecurityToken token = dkt.getSourceToken();
                if (token instanceof SigningSecurityTokenImpl) {
                    signingSecurityToken = (SigningSecurityTokenImpl)dkt.getSourceToken();
                } else {
                    throw new InvalidDocumentFormatException("Unable to record signature using unsupport key derivation source: " + token.getType());
                }
            } else if (signingToken instanceof SigningSecurityTokenImpl) {
                signingSecurityToken = (SigningSecurityTokenImpl)signingToken;
            } else
                throw new RuntimeException("No signing security token found");

            // record for later
            if (elementCovered != null) {
                // check whether this is a token reference
                Element targetElement = (Element)cntx.securityTokenReferenceElementToTargetElement.get(elementCovered);
                if (targetElement != null) {
                    elementCovered = targetElement;
                }
                // make reference to this element
                final SignedElement signedElement = new SignedElementImpl(signingSecurityToken, elementCovered);
                cntx.elementsThatWereSigned.add(signedElement);
                signingSecurityToken.addSignedElement(signedElement);
            } else {
                // make reference to this part
                final SignedPart signedPart = new SignedPartImpl(signingSecurityToken, partCovered);;
                cntx.partsThatWereSigned.add(signedPart);
                signingSecurityToken.addSignedPart(signedPart);
            }
            signingSecurityToken.onPossessionProved();
        }
    }

    // @return the identified cert from its SKI, or null if we struck out
    private X509Certificate resolveCertBySkiRef(ProcessingStatusHolder cntx, Element ki) throws InvalidDocumentFormatException {
        // We might have here a KeyInfo/SecurityTokenReference/KeyId[@valueType="...SKI"]/BASE64EDCRAP
        if (cntx.senderCertificate == null)
            return null; // nothing to compare it with
        try {
            KeyInfoElement.assertKeyInfoMatchesCertificate(ki, cntx.senderCertificate);
            return cntx.senderCertificate;
        } catch (UnexpectedKeyInfoException e) {
            // Ski was mentioned, but did not match senderCert.
            return null;
        } catch (KeyInfoElement.UnsupportedKeyInfoFormatException e) {
            // We didn't recognize this KeyInfo
            return null;
        } catch (CertificateException e) {
            throw new InvalidDocumentFormatException("KeyInfo contained an embedded cert, but it could not be decoded", e);
        }
    }

    private ProcessorResult produceResult(final ProcessingStatusHolder cntx) {
        ProcessorResult processorResult = new ProcessorResult() {

            public SignedElement[] getElementsThatWereSigned() {
                return (SignedElement[])cntx.elementsThatWereSigned.toArray(PROTOTYPE_SIGNEDELEMENT_ARRAY);
            }

            public EncryptedElement[] getElementsThatWereEncrypted() {
                return (EncryptedElement[])cntx.elementsThatWereEncrypted.toArray(PROTOTYPE_ELEMENT_ARRAY);
            }

            public SignedPart[] getPartsThatWereSigned() {
                return cntx.partsThatWereSigned.toArray(PROTOTYPE_SIGNEDPART_ARRAY);
            }

            public XmlSecurityToken[] getXmlSecurityTokens() {
                return (XmlSecurityToken[])cntx.securityTokens.toArray(PROTOTYPE_SECURITYTOKEN_ARRAY);
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
                        XmlSecurityToken token = (XmlSecurityToken)i.next();
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

            public String getLastSignatureValue() {
                return cntx.lastSignatureValue;
            }

            public String getLastSignatureConfirmation()
            {
                return cntx.lastSignatureConfirmation;
            }

            public String getLastKeyEncryptionAlgorithm() {
                return cntx.lastKeyEncryptionAlgorithm;
            }

            public boolean isWsse11Seen() {
                return cntx.isWsse11Seen;
            }

            public boolean isDerivedKeySeen() {
                return cntx.isDerivedKeySeen;
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
        if (cntx.timestamp != null) {
            Element timeElement = cntx.timestamp.asElement();
            SigningSecurityToken[] signingTokens = processorResult.getSigningTokens(timeElement);
            if (signingTokens.length == 1) {
                cntx.timestamp.setSigned();
            } else if (signingTokens.length > 1) {
                throw new IllegalStateException("More then one signing token over Timestamp detected!");
            }
        }
        return processorResult;
    }

    private static final Logger logger = Logger.getLogger(WssProcessorImpl.class.getName());
    private static final ParsedElement[] PROTOTYPE_ELEMENT_ARRAY = new EncryptedElement[0];
    private static final SignedElement[] PROTOTYPE_SIGNEDELEMENT_ARRAY = new SignedElement[0];
    private static final SignedPart[] PROTOTYPE_SIGNEDPART_ARRAY = new SignedPart[0];
    private static final XmlSecurityToken[] PROTOTYPE_SECURITYTOKEN_ARRAY = new XmlSecurityToken[0];

    private long signedAttachmentSizeLimit;

    private class ProcessingStatusHolder {
        final Message message;
        final Document processedDocument;
        final Collection elementsThatWereSigned = new ArrayList();
        final Collection elementsThatWereEncrypted = new ArrayList();
        final Collection<SignedPart> partsThatWereSigned = new ArrayList();
        final Collection securityTokens = new ArrayList();
        final Collection derivedKeyTokens = new ArrayList();
        Map elementsByWsuId = null;
        TimestampImpl timestamp = null;
        Element releventSecurityHeader = null;
        Map x509TokensById = new HashMap();
        Map x509TokensByThumbprint = new HashMap();
        Map x509TokensBySki = new HashMap();
        Map<Node,Node> securityTokenReferenceElementToTargetElement = new HashMap();
        Map encryptedKeyById = new HashMap();
        private Set processedEncryptedKeys = null;
        SecurityActor secHeaderActor;
        boolean documentModified = false;
        boolean encryptionIgnored = false;
        X509Certificate senderCertificate = null;
        String lastSignatureValue = null;
        String lastSignatureConfirmation = null;
        String lastKeyEncryptionAlgorithm = null;
        boolean isWsse11Seen = false;
        boolean isDerivedKeySeen = false; // If we see any derived keys, we'll assume we can derive our own keys in reponse
        SecurityTokenResolver securityTokenResolver = null;
        Resolver<String,X509Certificate> messageX509TokenResolver = null;

        public ProcessingStatusHolder(Message message, Document processedDocument) {
            this.message = message;
            this.processedDocument = processedDocument;
        }

        void addProcessedEncryptedKey(EncryptedKey ek) {
            getProcessedEncryptedKeys().add(ek);
        }

        Set getProcessedEncryptedKeys() {
            if (processedEncryptedKeys == null) processedEncryptedKeys = new HashSet();
            return processedEncryptedKeys;
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
                Document d = message.getXmlKnob().getDocumentWritable();
                if (d != processedDocument)
                    throw new IllegalStateException("Writable document is not the same as the one we started to process"); // can't happen
            } catch (SAXException e) {
                throw new CausedIllegalStateException(e); // can't happen anymore
            } catch (IOException e) {
                throw new CausedIllegalStateException(e); // can't happen anymore
            }
        }

        /**
         * Return a resolver that will find certs from already-seen X.509 tokens in this message by their wsu:Id.
         * @return a Resolver<String,X509Certificate> that will find certs from already-seen X.509 BSTs in this message processing context
         */
        public Resolver<String,X509Certificate> getMessageX509TokenResolver() {
            if (messageX509TokenResolver != null)
                return messageX509TokenResolver;
            return messageX509TokenResolver = new Resolver<String,X509Certificate>() {
                public X509Certificate resolve(String id) {
                    X509Certificate resolved = null;
                    Object token = x509TokensById.get(id);
                    if (token instanceof X509BinarySecurityTokenImpl) {
                        X509BinarySecurityTokenImpl bst = (X509BinarySecurityTokenImpl) token;
                        resolved = bst.getCertificate();
                    }
                    return resolved;
                }
            };
        }
    }

    private static class X509BinarySecurityTokenImpl extends X509SigningSecurityTokenImpl implements X509SecurityToken {
        private final X509Certificate finalcert;

        public X509BinarySecurityTokenImpl(X509Certificate finalcert, Element binarySecurityTokenElement) {
            super(binarySecurityTokenElement);
            this.finalcert = finalcert;
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.WSS_X509_BST;
        }

        public String getElementId() {
            return SoapUtil.getElementWsuId(asElement());
        }

        public X509Certificate getMessageSigningCertificate() {
            return finalcert;
        }

        public X509Certificate getCertificate() {
            return finalcert;
        }

        public String toString() {
            return "X509SecurityToken: " + finalcert.toString();
        }
    }

    private static class TimestampImpl extends ParsedElementImpl implements WssTimestamp {
        private final TimestampDate createdTimestampDate;
        private final TimestampDate expiresTimestampDate;
        private boolean signed = false;

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
            return signed;
        }

        void setSigned() {
            signed = true;
        }
    }

    private static class DerivedKeyTokenImpl extends ParsedElementImpl implements DerivedKeyToken {
        private final byte[] finalKey;
        private final XmlSecurityToken sourceToken;
        private final String elementWsuId;

        public DerivedKeyTokenImpl(Element dktel, byte[] finalKey, XmlSecurityToken sourceToken) {
            super(dktel);
            this.finalKey = finalKey;
            this.sourceToken = sourceToken;
            elementWsuId = SoapUtil.getElementWsuId(dktel);
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.WSSC_DERIVED_KEY;
        }

        public String getElementId() {
            return elementWsuId;
        }

        byte[] getComputedDerivedKey() {
            return finalKey;
        }

        public XmlSecurityToken getSourceToken() {
            return sourceToken;
        }

        public String toString() {
            return "DerivedKeyToken: " + finalKey.toString();
        }

        public byte[] getSecretKey() {
            return finalKey;
        }
    }

    private static class EncryptedKeyImpl extends SigningSecurityTokenImpl implements EncryptedKey {
        private final String elementWsuId;
        private final byte[] encryptedKeyBytes;
        private final SignerInfo signerInfo;
        private SecurityTokenResolver tokenResolver;
        private String encryptedKeySHA1 = null;
        private byte[] secretKeyBytes = null;

        // Constructor that supports lazily-unwrapping the key
        EncryptedKeyImpl(Element encryptedKeyEl, SecurityTokenResolver tokenResolver, Resolver<String,X509Certificate> x509Resolver)
                throws InvalidDocumentFormatException, IOException, GeneralSecurityException, UnexpectedKeyInfoException {
            super(encryptedKeyEl);
            this.elementWsuId = SoapUtil.getElementWsuId(encryptedKeyEl);
            this.tokenResolver = tokenResolver;
            this.signerInfo = KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyEl, tokenResolver, x509Resolver);
            String cipherValueB64 = XencUtil.getEncryptedKeyCipherValue(encryptedKeyEl);
            this.encryptedKeyBytes = HexUtils.decodeBase64(cipherValueB64.trim());
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.WSS_ENCRYPTEDKEY;
        }

        public String getElementId() {
            return elementWsuId;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer("EncryptedKey: wsuId=");
            sb.append(elementWsuId).append(" unwrapped=").append(isUnwrapped());
            if (secretKeyBytes != null) sb.append(" keylength=").append(secretKeyBytes.length);
            if (encryptedKeySHA1 != null) sb.append(" encryptedKeySha1=").append(encryptedKeySHA1);
            return sb.toString();
        }

        private void unwrapKey() throws InvalidDocumentFormatException, GeneralSecurityException {
            getEncryptedKeySHA1();
            if (secretKeyBytes != null) return;

            // Extract the encrypted key
            Element encMethod = XmlUtil.findOnlyOneChildElementByName(asElement(),
                                                                      SoapUtil.XMLENC_NS,
                                                                      "EncryptionMethod");
            secretKeyBytes = XencUtil.decryptKey(encryptedKeyBytes, XencUtil.getOaepBytes(encMethod), signerInfo.getPrivate());

            // Since we've just done the expensive work, ensure that it gets saved for future reuse
            maybePublish();
        }

        public byte[] getSecretKey() throws InvalidDocumentFormatException, GeneralSecurityException {
            if (secretKeyBytes == null)
                unwrapKey();
            return secretKeyBytes;
        }

        public boolean isUnwrapped() {
            return secretKeyBytes != null;
        }

        void setSecretKey(byte[] secretKeyBytes) {
            this.secretKeyBytes = secretKeyBytes;
            maybePublish();
        }

        private void maybePublish() {
            if (tokenResolver != null && encryptedKeySHA1 != null && secretKeyBytes != null) {
                tokenResolver.putSecretKeyByEncryptedKeySha1(encryptedKeySHA1, secretKeyBytes);
                tokenResolver = null;
            }
        }

        public String getEncryptedKeySHA1() {
            if (encryptedKeySHA1 != null)
                return encryptedKeySHA1;
            encryptedKeySHA1 = XencUtil.computeEncryptedKeySha1(encryptedKeyBytes);
            if (secretKeyBytes == null && tokenResolver != null) {
                // Save us a step unwrapping
                secretKeyBytes = tokenResolver.getSecretKeyByEncryptedKeySha1(encryptedKeySHA1);
            } else
                maybePublish();
            return encryptedKeySHA1;
        }
    }

    private static class SecurityContextTokenImpl extends SigningSecurityTokenImpl implements SecurityContextToken {
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

    private static class SignedElementImpl implements SignedElement {
        private final SigningSecurityToken signingToken;
        private final Element element;

        public SignedElementImpl(SigningSecurityToken signingToken, Element element) {
            this.signingToken = signingToken;
            this.element = element;
        }

        public SigningSecurityToken getSigningSecurityToken() {
            return signingToken;
        }

        public Element asElement() {
            return element;
        }
    }

    private static class SignedPartImpl implements SignedPart {
        private final SigningSecurityToken signingToken;
        private final PartInfo partInfo;

        public SignedPartImpl(SigningSecurityToken signingToken, PartInfo partInfo) {
            this.signingToken = signingToken;
            this.partInfo = partInfo;
        }

        public SigningSecurityToken getSigningSecurityToken() {
            return signingToken;
        }

        public PartInfo getPartInfo() {
            return partInfo;
        }
    }
}
