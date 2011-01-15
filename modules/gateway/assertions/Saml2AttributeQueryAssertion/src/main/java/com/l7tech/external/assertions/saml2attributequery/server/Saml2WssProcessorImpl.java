package com.l7tech.external.assertions.saml2attributequery.server;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.*;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.processor.*;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles decoration for the SAML2 attributes.
 */
public class Saml2WssProcessorImpl {
    private static final Logger logger = Logger.getLogger(Saml2WssProcessorImpl.class.getName());

    private final Message message;

    private final Collection<SignedElement> elementsThatWereSigned = new ArrayList<SignedElement>();
    private final Collection<EncryptedElement> elementsThatWereEncrypted = new ArrayList<EncryptedElement>();
    private final Collection<SignedPart> partsThatWereSigned = new ArrayList<SignedPart>();
    private final Collection<XmlSecurityToken> securityTokens = new ArrayList<XmlSecurityToken>();
    public final List<SignatureConfirmation> signatureConfirmationValues = new ArrayList<SignatureConfirmation>();
    public final List<String> validatedSignatureValues = new ArrayList<String>();

    private X509Certificate senderCertificate = null;
    private SecurityTokenResolver securityTokenResolver = null;
    private SecurityContextFinder securityContextFinder = null;

    private Document processedDocument;
    private Map<String,Element> elementsByWsuId = null;
    private TimestampImpl timestamp = null;
    private Element releventSecurityHeader = null;
    private Map<String,SigningSecurityToken> x509TokensByThumbprint = new HashMap<String,SigningSecurityToken>();
    private Map<String,SigningSecurityToken> x509TokensBySki = new HashMap<String,SigningSecurityToken>();
    private Map<Node,Node> strToTarget = new HashMap<Node,Node>();
    private Map<String,EncryptedKey> encryptedKeyById = new HashMap<String,EncryptedKey>();
    private Set<EncryptedKey> processedEncryptedKeys = null;
    private String lastKeyEncryptionAlgorithm = null;
    private boolean isWsse11Seen = false;
    private boolean isDerivedKeySeen = false; // If we see any derived keys, we'll assume we can derive our own keys in reponse
    private Resolver<String,X509Certificate> messageX509TokenResolver = null;
    boolean checkSigningCertValidity = true;

    /**
     * Create a WssProcessorImpl context bound to the specified message.
     *
     * @param message the Message, providing access to attachments if needed to check signatures.  Required.
     */
    public Saml2WssProcessorImpl(Message message) {
        this.message = message;
    }

    /**
     * @param securityTokenResolver   a resolver for looking up certificates in various ways, or null disable certificate reference support.
     */
    public void setSecurityTokenResolver(SecurityTokenResolver securityTokenResolver) {
        this.securityTokenResolver = securityTokenResolver;
    }

    /**
     * Process the current Message in-place.
     * That is, the contents of the Header/Security are processed as per the WSS rules.
     * The message may be modified if encrypted elements are decrypted.  If this modification results in an
     * empty Security or SOAP Header element then the newly-empty elements will be removed as well.
     * <p/>
     * This uses the currently bound Message and the currently-set senderCertificate, securityContextFinder,
     * securityTokenResolver, and other instance settings.
     *
     * @param signedElement  the element containing the ds:Signature.  Required.
     * @param securityChildToProcess  the ds:Signature element.  Required.
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws InvalidDocumentFormatException if there is a problem with the document format that can't be ignored
     * @throws java.security.GeneralSecurityException if there is a problem with a key or certificate
     * @throws ProcessorException in case of some other problem
     * @throws BadSecurityContextException if the message contains a WS-SecureConversation SecurityContextToken, but the securityContextFinder has no record of that session.
     * @throws org.xml.sax.SAXException if the first part's content type is not text/xml; or,
     *                      if the XML in the first part's InputStream is not well formed
     * @throws java.io.IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if there is a problem reading from or writing to a stash
     * @throws IllegalStateException if the Message has not yet been initialized
     */
    public ProcessorResult processMessage(Element signedElement, Element securityChildToProcess)
            throws InvalidDocumentFormatException, ProcessorException, GeneralSecurityException, IOException, BadSecurityContextException, SAXException
    {
        if (message == null)
            throw new IllegalStateException("this WssProcessorImpl instance was not bound to a message upon creation");

        this.processedDocument = message.getXmlKnob().getDocumentReadOnly();
        // Reset all potential outputs
        elementsThatWereSigned.clear();
        elementsThatWereEncrypted.clear();
        securityTokens.clear();
        timestamp = null;
        releventSecurityHeader = null;
        elementsByWsuId = DomUtils.getElementByIdMap(processedDocument, SoapConstants.DEFAULT_ID_ATTRIBUTE_CONFIG);

        releventSecurityHeader = signedElement;
        if(releventSecurityHeader != null && releventSecurityHeader.getAttribute("ID") != null) {
            elementsByWsuId = new HashMap<String, Element>();
            elementsByWsuId.put(releventSecurityHeader.getAttribute("ID"), releventSecurityHeader);
        }

        // maybe there are no security headers at all in which case, there is nothing to process
        if (releventSecurityHeader == null) {
            logger.finer("No relevent security header found.");
            return produceResult();
        }

        // Process the digital signature
        if (securityChildToProcess != null) {
            Node parent = null;
            Node nextSibling = null;

            if(XmlUtil.isElementAncestor(securityChildToProcess, releventSecurityHeader)) {
                parent = securityChildToProcess.getParentNode();
                nextSibling = securityChildToProcess.getNextSibling();
            }
            releventSecurityHeader.removeChild(securityChildToProcess);
            
            processSignature(securityChildToProcess, securityContextFinder);

            if(XmlUtil.isElementAncestor(securityChildToProcess, releventSecurityHeader) && parent != null) {
                parent.insertBefore(securityChildToProcess, nextSibling);
            }
        }

        return produceResult();
    }

