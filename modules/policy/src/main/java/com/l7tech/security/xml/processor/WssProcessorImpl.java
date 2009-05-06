package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.*;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.kerberos.KerberosConfigException;
import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.*;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.UnsupportedDocumentFormatException;
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
import java.security.cert.*;
import java.text.MessageFormat;
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
 */
@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
public class WssProcessorImpl implements WssProcessor {
    private static final Logger logger = Logger.getLogger(WssProcessorImpl.class.getName());

    static {
        JceProvider.init();
    }

    private final Message message;

    private final Collection<SignedElement> elementsThatWereSigned = new ArrayList<SignedElement>();
    private final Collection<EncryptedElement> elementsThatWereEncrypted = new ArrayList<EncryptedElement>();
    private final Collection<SignedPart> partsThatWereSigned = new ArrayList<SignedPart>();
    private final Collection<XmlSecurityToken> securityTokens = new ArrayList<XmlSecurityToken>();
    private final Collection<DerivedKeyToken> derivedKeyTokens = new ArrayList<DerivedKeyToken>();
    public final List<SignatureConfirmation> signatureConfirmationValues = new ArrayList<SignatureConfirmation>();
    public final List<String> validatedSignatureValues = new ArrayList<String>();

    private X509Certificate senderCertificate = null;
    private SecurityTokenResolver securityTokenResolver = null;
    private SecurityContextFinder securityContextFinder = null;
    private long signedAttachmentSizeLimit = 0;
    private boolean rejectOnMustUnderstand = true;

    private Document processedDocument;
    private Map<String,Element> elementsByWsuId = null;
    private TimestampImpl timestamp = null;
    private Element releventSecurityHeader = null;
    private Map<String,XmlSecurityToken> x509TokensById = new HashMap<String,XmlSecurityToken>();
    private Map<String,SigningSecurityToken> x509TokensByThumbprint = new HashMap<String,SigningSecurityToken>();
    private Map<String,SigningSecurityToken> x509TokensBySki = new HashMap<String,SigningSecurityToken>();
    private Map<Node,Node> strToTarget = new HashMap<Node,Node>();
    private Map<String,EncryptedKey> encryptedKeyById = new HashMap<String,EncryptedKey>();
    private Set<EncryptedKey> processedEncryptedKeys = null;
    private SecurityActor secHeaderActor;
    private boolean documentModified = false;
    private boolean encryptionIgnored = false;
    private String lastKeyEncryptionAlgorithm = null;
    private boolean isWsse11Seen = false;
    private boolean isDerivedKeySeen = false; // If we see any derived keys, we'll assume we can derive our own keys in reponse
    private Resolver<String,X509Certificate> messageX509TokenResolver = null;

    /**
     * Create a WssProcessorImpl context not bound to any message.
     * An unbound WssProcessorImpl cannot perform any operations except {@link #undecorateMessage}.
     */
    public WssProcessorImpl() {
        // TODO remove this constructor and the entire WssProcessor interface, then rename this class to WssProcessor
        this(null);
    }

    /**
     * Create a WssProcessorImpl context bound to the specified message.
     *
     * @param message the Message, providing access to attachments if needed to check signatures.  Required.
     */
    public WssProcessorImpl(Message message) {
        this.message = message;
    }

    public ProcessorResult undecorateMessage(Message message,
                                             X509Certificate senderCertificate,
                                             SecurityContextFinder securityContextFinder,
                                             SecurityTokenResolver securityTokenResolver)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, BadSecurityContextException, SAXException, IOException
    {
        final WssProcessorImpl context = new WssProcessorImpl(message);
        context.setSenderCertificate(senderCertificate);
        context.setSecurityContextFinder(securityContextFinder);
        context.setSecurityTokenResolver(securityTokenResolver);
        context.setSignedAttachmentSizeLimit(signedAttachmentSizeLimit);
        return context.processMessage();
    }

    /**
     * Set a limit on the maximum signed attachment size.
     *
     * @param size the limit in bytes
     */
    public void setSignedAttachmentSizeLimit(final long size) {
        signedAttachmentSizeLimit = size;
    }

    /**
     * @param senderCertificate    the sender's cert, if known, so that Signatures containing SKI KeyInfos can be matched up, or null to disable this feature.
     */
    public void setSenderCertificate(X509Certificate senderCertificate) {
        this.senderCertificate = senderCertificate;
    }

    /**
     * @param securityContextFinder a security context finder for looking up ws-sc sessions, or null to disable WS-SC support.
     */
    public void setSecurityContextFinder(SecurityContextFinder securityContextFinder) {
        this.securityContextFinder = securityContextFinder;
    }

    /**
     * @param securityTokenResolver   a resolver for looking up certificates in various ways, or null disable certificate reference support.
     */
    public void setSecurityTokenResolver(SecurityTokenResolver securityTokenResolver) {
        this.securityTokenResolver = securityTokenResolver;
    }

