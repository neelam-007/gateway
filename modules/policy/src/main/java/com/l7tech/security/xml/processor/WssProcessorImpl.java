package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.KeyInfo;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.type.EncryptedData;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.kerberos.*;
import com.l7tech.message.*;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory;
import com.l7tech.security.xml.decorator.WssDecoratorUtils;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.UnsupportedDocumentFormatException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
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
    private final SignatureConfirmation signatureConfirmation = new SignatureConfirmationImpl();
    public final List<String> validatedSignatureValues = new ArrayList<String>();

    // WARNING : Settings must be copied in undecorateMessage
    private SecurityTokenResolver securityTokenResolver = null;
    private WrappedSecurityContextFinder securityContextFinder = new WrappedSecurityContextFinder(null);
    private long signedAttachmentSizeLimit = 0;
    private boolean rejectOnMustUnderstand = true;
    private boolean permitMultipleTimestampSignatures = false;
    private boolean permitUnknownBinarySecurityTokens = false;
    private boolean strictSignatureConfirmationValidation = true;
    private boolean contextUsesWss11 = false;
    private IdAttributeConfig idAttributeConfig = SoapUtil.getDefaultIdAttributeConfig();
    // WARNING : Settings must be copied in undecorateMessage

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
    private String secHeaderActorUri;
    private boolean documentModified = false;
    private boolean encryptionIgnored = false;
    private String lastKeyEncryptionAlgorithm = null;
    private boolean isWsse11Seen = false;
    private boolean isDerivedKeySeen = false; // If we see any derived keys, we'll assume we can derive our own keys in reponse

    private WssProcessorErrorHandler errorHandler = null; // optional error handler invoked when certain failures occur (for now only for failed decryption attempts)

    /**
     * Create a WssProcessorImpl context not bound to any message.
     * An unbound WssProcessorImpl cannot perform any operations except {@link WssProcessor#undecorateMessage(com.l7tech.message.Message, SecurityContextFinder, com.l7tech.security.xml.SecurityTokenResolver)}.
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

    @Override
    public ProcessorResult undecorateMessage(Message message,
                                             SecurityContextFinder securityContextFinder,
                                             SecurityTokenResolver securityTokenResolver)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, BadSecurityContextException, SAXException, IOException
    {
        final WssProcessorImpl context = new WssProcessorImpl(message);
        context.setSecurityContextFinder(securityContextFinder);
        context.setSecurityTokenResolver(context.contextual( securityTokenResolver ));
        context.setSignedAttachmentSizeLimit(signedAttachmentSizeLimit);
        context.setPermitMultipleTimestampSignatures(permitMultipleTimestampSignatures);
        context.setPermitUnknownBinarySecurityTokens(permitUnknownBinarySecurityTokens);
        context.setRejectOnMustUnderstand(rejectOnMustUnderstand);
        context.setStrictSignatureConfirmationValidation(strictSignatureConfirmationValidation);
        context.setContextUsesWss11(contextUsesWss11);
        context.setIdAttributeConfig(idAttributeConfig);
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
     * @param securityContextFinder a security context finder for looking up ws-sc sessions, or null to disable WS-SC support.
     */
    public void setSecurityContextFinder(SecurityContextFinder securityContextFinder) {
        this.securityContextFinder = new WrappedSecurityContextFinder(securityContextFinder);
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
     * Set whether multiple signatures over the timestamp are permitted.
     *
     * <p>This setting is "false" by default, but should be set to "true" if
     * messages are expected to contain multiple signatures that cover the
     * timestamp.</p>
     *
     * @param permitMultipleTimestampSignatures True to permit multiple signatures covering the timestamp.
     */
    public void setPermitMultipleTimestampSignatures(boolean permitMultipleTimestampSignatures) {
        this.permitMultipleTimestampSignatures = permitMultipleTimestampSignatures;
    }

    /**
     * Set whether unknown BSTs are permitted.
     *
     * <p>This setting is "false" by default so unknown BSTs are not permitted.</p>
     *
     * @param permitUnknownBinarySecurityTokens True to permit unknown BSTs.
     */
    public void setPermitUnknownBinarySecurityTokens(boolean permitUnknownBinarySecurityTokens) {
        this.permitUnknownBinarySecurityTokens = permitUnknownBinarySecurityTokens;
    }

    /**
     * Flag for controlling how signature confirmation validation is performed.
     *
     * When set to true:
     * all WSS 1.1 signature confirmation checks are performed
     * all checks are also performed on responses that are detected as using WSS 1.1 (not just on the ones that correspond to requests marked as requiring WSS 1.1)
     *
     * When set to false, the following conditions will not cause validation to fail:
     * no SignatureConfirmation element in a WSS 1.1 response
     * SignatureConfirmation element with no Value attribute is not the only SignatureConfirmation element
     * signature confirmation values that are not found in the request
     * unencrypted signature confirmations corresponding to encrypted signatures in the request
     *
     * @return true if the processor should perform strict signature confirmation validation, false otherwise
     */
    public boolean isStrictSignatureConfirmationValidation() {
        return strictSignatureConfirmationValidation;
    }

    public void setStrictSignatureConfirmationValidation(boolean strictSignatureConfirmationValidation) {
        this.strictSignatureConfirmationValidation = strictSignatureConfirmationValidation;
    }

    /**
     * Flag for passing the global WSS 1.1 configuration from the context.
     *
     * @return true if the use of WSS 1.1 is configured globally at the context level, false otherwise
     */
    public boolean isContextUsesWss11() {
        return contextUsesWss11;
    }

    public void setContextUsesWss11(boolean contextUsesWss11) {
        this.contextUsesWss11 = contextUsesWss11;
    }

    /**
     * @return the current strategy for recognizing ID attributes (e.g. for Signature references).
     */
    public IdAttributeConfig getIdAttributeConfig() {
        return idAttributeConfig;
    }

    /**
     * @param idAttributeConfig  the new strategy for recognizing ID attributes (e.g. for Signature references).
     */
    public void setIdAttributeConfig(IdAttributeConfig idAttributeConfig) {
        this.idAttributeConfig = idAttributeConfig;
    }

    /**
     * @return current error handler, or null if one is not set.
     */
    public WssProcessorErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * @param errorHandler error handler to invoke when certain errors occur, or null.
     */
    public void setErrorHandler(@Nullable WssProcessorErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
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
        elementsByWsuId = DomUtils.getElementByIdMap(processedDocument, idAttributeConfig);

        String currentSoapNamespace = processedDocument.getDocumentElement().getNamespaceURI();

        // Resolve the relevent Security header
        Element l7secheader = SoapUtil.getSecurityElementForL7(processedDocument);
        if (l7secheader != null) {
            releventSecurityHeader = l7secheader;
            secHeaderActor = SecurityActor.L7ACTOR;
        } else {
            releventSecurityHeader = SoapUtil.getSecurityElement(processedDocument);
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
        } else {
            secHeaderActorUri = SoapUtil.getActorValue(releventSecurityHeader);
        }

        // Process basic elements first
        processTokensAndReferences();

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
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.BINARYSECURITYTOKEN_EL_NAME) ||
                       securityChildToProcess.getLocalName().equals(SamlConstants.ELEMENT_ASSERTION)) {
                // now pre-processed, see processTokensAndReferences()
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.SIGNATURE_EL_NAME)) {
                if (securityChildToProcess.getNamespaceURI().equals( SoapConstants.DIGSIG_URI)) {
                    processSignature(securityChildToProcess);
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
                        final SecurityContext secContext = securityContextFinder.getSecurityContext("SecurityContextToken", identifier);
                        if (secContext == null) {
                            throw new BadSecurityContextException(identifier, securityChildToProcess.getNamespaceURI());
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
            } else if (securityChildToProcess.getLocalName().equals( SoapConstants.SECURITYTOKENREFERENCE_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.SECURITY_URIS_ARRAY)) {
                    processSecurityTokenReference(securityChildToProcess, true, true);
                } else {
                    logger.fine("Encountered SecurityTokenReference element but not of expected namespace (" +
                                securityChildToProcess.getNamespaceURI() + ')');
                }
            } else if (securityChildToProcess.getLocalName().equals("SignatureConfirmation")) {
                if (DomUtils.elementInNamespace(securityChildToProcess, new String[] { SoapConstants.SECURITY11_NAMESPACE } )) {
                    isWsse11Seen = true;
                    signatureConfirmation.addConfirmationElement(securityChildToProcess, strictSignatureConfirmationValidation);
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
                    throw new ProcessorValidationException(msg);
                } else {
                    logger.finer("Unknown element in security header: " + securityChildToProcess.getNodeName());
                }
            }

            if (removeRefList != null) {
                setDocumentModified();
                removeRefList.getParentNode().removeChild(removeRefList);
            }

            securityChildToProcess = DomUtils.findNextElementSibling( securityChildToProcess );
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

        validateSignatureConfirmations();

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

    private void processTokensAndReferences() throws ProcessorException, GeneralSecurityException, InvalidDocumentFormatException {
        Element securityChildToProcess = DomUtils.findFirstChildElement(releventSecurityHeader);
        while (securityChildToProcess != null) {
            if (securityChildToProcess.getLocalName().equals( SoapConstants.BINARYSECURITYTOKEN_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.SECURITY_URIS_ARRAY)) {
                    processBinarySecurityToken(securityChildToProcess);
                } else {
                    logger.fine("Encountered BinarySecurityToken element but not of right namespace (" +
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
            }

            securityChildToProcess = DomUtils.findNextElementSibling( securityChildToProcess );
        }

        securityChildToProcess = DomUtils.findFirstChildElement(releventSecurityHeader);
        while (securityChildToProcess != null) {
            if (securityChildToProcess.getLocalName().equals( SoapConstants.SECURITYTOKENREFERENCE_EL_NAME)) {
                if (DomUtils.elementInNamespace(securityChildToProcess, SoapConstants.SECURITY_URIS_ARRAY)) {
                    processSecurityTokenReference(securityChildToProcess, false, false);
                }
            }

            securityChildToProcess = DomUtils.findNextElementSibling( securityChildToProcess );
        }
    }

    private static class MustUnderstandException extends RuntimeException {
        final Element element;

        private MustUnderstandException(Element element) {
            this(element, null);
        }

        private MustUnderstandException(Element element, Exception e) {
            super("mustUnderstand: " + element.getNodeName(), e);
            this.element = element;
        }
    }

    /**
     * Scans for SOAP headers addressed to us with mustUnderstand=1 that we don't understand.
     * @throws ProcessorException if there is at least one SOAP header addressed to us
     *                                        with mustUnderstand=1 that we don't understand
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Header element
     */
    private void rejectIfHeadersNotUnderstood() throws InvalidDocumentFormatException, ProcessorException {
        Element header = SoapUtil.getHeaderElement(processedDocument);
        if (header == null)
            return;

        try {
            DomUtils.visitChildElements(header, new Functions.UnaryVoid<Element>() {
                @Override
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
                        if ( SoapConstants.ACTOR_VALUE_NEXT.equals(actor) ||
                             SoapConstants.ROLE_VALUE_NEXT.equals(actor) ||
                             SoapUtil.isElementForL7(element) )
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
            throw new ProcessorValidationException("Header addressed to us with mustUnderstand: " + ExceptionUtils.getMessage(e));
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
     * @return a ContextualSecurityTokenResolver that will find certs from already-seen X.509 BSTs in this message processing context
     */
    public ContextualSecurityTokenResolver contextual( final SecurityTokenResolver securityTokenResolver ) {
        return new ContextualSecurityTokenResolver.Support.DelegatingContextualSecurityTokenResolver(
                securityTokenResolver == null ? new SimpleSecurityTokenResolver() : securityTokenResolver
        ) {
            @Override
            public X509Certificate lookupByIdentifier( final String identifier ) {
                X509Certificate resolved = null;
                Object token = x509TokensById.get(identifier);
                if (token instanceof X509BinarySecurityTokenImpl) {
                    X509BinarySecurityTokenImpl bst = (X509BinarySecurityTokenImpl) token;
                    resolved = bst.getCertificate();
                }
                return resolved;
            }
        };
    }

    private void validateSignatureConfirmations() {

        Message relatedRequest = message.getRelated(MessageRole.REQUEST);
        SecurityKnob requestSK = relatedRequest == null ? null : relatedRequest.getSecurityKnob();
        Map<String, Boolean> reqSignatures = WssDecoratorUtils.getSignaturesDecorated(requestSK, secHeaderActorUri);

        if (reqSignatures == null) { // we're ok only if the message is using WSS 1.0 as far as we can tell
            if (isWsse11Seen) {
                signatureConfirmation.addError("Message has WSS 1.1 elements, but no decorations applied to the related request; signature validation will fail if enforced.");
            }
        } else { // there is a decorated request to validate against : inbound response validation
            boolean configuredWss11;
            boolean usesWss11;
            if (requestSK.getPolicyWssVersion() != null) {
                // explicitly configured WSS version overrides all the other flags
                configuredWss11 = requestSK.getPolicyWssVersion() == WsSecurityVersion.WSS11;
                usesWss11 = configuredWss11;
            } else {
                // fall-back to auto-detect if WSS 1.1 is not explicitly configured
                configuredWss11 = contextUsesWss11;
                usesWss11 = configuredWss11 || isWsse11Seen;
            }

            switch (signatureConfirmation.getConfirmationElements().size()) {
                case 0:
                    if (usesWss11) {
                        if (strictSignatureConfirmationValidation)
                            signatureConfirmation.addError("No SignatureConfirmation element found, expected at least one in a WSS 1.1 message.");
                        else if (configuredWss11 && !reqSignatures.isEmpty()) // don't fail if not strict and WSS 1.1 not configured
                            signatureConfirmation.addError("No signatures from the outbound request are confirmed.");
                    }
                    break;

                case 1:
                    // isWsse11Seen is true at this point
                    if (signatureConfirmation.hasNullValue() && !reqSignatures.isEmpty() &&
                        (strictSignatureConfirmationValidation || configuredWss11))
                        signatureConfirmation.addError("No signatures from the request are confirmed.");
                    // no break, the one non-null-value signature confirmation needs to be validated

                default:
                    if (usesWss11 || strictSignatureConfirmationValidation) {
                        Set<String> unverifiedConfirmations = signatureConfirmation.getConfirmationElements().keySet();
                        unverifiedConfirmations.remove(null);// value-less entry already processed

                        List<String> extras = new ArrayList<String>();
                        List<String> missingOrNotSigned = new ArrayList<String>();
                        List<String> unEncrypted = new ArrayList<String>();

                        for (String maybeConfirmedValue : unverifiedConfirmations) {
                            if (!reqSignatures.containsKey(maybeConfirmedValue))
                                extras.add(maybeConfirmedValue);
                        }

                        for (String requestSig : reqSignatures.keySet()) {
                            Set<SignedElement> signed;
                            if (!unverifiedConfirmations.contains(requestSig)) {
                                missingOrNotSigned.add(requestSig);
                            } else {
                                signed = getSignedElements(signatureConfirmation.getElement(requestSig));
                                if (signed.isEmpty())
                                    missingOrNotSigned.add(requestSig);
                                else
                                    signatureConfirmation.addSignedElement(requestSig, signed);

                                if (reqSignatures.get(requestSig) && !wasEncrypted(signatureConfirmation.getElement(requestSig)))
                                    unEncrypted.add(requestSig);
                            }
                        }

                        if (!missingOrNotSigned.isEmpty())// main validation check
                            signatureConfirmation.addError("Request signatures not confirmed: " + Arrays.toString(missingOrNotSigned.toArray()));

                        if (strictSignatureConfirmationValidation) {
                            if (!extras.isEmpty())
                                signatureConfirmation.addError("Unknown SignatureConfirmations in response: " + Arrays.toString(extras.toArray()));
                            if (!unEncrypted.isEmpty())
                                signatureConfirmation.addError("Encrypted signatures in request not encrypted in response: " + Arrays.toString(unEncrypted.toArray()));
                        }
                    }
            } // switch
        }
    }

    private Set<SignedElement> getSignedElements(Element e) {
        Set<SignedElement> result = new HashSet<SignedElement>();
        for(SignedElement signed : elementsThatWereSigned) {
            if (signed.asElement().equals(e))
                result.add(signed);
        }
        return result;
    }

    private boolean wasEncrypted(Element e) {
        for(EncryptedElement encrypted : elementsThatWereEncrypted) {
            if (encrypted.asElement().equals(e))
                return true;
        }
        return false;
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

    private KerberosSigningSecurityToken findKerberosSigningSecurityTokenBySha1( final String sha1,
                                                                                 final Element securityTokenReference ) throws ProcessorException {
        KerberosSigningSecurityToken kerberosSigningSecurityToken = null;

        for ( XmlSecurityToken xmlSecurityToken : securityTokens ) {
            if ( xmlSecurityToken instanceof KerberosSigningSecurityToken ) {
                KerberosSigningSecurityToken token = (KerberosSigningSecurityToken)xmlSecurityToken;
                if ( sha1.equals(KerberosUtils.getBase64Sha1(token.getTicket())) ) {
                    kerberosSigningSecurityToken = token;
                    break;
                }
            }
        }

        if ( kerberosSigningSecurityToken == null ) {
            String identifier = KerberosUtils.getSessionIdentifier(sha1);
            SecurityContext secContext = securityContextFinder.getSecurityContext("Kerberos KeyIdentifier", identifier);
            if ( secContext != null && secContext.getSecurityToken() instanceof KerberosSigningSecurityToken ) {
                KerberosSigningSecurityToken sessionToken = (KerberosSigningSecurityToken) secContext.getSecurityToken();
                kerberosSigningSecurityToken = KerberosSigningSecurityTokenImpl.createBinarySecurityToken(
                        securityTokenReference.getOwnerDocument(),
                        sessionToken.getServiceTicket(),
                        securityTokenReference.getPrefix(),
                        securityTokenReference.getNamespaceURI() );
                securityTokens.add( kerberosSigningSecurityToken );
            }
        }

        if ( kerberosSigningSecurityToken != null ) {
            strToTarget.put( securityTokenReference, kerberosSigningSecurityToken.asElement() );
        }

        return kerberosSigningSecurityToken;
    }

    // @return a new DerivedKeyToken.  Never null.
    private static DerivedKeyToken deriveKeyFromKerberosToken(Element derivedKeyEl, KerberosSigningSecurityToken kst)
            throws InvalidDocumentFormatException
    {
        assert derivedKeyEl != null;
        assert kst != null;
        return deriveKeyFromToken( derivedKeyEl, kst.getServiceTicket().getKey(), kst );
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

    private SamlAssertion findSamlSecurityTokenByAssertionId(String assertionId) throws InvalidDocumentFormatException {
        if (assertionId.length() < 1)
            throw new InvalidDocumentFormatException("Reference to empty SAML assertion ID");

        for (XmlSecurityToken token : securityTokens) {
            if (token instanceof SamlAssertion) {
                SamlAssertion samlAssertion = (SamlAssertion) token;
                if (assertionId.equals(samlAssertion.getAssertionId()))
                    return samlAssertion;
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

    private ProcessorResult produceResult() throws ProcessorException {
        ProcessorResult processorResult = new ProcessorResult() {
            @Override
            public SignedElement[] getElementsThatWereSigned() {
                return elementsThatWereSigned.toArray(new SignedElement[elementsThatWereSigned.size()]);
            }

            @Override
            public EncryptedElement[] getElementsThatWereEncrypted() {
                return elementsThatWereEncrypted.toArray(new EncryptedElement[elementsThatWereEncrypted.size()]);
            }

            @Override
            public SignedPart[] getPartsThatWereSigned() {
                return partsThatWereSigned.toArray(new SignedPart[partsThatWereSigned.size()]);
            }

            @Override
            public XmlSecurityToken[] getXmlSecurityTokens() {
                return securityTokens.toArray(new XmlSecurityToken[securityTokens.size()]);
            }

            @Override
            public WssTimestamp getTimestamp() {
                return timestamp;
            }

            @Override
            public String getSecurityNS() {
                if (releventSecurityHeader != null) {
                    return releventSecurityHeader.getNamespaceURI();
                }
                return null;
            }

            @Override
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

            @Override
            public String getWsscNS() {
                if ( !derivedKeyTokens.isEmpty() ) {
                    return derivedKeyTokens.iterator().next().asElement().getNamespaceURI();
                }
                return null;
            }

            @Override
            public SecurityActor getProcessedActor() {
                return secHeaderActor;
            }

            @Override
            public String getProcessedActorUri() {
                return secHeaderActorUri;
            }

            @Override
            public List<String> getValidatedSignatureValues() {
                return validatedSignatureValues;
            }

            @Override
            public SignatureConfirmation getSignatureConfirmation() {
                return signatureConfirmation;
            }

            @Override
            public String getLastKeyEncryptionAlgorithm() {
                return lastKeyEncryptionAlgorithm;
            }

            @Override
            public boolean isWsse11Seen() {
                return isWsse11Seen;
            }

            @Override
            public boolean isDerivedKeySeen() {
                return isDerivedKeySeen;
            }

            /**
             * @param element the element to find the signing tokens for
             * @return the array if tokens that signed the element or empty array if none
             */
            @Override
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
                            if (element == signedElement.asElement()) {
                                tokens.add(signingSecurityToken);
                            }
                        }
                    }
                }
                return tokens.toArray(new SigningSecurityToken[tokens.size()]);
            }
        };
        if ( timestamp != null && !permitMultipleTimestampSignatures) {
            Element timeElement = timestamp.asElement();
            SigningSecurityToken[] signingTokens = processorResult.getSigningTokens(timeElement);
            if (signingTokens.length > 1) {
                throw new ProcessorValidationException("More than one signing token over Timestamp detected!");
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
     * NOTE that this method will be called multiple times per reference, once in the "pre-procssing" phase
     * and once during main processing.
     *
     * @param str  the SecurityTokenReference element
     * @param logIfNothingFound True to log if the reference is missing or of an unknown type
     * @param tokenRequired True if a missing token is an error / warning
     * @throws com.l7tech.util.InvalidDocumentFormatException if STR is invalid format or points at something unsupported
     * @throws com.l7tech.security.xml.processor.ProcessorException if a securityContextFinder is required to resolve this STR, but one was not provided
     */
    private void processSecurityTokenReference(Element str, boolean logIfNothingFound, boolean tokenRequired )
            throws InvalidDocumentFormatException, ProcessorException
    {
        // Check if already processed
        if ( strToTarget.containsKey( str ) ) return;

        // Get identifier
        final String id = DomUtils.getElementIdValue(str, idAttributeConfig);
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

        processSecurityTokenReferenceWSS11(str);

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
                if ( tokenRequired ) {
                    String msg = "Rejecting SecurityTokenReference ID='" + logId + "' with ValueType of '" + valueType +
                                 "' because its target is either missing or not a BinarySecurityToken";
                    logger.warning(msg);
                    throw new InvalidDocumentFormatException(msg);
                } else {
                    return;
                }
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
                if ( tokenRequired ) {
                    String msg = "Rejecting SecurityTokenReference ID='" + logId + "' with ValueType of '" + valueType +
                                 "' because its target is either missing or not a SAML assertion";
                    throw new InvalidDocumentFormatException(msg);
                } else {
                    return;
                }
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

            KerberosSigningSecurityToken ksst = findKerberosSigningSecurityTokenBySha1(value,str);
            if ( ksst == null && tokenRequired ) {
                logger.warning("Could not find referenced Kerberos security token '"+value+"'.");
            }
        } else {
            if (logIfNothingFound)
                logger.warning("Ignoring SecurityTokenReference ID=" + logId + " with ValueType of " + valueType);
        }
    }

    private void processSecurityTokenReferenceWSS11(Element str) {
        if (str == null)
            return;

        Element keyIdentifierElement = DomUtils.findFirstChildElementByName(str, str.getNamespaceURI(), "KeyIdentifier");
        Element referenceElement = DomUtils.findFirstChildElementByName(str, str.getNamespaceURI(), "Reference");
        if (keyIdentifierElement != null) {
            String valueType = keyIdentifierElement.getAttribute("ValueType");
            if ( SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1.equals(valueType) || SoapConstants.VALUETYPE_X509_THUMB_SHA1.equals(valueType) ) {
                isWsse11Seen = true;
            }
        } else if (referenceElement != null) {
            if ( SoapConstants.VALUETYPE_ENCRYPTED_KEY.equals(referenceElement.getAttribute("ValueType"))) {
                isWsse11Seen = true;
            }
        }
        if ( SoapConstants.VALUETYPE_ENCRYPTED_KEY.equals(str.getAttributeNS(SoapConstants.SECURITY11_NAMESPACE, "TokenType")) ) {
            isWsse11Seen = true;
        }
    }

    private void processDerivedKey(Element derivedKeyEl)
            throws InvalidDocumentFormatException, ProcessorException, GeneralSecurityException {
        // get corresponding shared secret reference wsse:SecurityTokenReference
        final Element sTokrefEl = DomUtils.findFirstChildElementByName(derivedKeyEl,
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
                    xst = findKerberosSigningSecurityTokenBySha1(ref, sTokrefEl);
                }

                derivationSource = xst;
            } else if ( SoapConstants.VALUETYPE_SAML_ASSERTIONID_SAML11.equals(valueType) ||
                        SoapConstants.VALUETYPE_SAML_ASSERTIONID_SAML20.equals(valueType) ) {
                final String samlAssertionId = XmlUtil.getTextValue( keyIdEl );
                EncryptedKey ek = null;

                // Check for a token in the message
                final SamlAssertion samlAssertion = findSamlSecurityTokenByAssertionId(samlAssertionId);
                if (samlAssertion != null) {
                    ek = samlAssertion.getSubjectConfirmationEncryptedKey(securityTokenResolver);
                }

                if (ek == null) {
                    // Check for a previously seen token
                    if (securityTokenResolver == null) {
                        throw new ProcessorException("Unable to process DerivedKeyToken - it references a SAML token by ID, but no security token resolver is available");
                    }
                    //TODO track/cache SAML tokens rather than just the encrypted key secret
                    final byte[] secret = securityTokenResolver.getSecretKeyByTokenIdentifier( valueType, samlAssertionId );
                    if ( secret != null ) {
                        ek = WssProcessorUtil.makeEncryptedKey(releventSecurityHeader.getOwnerDocument(), secret, "");
                    }
                }

                if (ek == null) {
                    throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to an unknown SAML assertion '"+samlAssertionId+"'");
                }

                derivationSource = ek;
            } else
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to unsupported ValueType " + valueType);

        } else {
            ref = refEl.getAttribute("URI");
            if (ref == null || ref.length() < 1)
                throw new InvalidDocumentFormatException("DerivedKeyToken's SecurityTokenReference lacks URI parameter");
            if (ref.startsWith("#"))
                derivationSource = findXmlSecurityTokenById(ref);
            else {
                XmlSecurityToken tok = findSecurityContextTokenBySessionId(ref);

                // The SecurityContextToken may not be in this message, try lookup by ID
                if (tok == null && refEl.getAttribute("ValueType").endsWith("/sct") ) {
                    SecurityContext sctx = securityContextFinder.getSecurityContext("DerivedKeyToken", ref);
                    if (sctx != null) {
                        Element elm = refEl.getOwnerDocument().createElementNS("http://layer7tech.com/ns/wssc/SCT/virtual", "SecurityContextToken");
                        tok = new SecurityContextTokenImpl(sctx, elm, ref);
                        securityTokens.add(tok);
                    }
                }

                derivationSource = tok;
            }
        }

        processSecurityTokenReferenceWSS11(sTokrefEl);

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
        } else if (derivationSource instanceof KerberosSigningSecurityToken) {
            derivedKeyTokens.add(deriveKeyFromKerberosToken(derivedKeyEl, (KerberosSigningSecurityToken)derivationSource));
            isDerivedKeySeen = true;
        } else
            logger.info("Unsupported DerivedKeyToken reference target '" + derivationSource.getType() + "', ignoring this derived key.");
    }

    // @return a new DerivedKeyToken.  Never null.
    private static DerivedKeyToken deriveKeyFromEncryptedKey(Element derivedKeyEl, EncryptedKey ek) throws InvalidDocumentFormatException, GeneralSecurityException {
        return deriveKeyFromToken( derivedKeyEl, ek.getSecretKey(), ek );
    }

    // @return a new DerivedKeyToken.  Never null.
    private static DerivedKeyToken deriveKeyFromSecurityContext(Element derivedKeyEl, SecurityContextToken sct) throws InvalidDocumentFormatException {
        return deriveKeyFromToken( derivedKeyEl, sct.getSecurityContext().getSharedSecret(), sct );
    }

    private static DerivedKeyToken deriveKeyFromToken( final Element derivedKeyEl,
                                                       final byte[] secret,
                                                       final XmlSecurityToken token ) throws InvalidDocumentFormatException {
        try {
            final SecureConversationKeyDeriver keyDeriver = new SecureConversationKeyDeriver();
            final byte[] resultingKey = keyDeriver.derivedKeyTokenToKey(derivedKeyEl, secret);
            // remember this symmetric key so it can later be used to process the signature
            // or the encryption
            return new DerivedKeyTokenImpl(derivedKeyEl, resultingKey, token);
        } catch ( NoSuchAlgorithmException e) {
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
            logger.log( Level.WARNING,
                    "A usernametoken element was encountered but we dont support the format: '"+ExceptionUtils.getMessage( e )+"'",
                    ExceptionUtils.getDebugException( e ) );
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
                String msg = "Cannot resolve encrypted data element " + dataRefUri;
                logger.warning(msg);
                throw new ProcessorValidationException(msg);
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

        setDocumentModified();
        boolean onlyChild = isOnlyChild(encryptedDataElement);

        // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
        // soapMsg document
        final List<String> algorithm = new ArrayList<String>();

        // Support "flexible" answers to getAlgorithm() query when using 3des with HSM (Bug #3705)
        final FlexKey flexKey = new FlexKey(key);

        // override getEncryptionEngine to collect the encryptionmethod algorithm
        final EncryptionEngineAlgorithmCollectingAlgorithmFactory af =
                new EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, algorithm);

        // TODO we won't know the actual cipher until the EncryptionMethod is created, so we'll hope that the Provider will be the same for all symmetric crypto
        Provider symmetricProvider = JceProvider.getInstance().getBlockCipherProvider();
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        final DecryptionContext dc = XencUtil.createContextForDecryption( af );
        dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT,
                            null, null);

        final NodeList dataList;
        try {
            dataList = XencUtil.decryptAndReplaceUsingKey(encryptedDataElement, flexKey, dc, new Functions.UnaryVoid<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    if (errorHandler != null) {
                        errorHandler.onDecryptionError(throwable);
                    } else {
                        logger.log(Level.FINE, "Error decrypting", throwable);
                    }
                }
            });
        } catch (XencUtil.XencException e) {
            if (errorHandler != null)
                errorHandler.onDecryptionError(e);
            throw new ProcessorException("Error decrypting", e);
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

    /**
     * See if the parent element contains nothing else except attributes and this EncryptedData element
     * (and possibly a whitespace node before and after it)
     *
     * @param mightBeOnlyChildElement  the child element that should be checked to see if it the only child element of its parent.  Required.
     * @return true iff. the specified element has no siblings except attributes and text nodes containing nothing but whitespace.
     */
    private static boolean isOnlyChild(Element mightBeOnlyChildElement) throws InvalidDocumentFormatException {
        Node parentNode = mightBeOnlyChildElement.getParentNode();
        if (parentNode == null)
            throw new InvalidDocumentFormatException("Element has no parent: " + mightBeOnlyChildElement.getNodeName());

        // TODO trim() throws out all CTRL characters along with whitespace.  Need to think about this.
        Node nextWhitespace = null;
        Node nextSib = mightBeOnlyChildElement.getNextSibling();
        if (nextSib != null && nextSib.getNodeType() == Node.TEXT_NODE && nextSib.getNodeValue().trim().length() < 1)
            nextWhitespace = nextSib;
        Node prevWhitespace = null;
        Node prevSib = mightBeOnlyChildElement.getPreviousSibling();
        if (prevSib != null && prevSib.getNodeType() == Node.TEXT_NODE && prevSib.getNodeValue().trim().length() < 1)
            prevWhitespace = prevSib;

        boolean onlyChild = true;
        NodeList sibNodes = parentNode.getChildNodes();
        for (int i = 0; i < sibNodes.getLength(); ++i) {
            Node node = sibNodes.item(i);
            if (node == null || node.getNodeType() == Node.ATTRIBUTE_NODE)
                continue; // not relevant
            if (node == nextWhitespace || node == prevWhitespace)
                continue; // ignore
            if (node == mightBeOnlyChildElement)
                continue; // this is the encrypteddata element itself

            // we've found a relevant sibling, proving that not all of parentElement's non-attribute content
            // is encrypted within this EncryptedData
            onlyChild = false;
            break;
        }
        return onlyChild;
    }

    private void processBinarySecurityToken(final Element binarySecurityTokenElement)
            throws ProcessorException, GeneralSecurityException, InvalidDocumentFormatException {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Processing BinarySecurityToken");

        // assume that this is a b64ed binary x509 cert, get the value
        String valueType = binarySecurityTokenElement.getAttribute("ValueType");
        String encodingType = binarySecurityTokenElement.getAttribute("EncodingType");

        // todo use proper qname comparator rather than this hacky suffix check
        final Functions.BinaryVoidThrows<byte[], String, CertificateException> valueProcessor;
        if (valueType.endsWith("X509v3")) {
            // X509
            valueProcessor = new Functions.BinaryVoidThrows<byte[], String, CertificateException>() {
                @Override
                public void call(byte[] decodedValue, String wsuId) throws CertificateException {
                    recordCertFromBst(binarySecurityTokenElement, decodedValue, wsuId);
                }
            };
        } else if (valueType.endsWith("#GSS_Kerberosv5_AP_REQ")) {
            // Kerberos
            valueProcessor = new Functions.BinaryVoidThrows<byte[], java.lang.String, java.security.cert.CertificateException>() {
                @Override
                public void call(byte[] decodedValue, String wsuId) throws CertificateException {
                    recordKerberosFromBst(binarySecurityTokenElement, decodedValue, wsuId);
                }
            };
        } else if (valueType.endsWith("#X509PKIPathv1")) {
            // PkiPath object
            valueProcessor = new Functions.BinaryVoidThrows<byte[], String, CertificateException>() {
                @Override
                public void call(byte[] decodedValue, String wsuId) throws CertificateException {
                    // create the x509 binary cert based on it
                    recordCertPathFromBst(binarySecurityTokenElement, decodedValue, wsuId, "PkiPath");
                }
            };
        } else if (valueType.endsWith("#PKCS7")) {
            // Cert list from degenerate PCKS#7 with no signature
            valueProcessor = new Functions.BinaryVoidThrows<byte[], String, CertificateException>() {
                @Override
                public void call(byte[] decodedValue, String wsuId) throws CertificateException {
                    // create the x509 binary cert based on it
                    recordCertPathFromBst(binarySecurityTokenElement, decodedValue, wsuId, "PKCS7");
                }
            };
        } else {
            if (permitUnknownBinarySecurityTokens) {
                return; // ignore this token
            } else {
                throw new ProcessorValidationException("BinarySecurityToken has unsupported ValueType " + valueType);
            }
        }

        if (encodingType != null && encodingType.length() > 0 && !encodingType.endsWith("Base64Binary"))
            throw new ProcessorException("BinarySecurityToken has unsupported EncodingType " + encodingType);

        String value = DomUtils.getTextValue(binarySecurityTokenElement);
        if (value == null || value.length() < 1) {
            String msg = "The " + binarySecurityTokenElement.getLocalName() + " does not contain a value.";
            logger.warning(msg);
            throw new ProcessorException(msg);
        }

        final byte[] decodedValue; // must strip whitespace or base64 decoder misbehaves
        decodedValue = HexUtils.decodeBase64(value, true);

        final String wsuId = DomUtils.getElementIdValue(binarySecurityTokenElement, idAttributeConfig);

        valueProcessor.call(decodedValue, wsuId);
    }

    private void recordKerberosFromBst(Element binarySecurityTokenElement, byte[] decodedValue, String wsuId) {
        try {
            String spn;
            try {
                HttpRequestKnob requestKnob = message.getKnob(HttpRequestKnob.class);
                if (requestKnob != null && requestKnob.getRequestURL() != null) {
                    spn = KerberosClient.getKerberosAcceptPrincipal(requestKnob.getRequestURL().getProtocol(),
                            requestKnob.getRequestURL().getHost(), false);
                } else {
                    spn = KerberosClient.getKerberosAcceptPrincipal(false);
                }
            }
            catch(KerberosException ke) { // fallback to system property name
                spn = KerberosClient.getGSSServiceName();
            }
            securityTokens.add(new KerberosSigningSecurityTokenImpl(spn, new KerberosGSSAPReqTicket(decodedValue),
                                                                           getClientInetAddress(message),
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

    private void recordCertFromBst(Element binarySecurityTokenElement, byte[] decodedValue, String wsuId) throws CertificateException {
        // create the x509 binary cert based on it
        X509Certificate referencedCert = CertUtils.decodeCert(decodedValue);

        // remember this cert
        if (wsuId == null)
            logger.fine("This BinarySecurityToken does not have a recognized wsu:Id and may not be referenced properly by a subsequent signature.");
        XmlSecurityToken rememberedSecToken = new X509BinarySecurityTokenImpl(referencedCert, binarySecurityTokenElement);
        securityTokens.add(rememberedSecToken);
        x509TokensById.put(wsuId, rememberedSecToken);
    }

    private void recordCertPathFromBst(Element binarySecurityTokenElement, byte[] decodedValue, String wsuId, String certPathFormat) throws CertificateException {
        CertPath certPath = CertUtils.getFactory().generateCertPath(new ByteArrayInputStream(decodedValue), certPathFormat);
        List<? extends Certificate> certs = certPath.getCertificates();
        if (certs.isEmpty()) {
            logger.info(certPathFormat + " BinarySecurityToken contains an empty cert path");
            return;
        }
        final Certificate cert = certs.get(0);
        if (!(cert instanceof X509Certificate)) {
            logger.info(certPathFormat + " BinarySecurityToken contains a non-X.509 subject certificate");
            return;
        }
        X509Certificate x509Cert = (X509Certificate) cert;
        XmlSecurityToken rememberedSecToken = new X509BinarySecurityTokenImpl(x509Cert, binarySecurityTokenElement);
        securityTokens.add(rememberedSecToken);
        x509TokensById.put(wsuId, rememberedSecToken);
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

    private X509SigningSecurityTokenImpl handleX509Data(final Element str) throws InvalidDocumentFormatException, GeneralSecurityException {
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
            final X509SigningSecurityTokenImpl signingSecurityToken =
                    X509BinarySecurityTokenImpl.createBinarySecurityToken(str.getOwnerDocument(), certificate, str.getPrefix(), str.getNamespaceURI(), SecurityTokenType.X509_ISSUER_SERIAL);
            securityTokens.add( signingSecurityToken );
            strToTarget.put( str, signingSecurityToken.asElement() );
            return signingSecurityToken;
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
            KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyElement, securityTokenResolver );
        } catch (UnexpectedKeyInfoException e) {
            if (secHeaderActor == SecurityActor.L7ACTOR) {
                logger.warning("We do not appear to be the intended recipient for this EncryptedKey however the " +
                               "security header is clearly addressed to us");
                throw e;
            } else if (secHeaderActor == SecurityActor.NOACTOR) {
                logger.log(Level.INFO, "We do not appear to be the intended recipient for this " +
                                       "EncryptedKey. Will leave it alone since the security header is not " +
                                       "explicitly addressed to us.", ExceptionUtils.getDebugException( e ));
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
            ekTok = new EncryptedKeyImpl(encryptedKeyElement, securityTokenResolver );
            if (refList != null)
                decryptReferencedElements(ekTok.getSecretKey(), refList);
        } catch (ParserConfigurationException e) {
            logger.log(Level.FINE, "Error decrypting", e);
            throw new ProcessorException("Error decrypting", e);
        } catch (SAXException e) {
            logger.log(Level.FINE, "Error decrypting", e);
            throw new ProcessorException("Error decrypting", e);
        } catch (IOException e) {
            logger.log(Level.FINE, "Error decrypting", e);
            throw new ProcessorException("Error decrypting", e);
        }

        String wsuId = DomUtils.getElementIdValue(encryptedKeyElement, idAttributeConfig);
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

                // Add the fake X509SecurityToken that signed the assertion
                final EmbeddedSamlSignatureToken samlSignatureToken = new EmbeddedSamlSignatureToken(samlToken);
                securityTokens.add(samlSignatureToken);
                elementsThatWereSigned.addAll(Arrays.asList(samlSignatureToken.getSignedElements()));
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

    private void processSignature( final Element sigElement )
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

        // Process any STR that is used within the signature
        Element keyInfoStr = DomUtils.findFirstChildElementByName(keyInfoElement, SoapConstants.SECURITY_URIS_ARRAY, "SecurityTokenReference");
        if (keyInfoStr != null) {
            processSecurityTokenReference(keyInfoStr, false, true);
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
                logger.log(Level.FINE, "Signing certificate expired " + signingCert.getNotAfter(), ExceptionUtils.getDebugException(e));
                throw new ProcessorException(e);
            } catch (CertificateNotYetValidException e) {
                logger.log(Level.FINE, "Signing certificate is not valid until " + signingCert.getNotBefore(), ExceptionUtils.getDebugException(e));
                throw new ProcessorException(e);
            }
        }

        // Validate signature
        final SignatureContext sigContext = DsigUtil.createSignatureContextForValidation();
        final MimeKnob mimeKnob = message.getKnob(MimeKnob.class);
        PartIterator iterator;
        iterator = mimeKnob == null ? null : mimeKnob.getParts();
        final Map<String,PartInfo> partMap = new HashMap<String,PartInfo>();
        sigContext.setEntityResolver(new AttachmentEntityResolver(iterator, XmlUtil.getXss4jEntityResolver(), partMap, signedAttachmentSizeLimit));
        sigContext.setIDResolver(new IDResolver() {
            @Override
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

                // Might be a reference to an element that didn't exist yet when the elementsByWsuId map was
                // generated (perhaps it was just decrypted out of an EncryptedData)?  Do a full slow scan
                // before giving up.
                try {
                    return SoapUtil.getElementByWsuId(doc, s);
                } catch (InvalidDocumentFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        sigContext.setAlgorithmFactory(new WssProcessorAlgorithmFactory(strToTarget));
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.verifyXml, signingCert, signingKey);
        Validity validity = DsigUtil.verify(sigContext, sigElement, signingKey);

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
            String msg = DsigUtil.getInvalidSignatureMessage(validity);
            logger.warning(msg);
            throw new InvalidDocumentSignatureException(msg);
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

                // Check for and reject full xpointer references
                // TODO we should probably also check for xpointer child sequences such as "/1/2/3" or "<id>/1/2/3"
                if (elementCoveredURI!=null && elementCoveredURI.startsWith("xpointer") &&
                    elementCoveredURI.indexOf('(')>0 &&  elementCoveredURI.indexOf(')')>0 ) {
                    throw new InvalidDocumentFormatException("XPointer reference not supported : " + elementCoveredURI);
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
                final SignedElement signedElement = new SignedElementImpl(
                    signingSecurityToken, elementCovered, sigElement,
                    DsigUtil.findSigAlgorithm(sigElement), DsigUtil.findDigestAlgorithms(sigElement) );
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
        bst.setAttributeNS(null, "ValueType", SoapConstants.VALUETYPE_X509);
        DomUtils.setTextContent(bst, HexUtils.encodeBase64(certificate.getEncoded(), true));
        signingCertToken = new X509BinarySecurityTokenImpl(certificate, bst);
        return signingCertToken;
    }

    private InetAddress getClientInetAddress( final Message message ) {
        InetAddress clientAddress = null;

        TcpKnob tcpKnob = message.getKnob(TcpKnob.class);
        if ( tcpKnob != null ) {
            try {
                clientAddress = InetAddress.getByName( tcpKnob.getRemoteAddress() );
            } catch (UnknownHostException e) {
                // should not get here since we pass an IP address
                logger.log( Level.INFO,
                        "Could not create address for remote IP '"+tcpKnob.getRemoteAddress()+"'.",
                        ExceptionUtils.getDebugException( e ));
            }
        }

        return clientAddress;
    }

    private static class WrappedSecurityContextFinder {
        private final SecurityContextFinder securityContextFinder;

        private WrappedSecurityContextFinder( final SecurityContextFinder securityContextFinder ) {
            this.securityContextFinder = securityContextFinder;
        }

        private SecurityContext getSecurityContext( final String what,
                                                    final String identifier ) throws ProcessorException {
            if (securityContextFinder == null)
                throw new ProcessorValidationException(what + " element found in message, but no SecurityContextFinder is available");

            return securityContextFinder.getSecurityContext( identifier );
        }
    }
}