    private Set<EncryptedKey> getProcessedEncryptedKeys() {
        if (processedEncryptedKeys == null) processedEncryptedKeys = new HashSet<EncryptedKey>();
        //noinspection ReturnOfCollectionOrArrayField
        return processedEncryptedKeys;
    }

    // @return the identified cert from its SKI, or null if we struck out
    private X509Certificate resolveCertBySkiRef(Element ki) throws InvalidDocumentFormatException {
        // We might have here a KeyInfo/SecurityTokenReference/KeyId[@valueType="...SKI"]/BASE64EDCRAP
        if (senderCertificate == null)
            return null; // nothing to compare it with
        try {
            KeyInfoElement.assertKeyInfoMatchesCertificate(ki, senderCertificate);
            return senderCertificate;
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

    private ProcessorResult produceResult() {
        return new ProcessorResult() {
            public SignedElement[] getElementsThatWereSigned() {
                return elementsThatWereSigned.toArray(new SignedElement[elementsThatWereSigned.size()]);
            }

            public EncryptedElement[] getElementsThatWereEncrypted() {
                return elementsThatWereEncrypted.toArray(new EncryptedElement[elementsThatWereEncrypted.size()]);
            }

            public SignedPart[] getPartsThatWereSigned() {
                return partsThatWereSigned.toArray(new SignedPart[partsThatWereSigned.size()]);
            }

            public XmlSecurityToken[] getXmlSecurityTokens() {
                return securityTokens.toArray(new XmlSecurityToken[securityTokens.size()]);
            }

            public WssTimestamp getTimestamp() {
                return timestamp;
            }

            public String getSecurityNS() {
                if (releventSecurityHeader != null) {
                    return releventSecurityHeader.getNamespaceURI();
                }
                return null;
            }

            public String getWSUNS() {
                // look for the wsu namespace somewhere
                if (timestamp != null && timestamp.asElement() != null) {
                    return timestamp.asElement().getNamespaceURI();
                } else if (securityTokens != null && !securityTokens.isEmpty()) {
                    for (XmlSecurityToken token : securityTokens) {
                        NamedNodeMap attributes = token.asElement().getAttributes();
                        for (int j = 0; j < attributes.getLength(); j++) {
                            Attr n = (Attr)attributes.item(j);
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

            @Override
            public String getWsscNS() {
                return SoapConstants.WSSC_NAMESPACE;
            }

            public SecurityActor getProcessedActor() {
                return null;
            }

            @Override
            public String getProcessedActorUri() {
                return null;
            }

            public List<String> getValidatedSignatureValues() {
                return validatedSignatureValues;
            }

            public List<SignatureConfirmation> getSignatureConfirmationValues() {
                return signatureConfirmationValues;
            }

            @Override
            public SignatureConfirmation getSignatureConfirmation() {
                return null;
            }

            public String getLastKeyEncryptionAlgorithm() {
                return lastKeyEncryptionAlgorithm;
            }

            public boolean isWsse11Seen() {
                return isWsse11Seen;
            }

            public boolean isDerivedKeySeen() {
                return isDerivedKeySeen;
            }

            /**
             * @param element the element to find the signing tokens for
             * @return the array if tokens that signed the element or empty array if none
             */
            public SigningSecurityToken[] getSigningTokens(Element element) {
                if (element == null) {
                    throw new IllegalArgumentException();
                }

                Collection<SigningSecurityToken> tokens = new ArrayList<SigningSecurityToken>();
                if (processedDocument != element.getOwnerDocument()) {
                    throw new IllegalArgumentException("This element does not belong to the same document as processor result!");
                }

                for (SecurityToken securityToken : securityTokens) {
                    if (securityToken instanceof SigningSecurityToken) {
                        SigningSecurityToken signingSecurityToken = (SigningSecurityToken)securityToken;
                        final SignedElement[] signedElements = signingSecurityToken.getSignedElements();
                        for (int i = signedElements.length - 1; i >= 0; i--) {
                            SignedElement signedElement = signedElements[i];
                            if (element.equals(signedElement.asElement())) {
                                tokens.add(signingSecurityToken);
                            }
                        }
                    }
                }
                return tokens.toArray(new SigningSecurityToken[tokens.size()]);
            }
        };
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
     * @throws InvalidDocumentFormatException if STR is invalid format or points at something unsupported
     * @throws ProcessorException if a securityContextFinder is required to resolve this STR, but one was not provided
     */
    private void processSecurityTokenReference(Element str, SecurityContextFinder securityContextFinder)
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
            Element target = elementsByWsuId.get(value);
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
            strToTarget.put(str, target);
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
            Element target = elementsByWsuId.get(value);
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
            strToTarget.put(str, target);
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
                securityTokens.add(secConTok);
            }
            else {
                logger.warning("Could not find referenced Kerberos security token '"+value+"'.");
            }
        } else {
            logger.warning("Ignoring SecurityTokenReference ID=" + id + " with ValueType of " + valueType);
        }
    }

    // TODO centralize this KeyInfo processing into the KeyInfoElement class somehow
    private SigningSecurityToken resolveSigningTokenByRef(final Element parentElement) throws InvalidDocumentFormatException, GeneralSecurityException {
        // Looking for reference to a wsse:BinarySecurityToken or to a derived key
        // 1. look for a wsse:SecurityTokenReference element
        List secTokReferences = XmlUtil.findChildElementsByName(parentElement,
                                                                SoapUtil.SECURITY_URIS_ARRAY,
                                                                SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        if (!secTokReferences.isEmpty()) {
            // 2. Resolve the child reference
            Element securityTokenReference = (Element)secTokReferences.get(0);
            List references = XmlUtil.findChildElementsByName(securityTokenReference,
                                                              SoapUtil.SECURITY_URIS_ARRAY,
                                                              SoapUtil.REFERENCE_EL_NAME);
            List keyIdentifiers = XmlUtil.findChildElementsByName(securityTokenReference,
                                                                  SoapUtil.SECURITY_URIS_ARRAY,
                                                                  "KeyIdentifier");
            if (!references.isEmpty()) {
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
                final X509SigningSecurityTokenImpl token = null;
                if (token != null) {
                    if(logger.isLoggable(Level.FINEST)) logger.finest(MessageFormat.format("The keyInfo referred to a previously parsed Security Token ''{0}''", uriAttr));
                    return token;
                }

                final EncryptedKey ekToken = encryptedKeyById.get(uriAttr);
                if (ekToken != null) {
                    if(logger.isLoggable(Level.FINEST)) logger.finest(MessageFormat.format("The KeyInfo referred to a previously decrypted EncryptedKey ''{0}''", uriAttr));
                    return ekToken;
                }

                logger.fine("The reference " + uriAttr + " did not point to a X509Cert.");
            } else if (!keyIdentifiers.isEmpty()) {
                // TODO support multiple KeyIdentifier elements
                Element keyId = (Element)keyIdentifiers.get(0);
                String valueType = keyId.getAttribute("ValueType");
                String value = XmlUtil.getTextValue(keyId).trim();
                if (valueType != null && valueType.endsWith(SoapUtil.VALUETYPE_ENCRYPTED_KEY_SHA1_SUFFIX)) {
                    EncryptedKey found = resolveEncryptedKeyBySha1(value);
                    if (found != null) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("The KeyInfo referred to an already-known EncryptedKey token");
                        return found;
                    }
                } else if (valueType != null && valueType.endsWith(SoapUtil.VALUETYPE_X509_THUMB_SHA1_SUFFIX)) {
                    SigningSecurityToken token = x509TokensByThumbprint.get(value);
                    if (token != null) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("The KeyInfo referred to a previously used X.509 token.");
                        return token;
                    }

                    if (securityTokenResolver == null) {
                        logger.warning("The KeyInfo referred to a ThumbprintSHA1, but no SecurityTokenResolver is available");
                    } else {
                        X509Certificate foundCert = securityTokenResolver.lookup(value);
                        if (foundCert == null) {
                            logger.info("The KeyInfo referred to a ThumbprintSHA1, but we were unable to locate a matching cert");
                        } else {
                            if(logger.isLoggable(Level.FINEST))
                                logger.finest("The KeyInfo referred to a recognized X.509 certificate by its thumbprint: " + foundCert.getSubjectDN().getName());
                            token = new X509BinarySecurityTokenImpl(foundCert, keyId);
                            securityTokens.add(token);
                            x509TokensByThumbprint.put(value, token);
                            return token;
                        }
                    }
                } else if (valueType != null && valueType.endsWith(SoapUtil.VALUETYPE_SKI_SUFFIX)) {
                    SigningSecurityToken token = x509TokensBySki.get(value);
                    if (token != null) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("The KeyInfo referred to a previously used X.509 token.");
                        return token;
                    }

                    if (securityTokenResolver == null) {
                        logger.warning("The KeyInfo referred to a SKI, but no SecurityTokenResolver is available");
                    } else {
                        X509Certificate foundCert = securityTokenResolver.lookupBySki(value);
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
                            securityTokens.add(token);
                            x509TokensBySki.put(value, token);
                            return token;
                        }
                    }
                } else if (valueType != null && ArrayUtils.contains(SoapUtil.VALUETYPE_SAML_ASSERTIONID_ARRAY, valueType)) {
                    SigningSecurityToken token = null;
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

    private EncryptedKey resolveEncryptedKeyBySha1(String eksha1) throws InvalidDocumentFormatException, GeneralSecurityException {
        // We are trying to produce an EncryptedKey instance that matches this EncryptedKeySHA1 value.
        // If a SecurityTokenResolver exists and has already unwrapped a key with this EncryptedKeySHA1, then
        //    we'll reuse the already-unwrapped key.
        // If we have already seen an EncryptedKey in this request with a matching EncryptedKeySHA1, we'll return
        //    that token, after ensuring its key is unwrapped, reusing the cached key if possible.
        // If this request did not include a matching EncryptedKey, but we have a cached secret key matching
        //    this EncryptedKeySHA1, we'll create a new virtual EncryptedKey and add it to this request.

        EncryptedKeyCache resolver = securityTokenResolver;
        byte[] cachedSecretKey = resolver == null ? null : resolver.getSecretKeyByEncryptedKeySha1(eksha1);
        EncryptedKey found = findEncryptedKey(securityTokens, eksha1);

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
            found = WssProcessorUtil.makeEncryptedKey(releventSecurityHeader.getOwnerDocument(), cachedSecretKey, eksha1);
            securityTokens.add(found);
        } else if (cachedSecretKey != null && !found.isUnwrapped() && found instanceof EncryptedKeyImpl) {
            EncryptedKeyImpl eki = (EncryptedKeyImpl)found;
            eki.setSecretKey(cachedSecretKey);
        }

        return found;
    }

    // @return the token in tokes that is an EncryptedKey with the specified EncryptedKeySHA1, or null
    private static EncryptedKey findEncryptedKey(Collection<XmlSecurityToken> tokes, String eksha1) {
        for (SecurityToken token : tokes) {
            if (token instanceof EncryptedKey) {
                EncryptedKey ek = (EncryptedKey)token;
                if (eksha1.equals(ek.getEncryptedKeySHA1()))
                    return ek;
            }
        }
        return null;
    }

    class X509IssuerSerialOutput {
        SigningSecurityTokenImpl signingToken;
        X509Certificate signingCert;
    }

    public abstract class X509IssuerSerialSecurityToken extends SigningSecurityTokenImpl implements X509SecurityToken {
        X509IssuerSerialSecurityToken(Element e) {
            super(e);
        }
    }

    private X509IssuerSerialOutput handleX509Data(final Element str) throws InvalidDocumentFormatException {
        final Element x509data = XmlUtil.findFirstChildElementByName(str, DsigUtil.DIGSIG_URI, "X509Data");
        if (x509data == null) return null;

        Element issuerSerial = XmlUtil.findFirstChildElementByName(x509data, DsigUtil.DIGSIG_URI, "X509IssuerSerial");
        if (issuerSerial != null) {
            logger.fine("The signature refers to an X509IssuerSerial");
            // read ds:X509IssuerName and ds:X509SerialNumber
            Element X509IssuerNameEl = XmlUtil.findFirstChildElementByName(issuerSerial, DsigUtil.DIGSIG_URI, "X509IssuerName");
            Element X509SerialNumberEl = XmlUtil.findFirstChildElementByName(issuerSerial, DsigUtil.DIGSIG_URI, "X509SerialNumber");
            if (X509IssuerNameEl == null || X509SerialNumberEl == null) throw new InvalidDocumentFormatException("X509IssuerSerial was missing X509IssuerName and/or X509SerialNumber");

            String X509IssuerName = XmlUtil.getTextValue(X509IssuerNameEl);
            String X509SerialNumber = XmlUtil.getTextValue(X509SerialNumberEl);

            if (X509IssuerName == null || X509IssuerName.length() <= 0 || X509SerialNumber == null || X509SerialNumber.length() <= 0)
                throw new InvalidDocumentFormatException("X509IssuerName and/or X509SerialNumber was empty");

            if ( logger.isLoggable( Level.FINE ) ) {
                logger.log( Level.FINE, "Trying to lookup cert with Issuer DN '" + X509IssuerName + "' and serial '" + X509SerialNumber + "'");
            }

            final X500Principal issuer;
            try {
                issuer = new X500Principal(X509IssuerName);
            } catch ( IllegalArgumentException iae ) {
                logger.warning("Ignoring certificate reference with invalid issuer '"+X509IssuerName+"', serial number is '"+X509SerialNumber+"'.");
                return null;
            }
            final BigInteger serial;
            try {
                serial = new BigInteger(X509SerialNumber);
            } catch ( NumberFormatException nfe ) {
                logger.warning("Ignoring certificate reference with invalid serial number '"+X509SerialNumber+"', issuer is '"+X509IssuerName+"'.");
                return null;
            }
            final X509Certificate certificate = securityTokenResolver.lookupByIssuerAndSerial(issuer, serial);
            if (certificate == null) {
                logger.info("Could not find certificate for issuer '"+X509IssuerName+"', serial '"+X509SerialNumber+"'.");
                return null;
            }

            logger.fine("Certificate found");
            X509IssuerSerialOutput res = new X509IssuerSerialOutput();
            res.signingCert = certificate;
            res.signingToken = new X509IssuerSerialSecurityToken(issuerSerial) {
                public String getElementId() {
                    String id = null;
                    try {
                        id = SoapUtil.getElementWsuId(str);
                        if (id == null) {
                            id = SoapUtil.getElementWsuId(x509data);
                        }
                        return id;
                    } catch (InvalidDocumentFormatException e) {
                        throw new IllegalStateException(e); // shouldn't be possible at this point
                    }
                }

                public SecurityTokenType getType() {
                    return SecurityTokenType.X509_ISSUER_SERIAL;
                }

                public X509Certificate getCertificate() {
                    return certificate;
                }
            };
            securityTokens.add(res.signingToken);
            return res;
        }
        return null;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    private void processSignature(final Element sigElement,
                                  final SecurityContextFinder securityContextFinder)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, IOException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing Signature");

        // 1st, process the KeyInfo
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) {
            throw new ProcessorException("KeyInfo element not found in Signature Element");
        }

        X509Certificate signingCert = null;
        Key signingKey = null;
        // Try to resolve cert by reference
        SigningSecurityToken signingToken = resolveSigningTokenByRef(keyInfoElement);
        X509SigningSecurityTokenImpl signingCertToken = null;
        if (signingToken instanceof X509SigningSecurityTokenImpl)
            signingCertToken = (X509SigningSecurityTokenImpl)signingToken;

        if (signingCertToken != null) {
            signingCert = signingCertToken.getMessageSigningCertificate();
        } else {
            try {
                KeyInfo keyInfo = new KeyInfo(keyInfoElement);
                if(keyInfo.getX509Data() != null && keyInfo.getX509Data().length > 0) {
                    KeyInfo.X509Data x509Data = keyInfo.getX509Data()[0];
                    if(x509Data.getCertificates() != null && x509Data.getCertificates().length > 0) {
                        signingCert = x509Data.getCertificates()[0];
                    } else if(x509Data.getSKIs() != null && x509Data.getSKIs().length > 0) {
                        byte[] skiBytes = (byte[])x509Data.getSKIs()[0];
                        signingCert = securityTokenResolver.lookupBySki(HexUtils.encodeBase64(skiBytes, true));
                    }

                    if(signingCert != null) {
                        signingCertToken = new X509KeyInfoTokenImpl(signingCert, keyInfoElement);
                    }
                }
            } catch(XSignatureException xse) {
                throw new SignatureException(xse);
            }
        }

        // NOTE if we want to resolve embedded we need to set a signingCertToken as below
        // if (signingCert == null) { //try to resolve it as embedded
        //     signingCert = resolveEmbeddedCert(keyInfoElement);
        // }
        if (signingCert == null) { // last chance: see if we happen to recognize a SKI, perhaps because it is ours or theirs
            signingCert = resolveCertBySkiRef(keyInfoElement);
            if (signingCert != null) {
                // This dummy BST matches the required format for signing via an STR-Transform
                // for STR-Transform the prefix must match the one on the STR
                String wsseNs = releventSecurityHeader.getNamespaceURI();
                Element strEle = XmlUtil.findOnlyOneChildElementByName(keyInfoElement,
                                                                       SoapUtil.SECURITY_URIS_ARRAY,
                                                                       SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
                final String wssePrefix;
                if (strEle == null) {
                    wssePrefix = releventSecurityHeader.getPrefix();
                } else {
                    wssePrefix = strEle.getPrefix();
                }
                final Element bst;
                if (wssePrefix == null) {
                    bst = sigElement.getOwnerDocument().createElementNS(wsseNs, "BinarySecurityToken");
                    bst.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns", wsseNs);
                } else {
                    bst = sigElement.getOwnerDocument().createElementNS(wsseNs, wssePrefix+":BinarySecurityToken");
                    bst.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:"+wssePrefix, wsseNs);
                }
                bst.setAttribute("ValueType", SoapUtil.VALUETYPE_X509);
                XmlUtil.setTextContent(bst, HexUtils.encodeBase64(signingCert.getEncoded(), true));

                signingCertToken = new X509BinarySecurityTokenImpl(signingCert, bst);
                securityTokens.add(signingCertToken); // nasty, blah
            }
        }

        // Process any STR that is used within the signature
        Element keyInfoStr = XmlUtil.findFirstChildElementByName(keyInfoElement, SoapUtil.SECURITY_URIS_ARRAY, "SecurityTokenReference");
        if (keyInfoStr != null) {
            processSecurityTokenReference(keyInfoStr, securityContextFinder);

            X509IssuerSerialOutput tmp = handleX509Data(keyInfoStr);
            if (tmp != null) {
                signingCert = tmp.signingCert;
                signingToken = tmp.signingToken;
            }
        }

        if (signingCert != null) {
            signingKey = signingCert.getPublicKey();
        } else if (signingToken instanceof EncryptedKey) {
            signingKey = new SecretKeySpec(((SecretKeyToken)signingToken).getSecretKey(), "SHA1");
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

        if (signingCert != null && checkSigningCertValidity) {
            try {
                signingCert.checkValidity();
            } catch (CertificateExpiredException e) {
                logger.log(Level.WARNING, "Signing certificate expired " + signingCert.getNotAfter(), ExceptionUtils.getDebugException(e));
                throw new ProcessorException(e);
            } catch (CertificateNotYetValidException e) {
                logger.log(Level.WARNING, "Signing certificate is not valid until " + signingCert.getNotBefore(), ExceptionUtils.getDebugException(e));
                throw new ProcessorException(e);
            }
        }

        // Validate signature
        SignatureContext sigContext = new SignatureContext();
        MimeKnob mimeKnob = message.getKnob(MimeKnob.class);
        PartIterator iterator;
        iterator = mimeKnob == null ? null : mimeKnob.getParts();
        Map<String,PartInfo> partMap = new HashMap<String,PartInfo>();
        sigContext.setEntityResolver(new AttachmentEntityResolver(iterator, XmlUtil.getXss4jEntityResolver(), partMap, 0));
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                Element found = elementsByWsuId.get(s);
                if (found != null) {
                    // See if we need to remove any processed encrypted key elements.  We'll need to
                    // remove the encrypted key elements if this signature covers the Envelope or the
                    // Security header.
                    if (found == processedDocument.getDocumentElement() || found == releventSecurityHeader) {
                        // It's an enveloped signature, so remove any already-processed EncryptedKey elements
                        // before computing this hash
                        Set keys = getProcessedEncryptedKeys();
                        Iterator i = keys.iterator();
                        while (i.hasNext()) {
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

                try {
                    return SoapUtil.getElementByWsuId(doc, s);
                } catch (InvalidDocumentFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        sigContext.setAlgorithmFactory(new WssProcessorAlgorithmFactory(strToTarget));
        Validity validity = sigContext.verify(sigElement, signingKey);

        if (!validity.getCoreValidity()) {
            // if the signature does not match but an encrypted key was previously ignored,
            // it is likely that this is caused by the fact that decryption did not occur.
            // this is perfectly legal in wss passthrough mechanisms, we therefore ignores this
            // signature altogether
            boolean encryptionIgnored = false;
            if (encryptionIgnored) {
                logger.info("the validity of a signature could not be computed however an " +
                            "encryption element was previously ignored for passthrough " +
                            "purposes. this signature will therefore be ignored.");
                return;
            }
            StringBuilder msg = new StringBuilder("Signature not valid. " + validity.getSignedInfoMessage());
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

        validatedSignatureValues.add(XmlUtil.getTextValue(sigValueEl));

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
                elementCovered = elementsByWsuId.get(elementCoveredURI);
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
            } else if (signingToken instanceof SigningSecurityTokenImpl) {
                signingSecurityToken = signingToken;
            } else
                throw new RuntimeException("No signing security token found");

            // record for later
            if (elementCovered != null) {
                // check whether this is a token reference
                Element targetElement = (Element)strToTarget.get(elementCovered);
                if (targetElement != null) {
                    elementCovered = targetElement;
                }
                // make reference to this element
                final SignedElement signedElement = new SignedElementImpl(
                        signingSecurityToken,
                        elementCovered,
                        sigElement,
                        DsigUtil.findSigAlgorithm(sigElement),
                        DsigUtil.findDigestAlgorithms(sigElement));
                elementsThatWereSigned.add(signedElement);
                signingSecurityToken.addSignedElement(signedElement);
            } else {
                // make reference to this part
                final SignedPart signedPart = new SignedPartImpl(signingSecurityToken, partCovered);
                partsThatWereSigned.add(signedPart);
                signingSecurityToken.addSignedPart(signedPart);
            }
            signingSecurityToken.onPossessionProved();
        }
    }

    /**
     * Return a resolver that will find certs from already-seen X.509 tokens in this message by their wsu:Id.
     * @return a Resolver<String,X509Certificate> that will find certs from already-seen X.509 BSTs in this message processing context
     */
    private Resolver<String,X509Certificate> getMessageX509TokenResolver() {
        if (messageX509TokenResolver != null)
            return messageX509TokenResolver;
        messageX509TokenResolver = new Resolver<String,X509Certificate>() {
            public X509Certificate resolve(String id) {
                return null;
            }
        };
        return messageX509TokenResolver;
    }

    // @return the ReferenceList element that was processed and decrypted, or null if
    //         the encryptedKey was ignored (intended for a downstream recipient) or if this EncryptedKey did not contain
    //         a reference list.
    //
    // If the encrypted key was addressed to us, it will have been added to the context ProcessedEncryptedKeys set.
    private EncryptedKeyImpl processEncryptedKey(Element encryptedKeyElement)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing EncryptedKey");

        // If there's a KeyIdentifier, log whether it's talking about our key
        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
        try {
            KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyElement, securityTokenResolver, getMessageX509TokenResolver());
        } catch (UnexpectedKeyInfoException e) {
            return null;
        }

        // verify that the algo is supported
        lastKeyEncryptionAlgorithm = XencUtil.checkEncryptionMethod(encryptedKeyElement);

        final EncryptedKeyImpl ekTok;
        try {
            ekTok = new EncryptedKeyImpl(encryptedKeyElement, securityTokenResolver, getMessageX509TokenResolver());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error decrypting", e);
            throw new ProcessorException(e);
        }

        String wsuId = SoapUtil.getElementWsuId(encryptedKeyElement);
        if (wsuId != null) encryptedKeyById.put(wsuId, ekTok);

        securityTokens.add(ekTok);
        //addProcessedEncryptedKey(ekTok);
        return ekTok;
    }

    public EncryptedKeyImpl getEncryptedKey(Element encryptedContainerElement)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException
    {
        // Look inside the real EncryptedData element for the key
        NodeList elements = encryptedContainerElement.getElementsByTagNameNS(SoapConstants.XMLENC_NS, "EncryptedData");
        final Element encryptedDataElement;
        if(elements.getLength() > 0) {
            encryptedDataElement = (Element)elements.item(0);
        } else {
            return null; // Will not need a key since there isn't any encrypted data
        }
        elements = encryptedDataElement.getElementsByTagNameNS(SoapConstants.DIGSIG_URI, "KeyInfo");
        if(elements.getLength() > 0) {
            Element keyInfoElement = (Element)elements.item(0);
            elements = keyInfoElement.getElementsByTagNameNS(SoapConstants.XMLENC_NS, "EncryptedKey");
            if(elements.getLength() > 0) {
                Element encryptedKeyElement = (Element)elements.item(0);
                return processEncryptedKey(encryptedKeyElement);
            } else {
                return null;
            }
        } else {
            // Look for an EncryptedKey sibling
            for(Node sibling = encryptedDataElement.getNextSibling();sibling != null;sibling = sibling.getNextSibling()) {
                if(sibling.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)sibling;
                    if(el.getLocalName().equals("EncryptedKey") && SoapConstants.XMLENC_NS.equals(el.getNamespaceURI())) {
                        return processEncryptedKey(el);
                    }
                }
            }

            return null;
        }
    }

    public ProcessorResult decryptElement(Element encryptedDataElement)
            throws GeneralSecurityException, IOException, SAXException,
                   ProcessorException, InvalidDocumentFormatException
    {
        EncryptedKeyImpl ekTok = getEncryptedKey(encryptedDataElement);
        boolean removeEncryptedKey = ekTok.getElement().getParentNode() == encryptedDataElement;

        byte[] key = ekTok.getSecretKey();
        
        // Hacky for now -- we'll special case EncryptedHeader
        boolean wasEncryptedHeader = false;
        if ("EncryptedID".equals(encryptedDataElement.getLocalName()) || "EncryptedAssertion".equals(encryptedDataElement.getLocalName())) {
            encryptedDataElement = DomUtils.findFirstChildElementByName(encryptedDataElement,
                                                                       SoapConstants.XMLENC_NS,
                                                                       "EncryptedData");
            if (encryptedDataElement == null)
                throw new InvalidDocumentFormatException("EncryptedHeader did not contain EncryptedData");
            wasEncryptedHeader = true;
            isWsse11Seen = true;
        }

        Node parent = encryptedDataElement.getParentNode();
        if (parent == null || !(parent instanceof Element))
            throw new InvalidDocumentFormatException("Root of document is encrypted"); // sanity check, can't happen
        Element parentElement = (Element)encryptedDataElement.getParentNode();

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
        final DecryptionContext dc = new DecryptionContext();
        final List<String> algorithm = new ArrayList<String>();

        // Support "flexible" answers to getAlgorithm() query when using 3des with HSM (Bug #3705)
        final FlexKey flexKey = new FlexKey(key);


        // override getEncryptionEngine to collect the encryptionmethod algorithm
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn() {
            public EncryptionEngine getEncryptionEngine(EncryptionMethod encryptionMethod)
                    throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, StructureException {
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

        Element encMethod = XmlUtil.findFirstChildElementByName(encryptedDataElement, SoapUtil.XMLENC_NS, "EncryptionMethod");
        Element keyInfo = XmlUtil.findFirstChildElementByName(encryptedDataElement, SoapUtil.DIGSIG_URI, "KeyInfo");

        // TODO we won't know the actual cipher until the EncryptionMethod is created, so we'll hope that the Provider will be the same for all symmetric crypto
        Provider symmetricProvider = JceProvider.getInstance().getBlockCipherProvider();
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        dc.setAlgorithmFactory(af);
        dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT,
                            encMethod, keyInfo);
        dc.setKey(flexKey);
        NodeList dataList;
        try {
            // do the actual decryption
            dc.decrypt();
            dc.replace();
            // remember encrypted element
            dataList = dc.getDataAsNodeList();

            if(removeEncryptedKey) {
                ekTok.getElement().getParentNode().removeChild(ekTok.getElement());
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
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error decrypting", ioe);
            throw ioe;
        } catch (ParserConfigurationException pce) {
            logger.log(Level.WARNING, "Error decrytping", pce);
            return null;
        }
        // determine algorithm
        String algorithmName = XencAlgorithm.AES_128_CBC.getXEncName();
        if (!algorithm.isEmpty()) {
            if (algorithm.size() > 1)
                throw new ProcessorException("Multiple encryption algorithms found in element " + encryptedDataElement.getNodeName());
            algorithmName = algorithm.iterator().next();
        }

        // Now record the fact that some data was encrypted.
        // Did the parent element contain any non-attribute content other than this EncryptedData
        // (and possibly some whitespace before and after)?
        if (wasEncryptedHeader) {
            // If this was an EncryptedHeader, do the special-case transformation to restore the plaintext header
            Element newHeader = DomUtils.findOnlyOneChildElement(parentElement);
            Node newHeaderParent = parentElement.getParentNode();
            if (newHeaderParent == null || !(newHeaderParent instanceof Element))
                throw new InvalidDocumentFormatException("Root of document contained EncryptedHeader"); // sanity check, can't happen
            newHeaderParent.replaceChild(newHeader, parentElement); // promote decrypted header over top of EncryptedHeader
            logger.finer("All of encrypted header '" + newHeader.getLocalName() + "' was encrypted");
            elementsThatWereEncrypted.add(new EncryptedElementImpl(newHeader, algorithmName));
        } else if (onlyChild) {
            // All relevant content of the parent node was encrypted.
            logger.finer("All of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            elementsThatWereEncrypted.add(new EncryptedElementImpl(parentElement, algorithmName));
        } else {
            // There was unencrypted stuff mixed in with the EncryptedData, so we can only record elements as
            // encrypted that were actually wholly inside the EncryptedData.
            // TODO: In this situation, no note is taken of any encrypted non-Element nodes (such as text nodes)
            // This sucks, but at lesat this will err on the side of safety.
            logger.finer("Only some of element '" + parentElement.getLocalName() + "' non-attribute contents were encrypted");
            for (int i = 0; i < dataList.getLength(); i++) {
                Node node = dataList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    elementsThatWereEncrypted.add(new EncryptedElementImpl((Element)node, algorithmName));
                }
            }
        }

        return produceResult();
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
            try {
                return SoapUtil.getElementWsuId(asElement());
            } catch (InvalidDocumentFormatException e) {
                throw new IllegalStateException(e); // shouldn't be possible at this point
            }
        }

        public X509Certificate getMessageSigningCertificate() {
            return finalcert;
        }

        public String toString() {
            return "X509SecurityToken: " + finalcert.toString();
        }
    }

    private static class X509KeyInfoTokenImpl extends X509SigningSecurityTokenImpl implements X509SecurityToken {
        private final X509Certificate finalcert;

        public X509KeyInfoTokenImpl(X509Certificate finalcert, Element keyInfoElement) {
            super(keyInfoElement);
            this.finalcert = finalcert;
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.UNKNOWN;
        }

        public String getElementId() {
            return null;
        }

        public X509Certificate getMessageSigningCertificate() {
            return finalcert;
        }

        public String toString() {
            return "X509SecurityToken: " + finalcert.toString();
        }
    }

    private static class EncryptedKeyImpl extends SigningSecurityTokenImpl implements EncryptedKey {
        private final String elementWsuId;
        private final byte[] encryptedKeyBytes;
        private final SignerInfo signerInfo;
        private EncryptedKeyCache tokenResolver;
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
            StringBuilder sb = new StringBuilder("EncryptedKey: wsuId=");
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

        public SecurityContextTokenImpl(SecurityContext secContext, Element secConTokEl, String identifier) throws InvalidDocumentFormatException {
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
        private final Element signatureElement;
        private final String signatureAlgorithmId;
        private final String[] digestAlgorithmIds;

        public SignedElementImpl(SigningSecurityToken signingToken, Element element, Element signatureElement, String signatureAlgorithmId, String[] digestAlgorithmIds) {
            this.signingToken = signingToken;
            this.element = element;
            this.signatureElement = signatureElement;
            this.signatureAlgorithmId = signatureAlgorithmId;
            this.digestAlgorithmIds = digestAlgorithmIds;
        }

        public SigningSecurityToken getSigningSecurityToken() {
            return signingToken;
        }

        public Element getSignatureElement() {
            return signatureElement;
        }

        public String getSignatureAlgorithmId() {
            return signatureAlgorithmId;
        }

        public String[] getDigestAlgorithmIds() {
            return digestAlgorithmIds;
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