    /**
     * Control whether we will immediately fail when asked to process messages that have any SOAP header addressed
     * to us that we don't understand and that has mustUnderstand="1".  (See Bug #2157).
     * <p/>
     * A SOAP header is considered to be addressed to us if its role or actor is
     * either "SecureSpan" or "http://www.w3.org/2003/05/soap-envelope/role/next.html".
     * <p/>
     * When operating as a SOAP endpoint we should probably always reject such messages.  When operating as
     * an XML firewall things are not as clear cut but we should still reject such messages unless
     * configured not to.
     *
     * @param rejectOnMustUnderstand if true, we'll immediately fail if a message has mustUnderstand="1" on a SOAP header
     *        addressed to us that we do not understand.
     */
    public void setRejectOnMustUnderstand(boolean rejectOnMustUnderstand) {
        this.rejectOnMustUnderstand = rejectOnMustUnderstand;
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
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws InvalidDocumentFormatException if there is a problem with the document format that can't be ignored
     * @throws GeneralSecurityException if there is a problem with a key or certificate
     * @throws com.l7tech.security.xml.processor.ProcessorException in case of some other problem
     * @throws BadSecurityContextException if the message contains a WS-SecureConversation SecurityContextToken, but the securityContextFinder has no record of that session.
     * @throws SAXException if the first part's content type is not text/xml; or,
     *                      if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if there is a problem reading from or writing to a stash
     * @throws IllegalStateException if the Message has not yet been initialized
     * @throws UnexpectedKeyInfoException if the message has a KeyInfo that doesn't match the expected value.  Note that
     *                                    this must be caught before ProcessorException.
     */
    public ProcessorResult processMessage()
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
        elementsByWsuId = SoapUtil.getElementByWsuIdMap(processedDocument);

        String currentSoapNamespace = processedDocument.getDocumentElement().getNamespaceURI();

        // Resolve the relevent Security header
        Element l7secheader = SoapUtil.getSecurityElement(processedDocument, SecurityActor.L7ACTOR.getValue());
        if(l7secheader == null) l7secheader = SoapUtil.getSecurityElement(processedDocument, SecurityActor.L7ACTOR_URI.getValue());
        Element noactorsecheader = SoapUtil.getSecurityElement(processedDocument);
        if (l7secheader != null) {
            releventSecurityHeader = l7secheader;
            secHeaderActor = SecurityActor.L7ACTOR;
        } else {
            releventSecurityHeader = noactorsecheader;
            if (releventSecurityHeader != null) {
                secHeaderActor = SecurityActor.NOACTOR;
            }
        }

        if (rejectOnMustUnderstand)
            rejectIfHeadersNotUnderstood();

        // maybe there are no security headers at all in which case, there is nothing to process
        if (releventSecurityHeader == null) {
            logger.finer("No relevent security header found.");
            return produceResult();
        }

        // Process elements one by one
        Element securityChildToProcess = DomUtils.findFirstChildElement(releventSecurityHeader);
        while (securityChildToProcess != null) {
            Element removeRefList = null;

            if (securityChildToProcess.getLocalName().equals( SoapConstants.ENCRYPTEDKEY_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals( SoapConstants.XMLENC_NS)) {
                    // http://www.w3.org/2001/04/xmlenc#
                    // if this element is processed BEFORE the signature validation, it should be removed
                    // for the signature to validate properly
                    // fla added (but only if the ec was actually processed)
                    // lyonsm: we now only remove the reference list, and only if we decrypted it.
                    //         The signature check will take care of removing any processed encrypted keys as well,
                    //         but only if needed to validate an enveloped signature.
                    removeRefList = processEncryptedKey(securityChildToProcess);
                } else {
                    logger.finer("Encountered EncryptedKey element but not of right namespace (" +
                                 securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.TIMESTAMP_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.WSU_URIS_ARRAY)) {
                    processTimestamp(securityChildToProcess);
                } else {
                    logger.fine("Encountered Timestamp element but not of right namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.BINARYSECURITYTOKEN_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.SECURITY_URIS_ARRAY)) {
                    processBinarySecurityToken(securityChildToProcess);
                } else {
                    logger.fine("Encountered BinarySecurityToken element but not of right namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.SIGNATURE_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals( SoapConstants.DIGSIG_URI)) {
                    processSignature(securityChildToProcess, securityContextFinder);
                } else {
                    logger.fine("Encountered Signature element but not of right namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.USERNAME_TOK_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.SECURITY_URIS_ARRAY)) {
                    processUsernameToken(securityChildToProcess);
                } else {
                    logger.fine("Encountered UsernameToken element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.SECURITY_CONTEXT_TOK_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.WSSC_NAMESPACE_ARRAY)) {
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
                        securityTokens.add(secConTok);
                        logger.finest("SecurityContextToken (SecureConversation) added");
                    }
                } else {
                    logger.fine("Encountered SecurityContextToken element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.WSSC_DK_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.WSSC_NAMESPACE_ARRAY)) {
                    processDerivedKey(securityChildToProcess);
                } else {
                    logger.fine("Encountered DerivedKey element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.REFLIST_EL_NAME)) {
                // In the case of a Secure Conversation the reference list is declared outside
                // of the DerivedKeyToken
                if (securityChildToProcess.getNamespaceURI().equals( SoapConstants.XMLENC_NS)) {
                    processReferenceList(securityChildToProcess);
                } else {
                    logger.fine("Encountered ReferenceList element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals(SamlConstants.ELEMENT_ASSERTION)) {
                if (securityChildToProcess.getNamespaceURI().equals(SamlConstants.NS_SAML) ||
                    securityChildToProcess.getNamespaceURI().equals(SamlConstants.NS_SAML2)) {
                    processSamlSecurityToken(securityChildToProcess);
                } else {
                    logger.fine("Encountered SAML Assertion element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.SECURITYTOKENREFERENCE_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.SECURITY_URIS_ARRAY)) {
                    processSecurityTokenReference(securityChildToProcess, securityContextFinder, true);
                } else {
                    logger.fine("Encountered SecurityTokenReference element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals("SignatureConfirmation")) {
                if (DomUtils.elementInNamespace(securityChildToProcess, new String[] { SoapConstants.SECURITY11_NAMESPACE } )) {
                    processSignatureConfirmation(securityChildToProcess);
                } else {
                    logger.fine("Encountered SignatureConfirmation element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else {
                // Unhandled child elements of the Security Header
                String mu = securityChildToProcess.getAttributeNS(currentSoapNamespace,
                                                                  SoapConstants.MUSTUNDERSTAND_ATTR_NAME).trim();
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
                setDocumentModified();
                removeRefList.getParentNode().removeChild(removeRefList);
            }
            while (nextSibling != null && nextSibling.getNodeType() != Node.ELEMENT_NODE) {
                nextSibling = nextSibling.getNextSibling();
            }
            if (nextSibling != null && nextSibling.getNodeType() == Node.ELEMENT_NODE) {
                securityChildToProcess = (Element)nextSibling;
            } else securityChildToProcess = null;
        }

        // Backward compatibility - if we didn't see a timestamp in the Security header, check for one up in the SOAP Header
        Element header = (Element)releventSecurityHeader.getParentNode();
        if (timestamp == null) {
            // (header can't be null or we wouldn't be here)
            Element ts = DomUtils.findFirstChildElementByName(header,
                                                             SoapConstants.WSU_URIS_ARRAY,
                                                             SoapConstants.TIMESTAMP_EL_NAME);
            if (ts != null)
                processTimestamp(ts);
        }

        // NOTE fla, we used to remove the Security header altogether but we now leave this up to the policy
        // NOTE lyonsm we don't remove the mustUnderstand attr here anymore either, since it changes the request
        // possibly-needlessly

        // If our work has left behind an empty SOAP Header, remove it too
        Element soapHeader = (Element)releventSecurityHeader.getParentNode();
        if (DomUtils.elementIsEmpty(soapHeader)) {
            setDocumentModified(); // no worries -- empty sec header can only mean we made at least 1 change already
            soapHeader.getParentNode().removeChild(soapHeader);
        }

        return produceResult();
    }

    private static class MustUnderstandException extends RuntimeException {
        final Element element;

        private MustUnderstandException(Element element) {
            this(element, null);
        }

        public MustUnderstandException(Element element, Exception e) {
            super("mustUnderstand: " + element.getNodeName(), e);
            this.element = element;
        }
    }

    /**
     * Scans for SOAP headers addressed to us with mustUnderstand=1 that we don't understand.
     * @throws InvalidDocumentFormatException if there is at least one SOAP header addressed to us
     *                                        with mustUnderstand=1 that we don't understand
     */
    private void rejectIfHeadersNotUnderstood() throws InvalidDocumentFormatException {
        Element header = SoapUtil.getHeaderElement(processedDocument);
        if (header == null)
            return;

        try {
            DomUtils.visitChildElements(header, new Functions.UnaryVoid<Element>() {
                public void call(Element element) {
                    if (element == releventSecurityHeader)
                        return;
                    try {
                        // Do we understand it?
                        if ( SoapConstants.TIMESTAMP_EL_NAME.equals(element.getLocalName()))
                            return;
                        // TODO Do we recognize WS-Addressing headers addressed to us?

                        // Is it mustUnderstand = 1 or mustUnderstand = true?
                        String mustUnderstand = SoapUtil.getMustUnderstandAttributeValue(element);
                        if (mustUnderstand == null || "0".equals(mustUnderstand) || "false".equals(mustUnderstand))
                            return;

                        // Is it addressed to us?
                        String actor = SoapUtil.getActorValue(element);
                        if ("SecureSpan".equals(actor) || SoapConstants.ACTOR_VALUE_NEXT.equals(actor))
                            throw new MustUnderstandException(element);

                        // For role we can use a strict match since, for SOAP 1.2, they specified the role attr's namespace
                        String role = element.getAttributeNS("http://www.w3.org/2003/05/soap-envelope", "role");
                        if ("SecureSpan".equals(role) || SoapConstants.ROLE_VALUE_NEXT.equals(role))
                            throw new MustUnderstandException(element);

                    } catch (InvalidDocumentFormatException e) {
                        throw new MustUnderstandException(element, e);
                    }
                }
            });
        } catch (MustUnderstandException e) {
            throw new InvalidDocumentFormatException("Header addressed to us with mustUnderstand: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void addProcessedEncryptedKey(EncryptedKey ek) {
        getProcessedEncryptedKeys().add(ek);
    }

    private Set<EncryptedKey> getProcessedEncryptedKeys() {
        if (processedEncryptedKeys == null) processedEncryptedKeys = new HashSet<EncryptedKey>();
        //noinspection ReturnOfCollectionOrArrayField
        return processedEncryptedKeys;
    }

    /**
     * Call this before modifying processedDocument in any way.  This will upgrade the document to writable,
     * which will set various flags inside the Message
     * (for reserializing the document later, and possibly building a new TarariMessageContext), and will
     * possibly cause a copy of the current document to be cloned and saved.
     */
    private void setDocumentModified() {
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
    private Resolver<String,X509Certificate> getMessageX509TokenResolver() {
        if (messageX509TokenResolver != null)
            return messageX509TokenResolver;
        messageX509TokenResolver = new Resolver<String,X509Certificate>() {
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
        return messageX509TokenResolver;
    }

    private void processSignatureConfirmation(Element securityChildToProcess) {
        isWsse11Seen = true;
        String value = securityChildToProcess.getAttribute("Value");
        if (value == null || value.length() < 1) {
            logger.fine("Ignoring empty SignatureConfirmation header");
            return;
        }
        signatureConfirmationValues.add(new SignatureConfirmationImpl(securityChildToProcess, value));
    }

    private XmlSecurityToken findSecurityContextTokenBySessionId(String refUri) {
        Collection tokens = securityTokens;
        for (Object o : tokens) {
            if (o instanceof SecurityContextToken) {
                SecurityContextToken token = (SecurityContextToken)o;
                if (refUri.equals(token.getContextIdentifier()))
                    return token;
            }
        }
        return null;
    }

    // @return a new DerivedKeyToken.  Never null.
    private static DerivedKeyToken deriveKeyFromKerberosToken(Element derivedKeyEl, KerberosSecurityToken kst)
            throws InvalidDocumentFormatException
    {
        assert derivedKeyEl != null;
        assert kst != null;
        try {
            SecureConversationKeyDeriver keyDeriver = new SecureConversationKeyDeriver();
            final byte[] resultingKey = keyDeriver.derivedKeyTokenToKey(derivedKeyEl,
                                                                        kst.getTicket().getServiceTicket().getKey());
            return new DerivedKeyTokenImpl(derivedKeyEl, resultingKey, kst);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    /**
     * Locate an already-seen XmlSecurityToken in the specified context, by searching for the specified URI reference.
     *
     * @param refUri  the URI reference, including initial "#" character.  Must not be null or non-empty.
     * @return the matching already-seen XmlSecurityToken, or null if none was found.
     * @throws com.l7tech.util.InvalidDocumentFormatException if the URI reference is empty or does not begin with a hash mark
     */
    private XmlSecurityToken findXmlSecurityTokenById(String refUri)
            throws InvalidDocumentFormatException
    {
        if (!refUri.startsWith("#"))
            throw new InvalidDocumentFormatException("SecurityTokenReference URI does not start with '#'");
        if (refUri.length() < 2)
            throw new InvalidDocumentFormatException("SecurityTokenReference URI is too short");
        refUri = refUri.substring(1);

        for (Object securityToken : securityTokens) {
            SecurityToken token = (SecurityToken)securityToken;
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

    private void processTimestamp(final Element timestampElement)
            throws InvalidDocumentFormatException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing Timestamp");
        if (timestamp != null)
            throw new InvalidDocumentFormatException("More than one Timestamp element was encountered in the Security header");

        final Element created = DomUtils.findOnlyOneChildElementByName(timestampElement,
                                                                      SoapConstants.WSU_URIS_ARRAY,
                                                                      SoapConstants.CREATED_EL_NAME);
        final Element expires = DomUtils.findOnlyOneChildElementByName(timestampElement,
                                                                      SoapConstants.WSU_URIS_ARRAY,
                                                                      SoapConstants.EXPIRES_EL_NAME);

        final TimestampDate createdTimestampDate;
        final TimestampDate expiresTimestampDate;
        try {
            createdTimestampDate = created == null ? null : new TimestampDate(created);
            expiresTimestampDate = expires == null ? null : new TimestampDate(expires);
        } catch (ParseException e) {
            throw new InvalidDocumentFormatException("Unable to parse Timestamp", e);
        }

        timestamp = new TimestampImpl(createdTimestampDate, expiresTimestampDate, timestampElement);
    }

    // TODO merge this into KeyInfoElement class somehow
    private DerivedKeyTokenImpl resolveDerivedKeyByRef(final Element parentElement) {

        // Looking for reference to a a derived key token
        // 1. look for a wsse:SecurityTokenReference element
        List secTokReferences = DomUtils.findChildElementsByName(parentElement,
                                                                SoapConstants.SECURITY_URIS_ARRAY,
                                                                SoapConstants.SECURITYTOKENREFERENCE_EL_NAME);
        if (!secTokReferences.isEmpty()) {
            // 2. Resolve the child reference
            Element securityTokenReference = (Element)secTokReferences.get(0);
            List references = DomUtils.findChildElementsByName(securityTokenReference,
                                                              SoapConstants.SECURITY_URIS_ARRAY,
                                                              SoapConstants.REFERENCE_EL_NAME);
            if (references.isEmpty()) {
                logger.finest("SecurityTokenReference does not contain any References");
                return null;
            }

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
            for (DerivedKeyToken maybeDerivedKey : derivedKeyTokens) {
                if (maybeDerivedKey instanceof DerivedKeyTokenImpl) {
                    if (maybeDerivedKey.getElementId().equals(uriAttr)) {
                        return (DerivedKeyTokenImpl)maybeDerivedKey;
                    }
                }
            }
        }
        return null;
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
        ProcessorResult processorResult = new ProcessorResult() {
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
                            if ("Id".equals(n.getLocalName()) &&
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
                return secHeaderActor;
            }

            public List<String> getValidatedSignatureValues() {
                return validatedSignatureValues;
            }

            public List<SignatureConfirmation> getSignatureConfirmationValues() {
                return signatureConfirmationValues;
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
        if (timestamp != null) {
            Element timeElement = timestamp.asElement();
            SigningSecurityToken[] signingTokens = processorResult.getSigningTokens(timeElement);
            if (signingTokens.length == 1) {
                timestamp.setSigned();
            } else if (signingTokens.length > 1) {
                throw new IllegalStateException("More then one signing token over Timestamp detected!");
            }
        }
        return processorResult;
    }

    /**
     * Process a free-floating SecurityTokenReference. different mechanisms for referencing security tokens using the
     * <wsse:SecurityTokenReference> exist, and currently the supported are:
     * <ul>
     * <li> Key Identifier <wsse:KeyIdentifier></li>
     * <li> Security Token Reference <wsse:Reference></li>
     * <li> X509 issuer name and issuer serial <ds:X509IssuerName>,  <ds:X509SerialNumber></li>
     * </ul>
     * The unsupported SecurityTokeReference types are:
     * <ul>
     * <li> Embedded token <wsse:Embedded></li>
     * <li> Key Name <ds:KeyName></li>
     * </ul>
     * This is as per <i>Web Services Security: SOAP Message Security 1.0 (WS-Security 2004) OASIS standard</i>
     * TODO this should be merged into KeyInfoElement
     *
     * @param str  the SecurityTokenReference element
     * @param securityContextFinder the context finder to perform lookups with (may be null)
     * @param logIfNothingFound
     * @throws com.l7tech.util.InvalidDocumentFormatException if STR is invalid format or points at something unsupported
     * @throws com.l7tech.security.xml.processor.ProcessorException if a securityContextFinder is required to resolve this STR, but one was not provided
     */
    private void processSecurityTokenReference(Element str, SecurityContextFinder securityContextFinder, boolean logIfNothingFound)
            throws InvalidDocumentFormatException, ProcessorException
    {
        // Get identifier
        final String id = SoapUtil.getElementWsuId(str);
        final String logId = id == null ? "<noid>" : id;

        // Reference or KeyIdentifier values
        boolean isKeyIdentifier = false;
        String value;
        String valueType;
        String encodingType = null;

        Element keyIdentifierElement = DomUtils.findFirstChildElementByName(str, str.getNamespaceURI(), "KeyIdentifier");
        Element referenceElement = DomUtils.findFirstChildElementByName(str, str.getNamespaceURI(), "Reference");
        if (keyIdentifierElement != null) {
            isKeyIdentifier = true;
            value = DomUtils.getTextValue(keyIdentifierElement).trim();
            valueType = keyIdentifierElement.getAttribute("ValueType");
            encodingType = keyIdentifierElement.getAttribute("EncodingType");
        } else if (referenceElement != null) {
            value = referenceElement.getAttribute("URI");
            if (value != null && value.length() == 0) {
                value = null; // we want null not empty string for missing URI
            }
            if (value != null && value.charAt(0) == '#') {
                value = value.substring(1);
            }
            valueType = referenceElement.getAttribute("ValueType");
        } else {
            if (logIfNothingFound)
                logger.warning(MessageFormat.format("Ignoring SecurityTokenReference ID={0} with no KeyIdentifier or Reference", logId));
            return;
        }

        if (value == null) {
            String msg = "Rejecting SecurityTokenReference ID=" + logId
                         + " as the target Reference ID/KeyIdentifier is missing or could not be determined.";
            logger.warning(msg);
            throw new InvalidDocumentFormatException(msg);
        }

        // Process KeyIdentifier or Reference
        if (SoapUtil.isValueTypeX509v3(valueType) && !isKeyIdentifier) {
            if (encodingType != null && encodingType.length() > 0) {
                logger.warning("Ignoring SecurityTokenReference ID='" + logId
                               + "' with non-empty KeyIdentifier/@EncodingType='" + encodingType + "'.");
                return;
            }
            Element target = elementsByWsuId.get(value);
            if (target == null
                || (!target.getLocalName().equals("BinarySecurityToken") && !target.getLocalName().equals("Assertion"))
                || !ArrayUtils.contains( SoapConstants.SECURITY_URIS_ARRAY, target.getNamespaceURI())
                || !SoapUtil.isValueTypeX509v3(target.getAttribute("ValueType"))) {
                String msg = "Rejecting SecurityTokenReference ID='" + logId + "' with ValueType of '" + valueType +
                             "' because its target is either missing or not a BinarySecurityToken";
                logger.warning(msg);
                throw new InvalidDocumentFormatException(msg);
            }
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Remembering SecurityTokenReference ID=" + logId + " pointing at X.509 BST " + value);
            strToTarget.put(str, target);
        } else if (SoapUtil.isValueTypeSaml(valueType)) {
            if (encodingType != null && encodingType.length() > 0) {
                logger.warning("Ignoring SecurityTokenReference ID='" + logId
                               + "' with non-empty KeyIdentifier/@EncodingType='" + encodingType + "'.");
                return;
            }
            Element target = elementsByWsuId.get(value);
            if (target == null
                || !target.getLocalName().equals("Assertion")
                || (!target.getNamespaceURI().equals(SamlConstants.NS_SAML) &&
                    !target.getNamespaceURI().equals(SamlConstants.NS_SAML2))) {
                String msg = "Rejecting SecurityTokenReference ID='" + logId + "' with ValueType of '" + valueType +
                             "' because its target is either missing or not a SAML assertion";
                logger.warning(msg); // TODO remove redundant logging after debugging complete
                throw new InvalidDocumentFormatException(msg);
            }
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Remembering SecurityTokenReference ID=" + logId + " pointing at SAML assertion " + value);
            strToTarget.put(str, target);
        } else if (SoapUtil.isValueTypeKerberos(valueType) && isKeyIdentifier) {
            if (encodingType == null || !encodingType.equals( SoapConstants.ENCODINGTYPE_BASE64BINARY)) {
                logger.warning("Ignoring SecurityTokenReference ID=" + logId +
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
            if (logIfNothingFound)
                logger.warning("Ignoring SecurityTokenReference ID=" + logId + " with ValueType of " + valueType);
        }
    }

    private void processDerivedKey(Element derivedKeyEl)
            throws InvalidDocumentFormatException, ProcessorException, GeneralSecurityException {
        // get corresponding shared secret reference wsse:SecurityTokenReference
        Element sTokrefEl = DomUtils.findFirstChildElementByName(derivedKeyEl,
                                                                SoapConstants.SECURITY_URIS_ARRAY,
                                                                SoapConstants.SECURITYTOKENREFERENCE_EL_NAME);
        if (sTokrefEl == null) throw new InvalidDocumentFormatException("DerivedKeyToken should " +
                                                                        "contain a SecurityTokenReference");
        Element refEl = DomUtils.findFirstChildElementByName(sTokrefEl,
                                                            SoapConstants.SECURITY_URIS_ARRAY,
                                                            SoapConstants.REFERENCE_EL_NAME);

        final XmlSecurityToken derivationSource;
        final String ref;
        if (refEl == null) {
            // Check for an EncryptedKeySHA1 reference
            Element keyIdEl = DomUtils.findFirstChildElementByName(sTokrefEl,
                                                                  SoapConstants.SECURITY_URIS_ARRAY,
                                                                  "KeyIdentifier");
            ref = DomUtils.getTextValue(keyIdEl);

            String valueType = keyIdEl.getAttribute("ValueType");
            if (valueType == null)
                throw new InvalidDocumentFormatException("DerivedKey SecurityTokenReference KeyIdentifier has no ValueType");

            if ( SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1.equals(valueType)) {
                derivationSource = resolveEncryptedKeyBySha1(ref);

            } else if ( SoapConstants.VALUETYPE_KERBEROS_APREQ_SHA1.equals(valueType)) {
                if (securityTokenResolver == null)
                    throw new ProcessorException("Unable to process DerivedKeyToken - it references a Kerberosv5APREQSHA1, but no security token resolver is available");
                XmlSecurityToken xst = securityTokenResolver.getKerberosTokenBySha1(ref);

                if(xst==null) {
                    xst = findSecurityContextTokenBySessionId( KerberosUtils.getSessionIdentifier(ref));
                }

                derivationSource = xst;
            } else
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to unsupported ValueType " + valueType);

        } else {
            ref = refEl.getAttribute("URI");
            if (ref == null || ref.length() < 1)
                throw new InvalidDocumentFormatException("DerivedKeyToken's SecurityTokenReference lacks URI parameter");
            if (ref.startsWith("#"))
                derivationSource = findXmlSecurityTokenById(ref);
            else
                derivationSource = findSecurityContextTokenBySessionId(ref);
        }

        if(derivationSource==null) {
            logger.info("Invalid DerivedKeyToken reference target '" + ref + "', ignoring this derived key.");
            return;
        }

        if (derivationSource instanceof SecurityContextTokenImpl) {
            derivedKeyTokens.add(deriveKeyFromSecurityContext(derivedKeyEl,
                                                              (SecurityContextToken)derivationSource));
            // We won't count this as having seen a derived key, since WS-SC has always used them, and older SSBs
            // won't understand them if we try to use them for a non-WS-SC response
        } else if (derivationSource instanceof EncryptedKey) {
            derivedKeyTokens.add(deriveKeyFromEncryptedKey(derivedKeyEl,
                                                           (EncryptedKey)derivationSource));
            isDerivedKeySeen = true;
        } else if (derivationSource instanceof KerberosSecurityToken) {
            derivedKeyTokens.add(deriveKeyFromKerberosToken(derivedKeyEl, (KerberosSecurityToken)derivationSource));
            isDerivedKeySeen = true;
        } else
            logger.info("Unsupported DerivedKeyToken reference target '" + derivationSource.getType() + "', ignoring this derived key.");
    }

    // @return a new DerivedKeyToken.  Never null.
    private static DerivedKeyToken deriveKeyFromEncryptedKey(Element derivedKeyEl, EncryptedKey ek) throws InvalidDocumentFormatException, GeneralSecurityException {
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
    private static DerivedKeyToken deriveKeyFromSecurityContext(Element derivedKeyEl, SecurityContextToken sct) throws InvalidDocumentFormatException {
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

    private static String extractIdentifierFromSecConTokElement(Element secConTokEl) {
        // look for the wssc:Identifier child
        Element id = DomUtils.findFirstChildElementByName(secConTokEl,
                                                         SoapConstants.WSSC_NAMESPACE_ARRAY,
                                                         SoapConstants.WSSC_ID_EL_NAME);
        if (id == null) return null;
        return DomUtils.getTextValue(id);
    }

    private void processUsernameToken(final Element usernameTokenElement)
            throws InvalidDocumentFormatException
    {
        final UsernameTokenImpl rememberedSecToken;
        try {
            rememberedSecToken = new UsernameTokenImpl(usernameTokenElement);
            securityTokens.add(rememberedSecToken);
        } catch (UnsupportedDocumentFormatException e) {
            // if the format is not supported, we should ignore it completly
            logger.log(Level.INFO, "A usernametoken element was encountered but we dont support the format.", e);
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
     *                if the message contains an encryptedkey
     * @throws java.security.GeneralSecurityException
     *                                  if there was a problem with a key or crypto provider
     * @throws javax.xml.parsers.ParserConfigurationException
     *                                  if there was a problem with the XML parser
     * @throws java.io.IOException              if there was an IO error while reading the document or a key
     * @throws org.xml.sax.SAXException if there was a problem parsing the document
     * @throws com.l7tech.util.InvalidDocumentFormatException  if there is a problem decrypting an element due to message format
     * @throws com.l7tech.security.xml.processor.ProcessorException  if an encrypted data element cannot be resolved
     */
    private void decryptReferencedElements(byte[] key, Element refList)
            throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
                   ProcessorException, InvalidDocumentFormatException
    {
        List<Element> dataRefEls = DomUtils.findChildElementsByName(refList, SoapConstants.XMLENC_NS, SoapConstants.DATAREF_EL_NAME);
        if (dataRefEls == null || dataRefEls.isEmpty()) {
            logger.warning("EncryptedData is present, but contains at least one empty ReferenceList");
            return;
        }

        for (Element dataRefEl : dataRefEls) {
            String dataRefUri = dataRefEl.getAttribute( SoapConstants.REFERENCE_URI_ATTR_NAME);
            if (dataRefUri.startsWith("#"))
                dataRefUri = dataRefUri.substring(1);
            Element encryptedDataElement = elementsByWsuId.get(dataRefUri);
            if (encryptedDataElement == null) // TODO can omit this second search if encrypted sections never overlap
                encryptedDataElement = SoapUtil.getElementByWsuId(refList.getOwnerDocument(), dataRefUri);
            if (encryptedDataElement == null) {
                String msg = "cannot resolve encrypted data element " + dataRefUri;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }

            decryptElement(encryptedDataElement, key);
        }
    }

    private void decryptElement(Element encryptedDataElement, byte[] key)
            throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
                   ProcessorException, InvalidDocumentFormatException
    {
        // Hacky for now -- we'll special case EncryptedHeader
        boolean wasEncryptedHeader = false;
        if ("EncryptedHeader".equals(encryptedDataElement.getLocalName())) {
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
        setDocumentModified();
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
    }

    private void processBinarySecurityToken(final Element binarySecurityTokenElement)
            throws ProcessorException, GeneralSecurityException
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

        String value = DomUtils.getTextValue(binarySecurityTokenElement);
        if (value == null || value.length() < 1) {
            String msg = "The " + binarySecurityTokenElement.getLocalName() + " does not contain a value.";
            logger.warning(msg);
            throw new ProcessorException(msg);
        }

        final byte[] decodedValue; // must strip whitespace or base64 decoder misbehaves
        decodedValue = HexUtils.decodeBase64(value, true);

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
            securityTokens.add(rememberedSecToken);
            x509TokensById.put(wsuId, rememberedSecToken);
        }
        else {
            try {
                securityTokens.add(new KerberosSecurityTokenImpl(new KerberosGSSAPReqTicket(decodedValue),
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

    // TODO centralize this KeyInfo processing into the KeyInfoElement class somehow
    private SigningSecurityToken resolveSigningTokenByRef(final Element parentElement) throws InvalidDocumentFormatException, GeneralSecurityException {
        // Looking for reference to a wsse:BinarySecurityToken or to a derived key
        // 1. look for a wsse:SecurityTokenReference element
        List secTokReferences = DomUtils.findChildElementsByName(parentElement,
                                                                SoapConstants.SECURITY_URIS_ARRAY,
                                                                SoapConstants.SECURITYTOKENREFERENCE_EL_NAME);
        if (!secTokReferences.isEmpty()) {
            // 2. Resolve the child reference
            final Element securityTokenReference = (Element)secTokReferences.get(0);
            final List<Element> references = DomUtils.findChildElementsByName(securityTokenReference,
                                                              SoapConstants.SECURITY_URIS_ARRAY,
                                                              SoapConstants.REFERENCE_EL_NAME);
            final List<Element> keyIdentifiers = DomUtils.findChildElementsByName(securityTokenReference,
                                                                  SoapConstants.SECURITY_URIS_ARRAY,
                                                                  "KeyIdentifier");
            final List<Element> x509datas = DomUtils.findChildElementsByName(securityTokenReference,
                                                                             SoapConstants.DIGSIG_URI,
                                                                             "X509Data");
            if (!references.isEmpty()) {
                // get the URI
                Element reference = references.get(0);
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
                final X509SigningSecurityTokenImpl token = (X509SigningSecurityTokenImpl)x509TokensById.get(uriAttr);
                if (token != null) {
                    if(logger.isLoggable(Level.FINEST)) logger.finest(MessageFormat.format("The keyInfo referred to a previously parsed Security Token ''{0}''", uriAttr));
                    if (!strToTarget.containsKey(securityTokenReference))
                        strToTarget.put(securityTokenReference, token.asElement());
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
                Element keyId = keyIdentifiers.get(0);
                String valueType = keyId.getAttribute("ValueType");
                String value = DomUtils.getTextValue(keyId).trim();
                if (valueType != null && valueType.endsWith( SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1_SUFFIX)) {
                    EncryptedKey found = resolveEncryptedKeyBySha1(value);
                    if (found != null) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.finest("The KeyInfo referred to an already-known EncryptedKey token");
                        return found;
                    }
                } else if (valueType != null && valueType.endsWith( SoapConstants.VALUETYPE_X509_THUMB_SHA1_SUFFIX)) {
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
                } else if (valueType != null && valueType.endsWith( SoapConstants.VALUETYPE_SKI_SUFFIX)) {
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
                            token = X509BinarySecurityTokenImpl.createBinarySecurityToken(parentElement.getOwnerDocument(), foundCert, securityTokenReference.getPrefix(), securityTokenReference.getNamespaceURI());
                            securityTokens.add(token);
                            if (!strToTarget.containsKey(securityTokenReference))
                                strToTarget.put(securityTokenReference, token.asElement());
                            x509TokensBySki.put(value, token);
                            return token;
                        }
                    }
                } else if (valueType != null && ArrayUtils.contains( SoapConstants.VALUETYPE_SAML_ASSERTIONID_ARRAY, valueType)) {
                    SigningSecurityToken token = (SigningSecurityToken) x509TokensById.get(value);
                    if (!(token instanceof SamlAssertion)) {
                        if(logger.isLoggable(Level.INFO))
                            logger.log(Level.INFO, "The KeyInfo referred to an unknown SAML token ''{0}''.", value);
                    }
                    return token;
                } else {
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("The KeyInfo used an unsupported KeyIdentifier ValueType: " + valueType);
                }
            } else if (!x509datas.isEmpty()) {
                return handleX509Data(securityTokenReference);
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

        SecurityTokenResolver resolver = securityTokenResolver;
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

    private X509IssuerSerialSecurityToken handleX509Data(final Element str) throws InvalidDocumentFormatException, GeneralSecurityException {
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
            final X509IssuerSerialSecurityToken issuerSerialSecurityToken = new X509IssuerSerialSecurityToken(x509data, certificate);
            securityTokens.add(issuerSerialSecurityToken);
            strToTarget.put( str, X509BinarySecurityTokenImpl.createBinarySecurityToken(str.getOwnerDocument(), certificate, str.getPrefix(), str.getNamespaceURI()).asElement() );
            return issuerSerialSecurityToken;
        }
        return null;
    }

    private void processReferenceList(Element referenceListEl) throws ProcessorException, InvalidDocumentFormatException {
        // get each element one by one
        List<Element> dataRefEls = DomUtils.findChildElementsByName(referenceListEl, SoapConstants.XMLENC_NS, SoapConstants.DATAREF_EL_NAME);
        if (dataRefEls == null || dataRefEls.isEmpty()) {
            logger.warning("ReferenceList is present, but is empty");
            return;
        }

        for (Element dataRefEl : dataRefEls) {
            String dataRefUri = dataRefEl.getAttribute( SoapConstants.REFERENCE_URI_ATTR_NAME);
            if (dataRefUri.startsWith("#")) dataRefUri = dataRefUri.substring(1);
            Element encryptedDataElement = elementsByWsuId.get(dataRefUri);
            if (encryptedDataElement == null)
                encryptedDataElement = SoapUtil.getElementByWsuId(referenceListEl.getOwnerDocument(), dataRefUri);
            if (encryptedDataElement == null) {
                String msg = "cannot resolve encrypted data element " + dataRefUri;
                logger.warning(msg);
                throw new ProcessorException(msg);
            }
            // get the reference to the derived key token from the http://www.w3.org/2000/09/xmldsig#:KeyInfo element
            Element keyInfo = DomUtils.findFirstChildElementByName(encryptedDataElement, SoapConstants.DIGSIG_URI, SoapConstants.KINFO_EL_NAME);
            if (keyInfo == null) {
                throw new InvalidDocumentFormatException("The DataReference here should contain a KeyInfo child");
            }
            SecretKeyToken dktok = resolveDerivedKeyByRef(keyInfo);
            try {
                if (dktok == null) {
                    SigningSecurityToken tok = resolveSigningTokenByRef(keyInfo);
                    if (tok instanceof EncryptedKey) {
                        dktok = (SecretKeyToken)tok;
                    } else {
                        // there are some keyinfo formats that we do not support. in that case, we should see if
                        // the message can possibly just passthrough
                        logger.info("The DataReference's KeyInfo did not refer to a DerivedKey or previously-known EncryptedKey." +
                                    "This element will not be decrypted.");
                        encryptionIgnored = true;
                        return;
                    }
                }
                decryptElement(encryptedDataElement, dktok.getSecretKey());
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

    // @return the ReferenceList element that was processed and decrypted, or null if
    //         the encryptedKey was ignored (intended for a downstream recipient) or if this EncryptedKey did not contain
    //         a reference list.
    //
    // If the encrypted key was addressed to us, it will have been added to the context ProcessedEncryptedKeys set.
    private Element processEncryptedKey(Element encryptedKeyElement)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing EncryptedKey");

        // If there's a KeyIdentifier, log whether it's talking about our key
        // Check that this is for us by checking the ds:KeyInfo/wsse:SecurityTokenReference/wsse:KeyIdentifier
        try {
            KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyElement, securityTokenResolver, getMessageX509TokenResolver());
        } catch (UnexpectedKeyInfoException e) {
            if (secHeaderActor == SecurityActor.L7ACTOR) {
                logger.warning("We do not appear to be the intended recipient for this EncryptedKey however the " +
                               "security header is clearly addressed to us");
                throw e;
            } else if (secHeaderActor == SecurityActor.NOACTOR) {
                logger.log(Level.INFO, "We do not appear to be the intended recipient for this " +
                                       "EncryptedKey. Will leave it alone since the security header is not " +
                                       "explicitly addressed to us.", e);
                encryptionIgnored = true;
                return null;
            }
        }

        // verify that the algo is supported
        lastKeyEncryptionAlgorithm = XencUtil.checkEncryptionMethod(encryptedKeyElement);

        // We got the key. Get the list of elements to decrypt.
        Element refList = DomUtils.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                SoapConstants.XMLENC_NS,
                                                                SoapConstants.REFLIST_EL_NAME);

        final EncryptedKeyImpl ekTok;
        try {
            ekTok = new EncryptedKeyImpl(encryptedKeyElement, securityTokenResolver, getMessageX509TokenResolver());
            if (refList != null)
                decryptReferencedElements(ekTok.getSecretKey(), refList);
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
        if (wsuId != null) encryptedKeyById.put(wsuId, ekTok);

        securityTokens.add(ekTok);
        addProcessedEncryptedKey(ekTok);
        return refList;
    }

    private void processSamlSecurityToken(final Element securityTokenElement)
            throws InvalidDocumentFormatException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing saml:Assertion XML SecurityToken");
        try {
            final SamlAssertion samlToken = SamlAssertion.newInstance(securityTokenElement, securityTokenResolver);
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
                securityTokens.add(samlSignatureToken);

                final SignedElement signedElement = new SignedElement() {
                    public SigningSecurityToken getSigningSecurityToken() {
                        return samlSignatureToken;
                    }

                    public Element asElement() {
                        return securityTokenElement;
                    }
                };

                elementsThatWereSigned.add(signedElement);
            }

            // Add the assertion itself
            securityTokens.add(samlToken);
            x509TokensById.put(samlToken.getElementId(), samlToken);
        } catch (SAXException e) {
            throw new InvalidDocumentFormatException(e);
        } catch (SignatureException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    private void processSignature(final Element sigElement,
                                  final SecurityContextFinder securityContextFinder)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, IOException
    {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing Signature");

        // 1st, process the KeyInfo
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null)
            throw new InvalidDocumentFormatException("KeyInfo element not found in Signature Element");

        X509Certificate signingCert = null;
        Key signingKey = null;
        // Try to find ref to derived key
        final DerivedKeyTokenImpl dkt = resolveDerivedKeyByRef(keyInfoElement);
        // Try to resolve cert by reference
        SigningSecurityToken signingToken = resolveSigningTokenByRef(keyInfoElement);
        X509SigningSecurityTokenImpl signingCertToken = null;
        if (signingToken instanceof X509SigningSecurityTokenImpl)
            signingCertToken = (X509SigningSecurityTokenImpl)signingToken;

        if (signingCertToken != null) {
            signingCert = signingCertToken.getMessageSigningCertificate();
        }
        if (signingCert == null) { //try to resolve it as embedded
             signingCert = KeyInfoElement.decodeEmbeddedCert(keyInfoElement);
            if (signingCert != null) {
                signingCertToken = createDummyBst(sigElement.getOwnerDocument(), signingCert);
                securityTokens.add(signingCertToken);
            }
        }
        if (signingCert == null) { // last chance: see if we happen to recognize a SKI, perhaps because it is ours or theirs
            signingCert = resolveCertBySkiRef(keyInfoElement);
            if (signingCert != null) {
                signingCertToken = createDummyBst(sigElement.getOwnerDocument(), signingCert);
                securityTokens.add(signingCertToken); // nasty, blah
            }
        }

        // Process any STR that is used within the signature
        Element keyInfoStr = DomUtils.findFirstChildElementByName(keyInfoElement, SoapConstants.SECURITY_URIS_ARRAY, "SecurityTokenReference");
        if (keyInfoStr != null) {
            processSecurityTokenReference(keyInfoStr, securityContextFinder, false);
        }

        if (signingCert == null && dkt != null) {
            signingKey = new SecretKeySpec(dkt.getComputedDerivedKey(), "SHA1");
        } else if (signingCert != null) {
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

        if (signingCert != null) {
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
        MimeKnob mimeKnob = (MimeKnob) message.getKnob(MimeKnob.class);
        PartIterator iterator;
        iterator = mimeKnob == null ? null : mimeKnob.getParts();
        Map<String,PartInfo> partMap = new HashMap<String,PartInfo>();
        sigContext.setEntityResolver(new AttachmentEntityResolver(iterator, XmlUtil.getXss4jEntityResolver(), partMap, signedAttachmentSizeLimit));
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

                return SoapUtil.getElementByWsuId(doc, s);
            }
        });
        sigContext.setAlgorithmFactory(new WssProcessorAlgorithmFactory(strToTarget));
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.verifyXml, signingCert, signingKey);
        Validity validity = sigContext.verify(sigElement, signingKey);

        if (!validity.getCoreValidity()) {
            // if the signature does not match but an encrypted key was previously ignored,
            // it is likely that this is caused by the fact that decryption did not occur.
            // this is perfectly legal in wss passthrough mechanisms, we therefore ignores this
            // signature altogether
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
        Element sigValueEl = DomUtils.findOnlyOneChildElementByName(sigElement, sigElement.getNamespaceURI(), "SignatureValue");
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
            } else if (dkt != null) {
                // If signed by a derived key token, credit the signature to the derivation source instead of the DKT
                XmlSecurityToken token = dkt.getSourceToken();
                if (token instanceof SigningSecurityTokenImpl) {
                    signingSecurityToken = (SigningSecurityToken)dkt.getSourceToken();
                } else {
                    throw new InvalidDocumentFormatException("Unable to record signature using unsupport key derivation source: " + token.getType());
                }
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
                final SignedElement signedElement = new SignedElementImpl(signingSecurityToken, elementCovered);
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
     * Create a fake BinarySecurityToken to hold an X.509 token that arrived with the message by means
     * other than a BinarySecurityToken (SKI reference, or embedded cert),
     * where there is no other security token available to point at.
     *
     * @param domFactory  document for creating new DOM nodes.  Required.
     * @param certificate  certificate this BST should contain.  Required.
     * @return an X509SigningSecurityTokenImpl containing the specified certificate.  Never null.
     * @throws java.security.cert.CertificateEncodingException if the certificate cannot be encoded.
     */
    private X509SigningSecurityTokenImpl createDummyBst(Document domFactory, X509Certificate certificate) throws CertificateEncodingException {
        X509SigningSecurityTokenImpl signingCertToken;
        // This dummy BST matches the required format for signing via an STR-Transform
        // for STR-Transform the prefix must match the one on the STR
        String wsseNs = releventSecurityHeader.getNamespaceURI();
        String wssePrefix = releventSecurityHeader.getPrefix();
        final Element bst;
        if (wssePrefix == null) {
            bst = domFactory.createElementNS(wsseNs, "BinarySecurityToken");
            bst.setAttributeNS(DomUtils.XMLNS_NS, "xmlns", wsseNs);
        } else {
            bst = domFactory.createElementNS(wsseNs, wssePrefix+":BinarySecurityToken");
            bst.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:"+wssePrefix, wsseNs);
        }
        bst.setAttribute("ValueType", SoapConstants.VALUETYPE_X509);
        DomUtils.setTextContent(bst, HexUtils.encodeBase64(certificate.getEncoded(), true));
        signingCertToken = new X509BinarySecurityTokenImpl(certificate, bst);
        return signingCertToken;
    }

    private static class TimestampDate extends ParsedElementImpl implements WssTimestampDate {
        Date date;
        String dateString;

        TimestampDate(Element createdOrExpiresElement) throws ParseException {
            super(createdOrExpiresElement);
            dateString = DomUtils.getTextValue(createdOrExpiresElement);
            date = ISO8601Date.parse(dateString);
        }

        public long asTime() {
            return date.getTime();
        }

        public String asIsoString() {
            return dateString;
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

        public com.l7tech.security.xml.processor.WssTimestampDate getCreated() {
            return createdTimestampDate;
        }

        public com.l7tech.security.xml.processor.WssTimestampDate getExpires() {
            return expiresTimestampDate;
        }

        public boolean isSigned() {
            return signed;
        }

        void setSigned() {
            signed = true;
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
