/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.security.xml.decorator;

import com.ibm.xml.dsig.*;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.keys.AesKey;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.security.xml.processor.X509BinarySecurityTokenImpl;
import com.l7tech.security.xml.processor.X509SigningSecurityTokenImpl;
import com.l7tech.util.*;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    private static final Logger logger = Logger.getLogger(WssDecorator.class.getName());

    public static final String PROPERTY_SUPPRESS_NANOSECONDS = "com.l7tech.server.timestamp.omitNanos";
    public static final String PROPERTY_SAML_USE_URI_REF = "com.l7tech.server.saml.useUriReference";
    public static final String PROPERTY_PROTECTTOKENS_SIGNS_DERIVED_KEYS = "com.l7tech.security.xml.protectTokensSignsDerivedKeys";

    private static final boolean PROTECTTOKENS_SIGNS_DERIVED_KEYS = SyspropUtil.getBoolean(PROPERTY_PROTECTTOKENS_SIGNS_DERIVED_KEYS, false);

    public static final int TIMESTAMP_TIMOUT_MILLIS = 300000;
    private static final int NEW_DERIVED_KEY_LENGTH = 32;
    private static final int OLD_DERIVED_KEY_LENGTH = 16;

    private static final Random rand = new SecureRandom();

    public WssDecoratorImpl() {
    }

    private static class Context {
        private final Message message;
        private final DecorationRequirements dreq;
        private long count = 0;
        private Map<String, Element> idToElementCache = new HashMap<String, Element>();
        private NamespaceFactory nsf = new NamespaceFactory();
        private byte[] lastEncryptedKeyBytes = null;
        private SecretKey lastEncryptedKeySecretKey = null;
        private AttachmentEntityResolver attachmentResolver;
        private Map<String,Boolean> signatures = new HashMap<String, Boolean>();
        private Set<String> encryptedSignatures = new HashSet<String>();

        private Context( final Message message,
                         final DecorationRequirements dreq ) {
            this.message = message;
            this.dreq = dreq;
        }

        String getBase64EncodingTypeUri() {
            return SoapConstants.SECURITY_NAMESPACE.equals(nsf.getWsseNs())
                    ? SoapConstants.ENCODINGTYPE_BASE64BINARY
                    : SoapConstants.ENCODINGTYPE_BASE64BINARY_2;     // lyonsm: ??? what is this for?  It hardcodes wsse: prefix!
        }
    }

    /**
     * @return random extra microseconds to add to the timestamp to make it more unique, or zero to not bother.
     */
    private static long getExtraTime( final Boolean includeNanoseconds ) {
        return  (includeNanoseconds != null && !includeNanoseconds) || (includeNanoseconds == null && SyspropUtil.getBoolean(PROPERTY_SUPPRESS_NANOSECONDS)) ?
                -1L : 
                (long) rand.nextInt(1000000);
    }

    /**
     * Decorate a soap message with WSS style security.
     *
     * @param message the soap message to decorate
     */
    @Override
    public DecorationResult decorateMessage( final Message message,
                                             final DecorationRequirements dreq )
      throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException, SAXException, IOException {
        final Context c = new Context( message, dreq );
        c.nsf = dreq.getNamespaceFactory();
        c.attachmentResolver = buildAttachmentEntityResolver(c, dreq);

        // get writeable document after getting MIME part iterator
        final Document soapMsg = message.getXmlKnob().getDocumentWritable();

        Boolean mustUnderstand = getMustUnderstand( dreq );
        Element securityHeader = createSecurityHeader(soapMsg, c,
                dreq.getSecurityHeaderActor(), mustUnderstand, dreq.isSecurityHeaderReusable());
        Set<Element> signList = dreq.getElementsToSign();
        Set<Element> cryptList = dreq.getElementsToEncrypt();
        Set<String> signPartList = dreq.getPartsToSign();

        Element timestamp = null;
        if ( dreq.isSecurityHeaderReusable() ) {
            timestamp = DomUtils.findOnlyOneChildElementByName(securityHeader, c.nsf.getWsuNs(), SoapUtil.TIMESTAMP_EL_NAME);
        }

        int timeoutMillis = dreq.getTimestampTimeoutMillis();
        if (timeoutMillis < 1)
            timeoutMillis = TIMESTAMP_TIMOUT_MILLIS;
        Boolean includeNanoSeconds = dreq.getTimestampResolution() == DecorationRequirements.TimestampResolution.DEFAULT ?
                null :
                dreq.getTimestampResolution() == DecorationRequirements.TimestampResolution.NANOSECONDS;
        if (dreq.isIncludeTimestamp() && timestamp==null) {
            Date createdDate = dreq.getTimestampCreatedDate();
            // Have to add some uniqueness to this timestamp
            timestamp = SoapUtil.addTimestamp(securityHeader, 
                c.nsf.getWsuNs(),
                createdDate, // null ok
                dreq.getTimestampResolution() != DecorationRequirements.TimestampResolution.SECONDS,
                getExtraTime( includeNanoSeconds ),
                timeoutMillis);
        }

        // If we aren't signing the entire message, find extra elements to sign
        if (dreq.isSignTimestamp() || !signList.isEmpty() || !signPartList.isEmpty()) {
            if (timestamp == null)
                timestamp = SoapUtil.addTimestamp(securityHeader,
                    c.nsf.getWsuNs(),
                    dreq.getTimestampCreatedDate(), // null ok
                    dreq.getTimestampResolution() != DecorationRequirements.TimestampResolution.SECONDS,
                    getExtraTime( includeNanoSeconds ),
                    timeoutMillis);
            signList.add(timestamp);
        }

        Element xencDesiredNextSibling = null;
        if ( dreq.isSecurityHeaderReusable() ) {
            // Add before any signatures or encrypted keys that are already in the header
            Element firstEncKeyElement =
                    DomUtils.findFirstChildElementByName( securityHeader, SoapUtil.XMLENC_NS, "EncryptedKey" );
            Element firstReferenceListElement =
                    DomUtils.findFirstChildElementByName( securityHeader, SoapUtil.XMLENC_NS, "ReferenceList" );
            Element firstSignatureElement =
                    DomUtils.findFirstChildElementByName( securityHeader, SoapUtil.DIGSIG_URI, "Signature" );

            if ( firstEncKeyElement != null )
                xencDesiredNextSibling = firstEncKeyElement;
            if ( firstReferenceListElement != null && (xencDesiredNextSibling==null || DomUtils.isAfter(firstReferenceListElement,xencDesiredNextSibling)))
                xencDesiredNextSibling = firstReferenceListElement;
            if ( firstSignatureElement != null && (xencDesiredNextSibling==null || DomUtils.isAfter(firstSignatureElement,xencDesiredNextSibling)))
                xencDesiredNextSibling = firstSignatureElement;
        }

        if (!dreq.getSignatureConfirmations().isEmpty()) {
            for (String conf : dreq.getSignatureConfirmations()) {
                Element sc = addSignatureConfirmation(securityHeader, conf);
                signList.add(sc);
            }
        }

        Element addedUsernameTokenHolder = null; // dummy element to hold fully-encrypted usernametoken.  will be removed later
        if (dreq.getUsernameTokenCredentials() != null) {
            Element usernameToken = createUsernameToken(securityHeader, dreq.getUsernameTokenCredentials());
            if (dreq.isSignUsernameToken()) signList.add(usernameToken);
            if (dreq.isEncryptUsernameToken()) {
                addedUsernameTokenHolder = DomUtils.createAndAppendElementNS(securityHeader,
                                                                            "EncryptedUsernameToken",
                                                                            usernameToken.getNamespaceURI(),
                                                                            usernameToken.getPrefix());
                securityHeader.removeChild(usernameToken);
                addedUsernameTokenHolder.appendChild(usernameToken);
                if (dreq.isSignUsernameToken())
                    signList.add(usernameToken);
                cryptList.add(addedUsernameTokenHolder);
            }
        }

        SamlAssertion saml = dreq.getSenderSamlToken();
        Element samlElement = null;
        if (saml != null) {
            samlElement = addSamlSecurityToken(securityHeader, saml.asElement());
            if (dreq.isIncludeSamlTokenInSignature()) {
                signList.add(samlElement);
            }
        }

        // If there are any L7a or WSA headers in the message, and we are signing anything else, then sign them too
        addAddressingElements( soapMsg, signList );

        // Add sender cert
        final Pair<X509Certificate, KeyInfoDetails> senderCertKeyInfo;
        senderCertKeyInfo = processSenderCertificate( c, securityHeader, signList );

        // Add kerberos ticket reference
        if (dreq.isIncludeKerberosTicketId() && dreq.getKerberosTicketId() != null) {
            Element ktokStr = addKerberosSecurityTokenReference(securityHeader, dreq.getKerberosTicketId());
            if (dreq.isProtectTokens() && !signList.isEmpty())
                signList.add(ktokStr);
        }

        // Add Kerberos ticket
        Element addedKerberosBst = null;
        if (dreq.isIncludeKerberosTicket() && dreq.getKerberosTicket() != null) {
            addedKerberosBst = addKerberosBinarySecurityToken(securityHeader, dreq.getKerberosTicket().getGSSAPReqTicket());
            if (dreq.isProtectTokens() && !signList.isEmpty())
                signList.add(addedKerberosBst);
        }

        // At this point, if we possess a sender cert, we have a senderCertKeyInfo, and have also have added a BST unless it's suppressed

        Element sct = null;
        DecorationRequirements.SecureConversationSession session =
          dreq.getSecureConversationSession();
        if (session != null) {
            if (session.getId() == null)
                throw new DecoratorException("If SecureConversation Session is specified, but it has no session ID");
            if (session.getSCNamespace() != null && session.getSCNamespace().equals( SoapConstants.WSSC_NAMESPACE2)) {
                c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
            }
            sct = addSecurityContextToken(c, securityHeader, session.getId());
            if (dreq.isProtectTokens() && !signList.isEmpty())
                signList.add(sct);
        }

        final Element signature;
        Element addedEncKey = null;
        XencUtil.XmlEncKey addedEncKeyXmlEncKey = null;
        if (signList.size() > 0) {
            final SignatureInfo signatureInfo;

            if ( dreq.getPreferredSigningTokenType() != null ) {
                switch ( dreq.getPreferredSigningTokenType() ) {
                    case X509:
                        signatureInfo = buildX509SignatureInfo( dreq, senderCertKeyInfo );
                        break;
                    default:
                        throw new DecoratorException("Signing is requested, but there is no key available.");
                }
            } else {
                if (sct != null) {
                    signatureInfo = processSecureConversationSigningToken( dreq, c, securityHeader, signList, sct, session );
                } else if (dreq.getEncryptedKey() != null &&
                           dreq.getEncryptedKeySha1() != null) {
                    signatureInfo = processEncryptedKeySigningToken( c, securityHeader, signList );
                } else if (addedKerberosBst != null) {
                    signatureInfo = processKerberosSigningToken( c, securityHeader, signList, addedKerberosBst );
                } else if (dreq.getKerberosTicket() != null) {
                    signatureInfo = processKerberosSHA1SigningToken( c, securityHeader, signList );
                } else if (senderCertKeyInfo.right != null) {
                    signatureInfo = buildX509SignatureInfo( dreq, senderCertKeyInfo );
                } else if (saml != null) {
                    signatureInfo = processSamlSigningToken( dreq, signList, saml, samlElement );
                } else if (dreq.getRecipientCertificate() != null) {
                    final Element[] addedEncKeyHolder = new Element[1];
                    final XencUtil.XmlEncKey[] addedEncKeyXmlEncKeyHolder = new XencUtil.XmlEncKey[1];
                    signatureInfo = processGeneratedEncryptedKeySigningToken( c, securityHeader, signList, addedEncKeyHolder, addedEncKeyXmlEncKeyHolder );
                    addedEncKey = addedEncKeyHolder[0];
                    addedEncKeyXmlEncKey = addedEncKeyXmlEncKeyHolder[0];
                } else
                    throw new DecoratorException("Signing is requested, but there is no key available.");
            }
            signature = addSignature(c,
                signatureInfo.senderSigningKey,
                signatureInfo.senderSigningCert,
                dreq.getSignatureMessageDigest(),
                signList.toArray(new Element[signList.size()]),
                signPartList.toArray(new String[signPartList.size()]),
                dreq.isSignPartHeaders(),
                securityHeader,
                signatureInfo.signatureKeyInfo,
                dreq.isSuppressSamlStrTransform());
            if (xencDesiredNextSibling == null)
                xencDesiredNextSibling = signature;

            NodeList sigValues = signature.getElementsByTagNameNS(SoapConstants.DIGSIG_URI, "SignatureValue");
            for(int i=0; i < sigValues.getLength(); i++) {
                c.signatures.put(sigValues.item(i).getTextContent(), isEncrypted(cryptList, sigValues.item(i)));
            }
        }

        if (cryptList.size() > 0) {
            // report any signature values that are getting encrypted by this decoration
            for (Element encrypted : cryptList) {
                NodeList sigValues = encrypted.getElementsByTagNameNS(SoapConstants.DIGSIG_URI, "SignatureValue");
                for(int i=0; i < sigValues.getLength(); i++) {
                    c.encryptedSignatures.add(sigValues.item(i).getTextContent());
                }
            }

            final Element[] elementsToEncrypt = cryptList.toArray(new Element[cryptList.size()]);
            if (sct != null) {
                encryptWithSecureConversationToken( c, securityHeader, xencDesiredNextSibling, sct, session, elementsToEncrypt );
            } else if (addedEncKey != null && addedEncKeyXmlEncKey != null) {
                encryptWithGeneratedEncryptedKeyToken( c, securityHeader, xencDesiredNextSibling, addedEncKey, addedEncKeyXmlEncKey, elementsToEncrypt );
            } else if (dreq.getEncryptedKeySha1() != null &&
                       dreq.getEncryptedKey() != null) {
                encryptWithEncryptedKeyToken( c, securityHeader, xencDesiredNextSibling, elementsToEncrypt );
            } else if (addedKerberosBst != null) {
                encryptWithKerberosToken( c, securityHeader, xencDesiredNextSibling, addedKerberosBst, elementsToEncrypt );
            } else if (dreq.getKerberosTicket() != null) {
                encryptWithKerberosSHA1Token( c, securityHeader, xencDesiredNextSibling, elementsToEncrypt );
            } else if (dreq.getRecipientCertificate() != null) {
                encryptWithX509Token( c, securityHeader, xencDesiredNextSibling, elementsToEncrypt );
            } else
                throw new DecoratorException("Encryption is requested, but there is no recipient key.");

            // Transform any encrypted username token into the correct form and position
            if (addedUsernameTokenHolder != null) {
                Element encdata = DomUtils.findFirstChildElement(addedUsernameTokenHolder);
                if (encdata == null || !"EncryptedData".equals(encdata.getLocalName()))
                    throw new DecoratorException("EncryptedUsernameToken does not contain EncryptedData"); // can't happen
                addedUsernameTokenHolder.removeChild(encdata);
                securityHeader.removeChild(addedUsernameTokenHolder);
                securityHeader.insertBefore(encdata, xencDesiredNextSibling);
            }
        }

        // Decoration is done.

        // Final cleanup: if we are about to emit an empty Security header, remove it now
        if (DomUtils.elementIsEmpty(securityHeader)) {
            final Element soapHeader = (Element)securityHeader.getParentNode();
            soapHeader.removeChild(securityHeader);

            // If we are about to emit an empty SOAP header, remove it now
            if (DomUtils.elementIsEmpty(soapHeader))
                soapHeader.getParentNode().removeChild(soapHeader);
        }

        DecorationResult result = produceResult(c);
        message.getSecurityKnob().addDecorationResult(result);
        return result;
    }

    private Pair<X509Certificate, KeyInfoDetails> processSenderCertificate( final Context c,
                                                                            final Element securityHeader,
                                                                            final Set<Element> signList ) throws CertificateEncodingException, DecoratorException {
        final Pair<X509Certificate, KeyInfoDetails> senderCertKeyInfo;

        // note [bugzilla #2551] dont include a x509 BST if this is gonna use a sc context
        // note [bugzilla #3907] dont include a x509 BST if using kerberos
        // don't include BST if adding encrypted username token
        if (c.dreq.getSenderMessageSigningCertificate() != null &&
            !signList.isEmpty() &&
            (c.dreq.getPreferredSigningTokenType() == DecorationRequirements.PreferredSigningTokenType.X509 ||
             (c.dreq.getSecureConversationSession() == null && c.dreq.getKerberosTicket() == null) ) &&
            !c.dreq.isEncryptUsernameToken() )
        {
            final X509Certificate senderMessageSigningCert = c.dreq.getSenderMessageSigningCertificate();
            switch(c.dreq.getKeyInfoInclusionType()) {
                case CERT: {
                    // Use keyinfo reference target of a BinarySecurityToken
                    Element x509Bst = addX509BinarySecurityToken(securityHeader, c.dreq.getSenderMessageSigningCertificate(), c);
                    String bstId = getOrCreateWsuId(c, x509Bst, null);
                    final KeyInfoDetails keyinfo = KeyInfoDetails.makeUriReference(bstId, SoapUtil.VALUETYPE_X509);
                    senderCertKeyInfo = new Pair<X509Certificate, KeyInfoDetails>(senderMessageSigningCert, keyinfo);
                    if (c.dreq.isProtectTokens())
                        signList.add(x509Bst);
                    break; }
                case STR_SKI: {
                    // Use keyinfo reference target of a SKI
                    X509Certificate senderCert = c.dreq.getSenderMessageSigningCertificate();
                    byte[] senderSki = CertUtils.getSKIBytesFromCert(senderCert);
                    if (senderSki == null) {
                        // Supposed to refer to sender cert by its SKI, but it has no SKI
                        throw new DecoratorException("suppressBst is requested, but the sender cert has no SubjectKeyIdentifier");
                    }
                    KeyInfoDetails keyinfo = KeyInfoDetails.makeKeyId(senderSki, SoapUtil.VALUETYPE_SKI);
                    senderCertKeyInfo = new Pair<X509Certificate, KeyInfoDetails>(senderMessageSigningCert, keyinfo);
                    break; }
                case ISSUER_SERIAL: {
                    senderCertKeyInfo = new Pair<X509Certificate, KeyInfoDetails>(senderMessageSigningCert, KeyInfoDetails.makeIssuerSerial(senderMessageSigningCert, true));
                    break; }
                default:
                    throw new DecoratorException("Unsupported KeyInfoInclusionType: " + c.dreq.getKeyInfoInclusionType());
            }
        } else {
            senderCertKeyInfo = new Pair<X509Certificate, KeyInfoDetails>(null, null);
        }

        return senderCertKeyInfo;
    }

    private void encryptWithX509Token( final Context c,
                                       final Element securityHeader,
                                       final Element xencDesiredNextSibling,
                                       final Element[] elementsToEncrypt ) throws GeneralSecurityException, DecoratorException {
        // Encrypt to recipient's certificate
        String encryptionAlgorithm = c.dreq.getEncryptionAlgorithm();

        XencUtil.XmlEncKey encKey = generateXmlEncKey(encryptionAlgorithm);
        addEncryptedKey(c,
                        securityHeader,
                        c.dreq.getRecipientCertificate(),
                        elementsToEncrypt,
                        encKey,
                        c.dreq.getKeyEncryptionAlgorithm(),
                        xencDesiredNextSibling);
    }

    private void encryptWithKerberosSHA1Token( final Context c,
                                               final Element securityHeader,
                                               final Element xencDesiredNextSibling,
                                               final Element[] elementsToEncrypt ) throws DecoratorException, GeneralSecurityException {
        // Derive key using KerberosSHA1 reference
        c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
        String kerbSha1 = KerberosUtils.getBase64Sha1(c.dreq.getKerberosTicket().getGSSAPReqTicket());
        KeyInfoDetails kerbShaRef = KeyInfoDetails.makeKeyId(kerbSha1, true, SoapConstants.VALUETYPE_KERBEROS_APREQ_SHA1);
        DerivedKeyToken dkt = addDerivedKeyToken(c,
                                                 securityHeader,
                                                 xencDesiredNextSibling,
                                                 kerbShaRef,
                                                 getKeyLengthInBytesForAlgorithm(c.dreq.getEncryptionAlgorithm()),
                                                 c.dreq.getKerberosTicket().getKey(),
                                                 "DerivedKey");
        String dktId = getOrCreateWsuId(c, dkt.dkt, "DerivedKey-Enc");
        XencUtil.XmlEncKey dktEncKey = new XencUtil.XmlEncKey(c.dreq.getEncryptionAlgorithm(), dkt.derivedKey);
        addEncryptedReferenceList(c,
                                  securityHeader,
                                  xencDesiredNextSibling,
                                  elementsToEncrypt,
                                  dktEncKey,
                                  KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
    }

    private void encryptWithKerberosToken( final Context c,
                                           final Element securityHeader,
                                           final Element xencDesiredNextSibling,
                                           final Element addedKerberosBst,
                                           final Element[] elementsToEncrypt ) throws DecoratorException, GeneralSecurityException {
        // Derive key using direct URI reference
        c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
        KeyInfoDetails kerbUriRef = KeyInfoDetails.makeUriReference(
                getOrCreateWsuId(c, addedKerberosBst, null),
                SoapConstants.VALUETYPE_KERBEROS_GSS_AP_REQ);
        DerivedKeyToken dkt = addDerivedKeyToken(c,
                                                 securityHeader,
                                                 xencDesiredNextSibling,
                                                 kerbUriRef,
                                                 getKeyLengthInBytesForAlgorithm(c.dreq.getEncryptionAlgorithm()),
                                                 c.dreq.getKerberosTicket().getKey(),
                                                 "DerivedKey");
        String dktId = getOrCreateWsuId(c, dkt.dkt, "DerivedKey-Enc");
        XencUtil.XmlEncKey dktEncKey = new XencUtil.XmlEncKey(c.dreq.getEncryptionAlgorithm(), dkt.derivedKey);
        addEncryptedReferenceList(c,
                                          securityHeader,
                                          xencDesiredNextSibling,
                                          elementsToEncrypt,
                                          dktEncKey,
                                          KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
    }

    private void encryptWithEncryptedKeyToken( final Context c,
                                               final Element securityHeader,
                                               final Element xencDesiredNextSibling,
                                               final Element[] elementsToEncrypt ) throws DecoratorException, GeneralSecurityException {
        final String eksha = c.dreq.getEncryptedKeySha1();
        final byte[] ekkey = c.dreq.getEncryptedKey();

        // Encrypt using a reference to an implicit EncryptedKey that the recipient is assumed
        // already to possess (perhaps because we got it from them originally)
        if (c.dreq.isUseDerivedKeys()) {
            // Derive a new key and use it for encryption
            DerivedKeyToken dkt = addDerivedKeyToken(c,
                                                     securityHeader,
                                                     xencDesiredNextSibling,
                                                     KeyInfoDetails.makeEncryptedKeySha1Ref(eksha),
                                                     getKeyLengthInBytesForAlgorithm(c.dreq.getEncryptionAlgorithm()),
                                                     ekkey,
                                                     "DerivedKey");
            String dktId = getOrCreateWsuId(c, dkt.dkt, "DerivedKey-Enc");
            XencUtil.XmlEncKey dktEncKey = new XencUtil.XmlEncKey(c.dreq.getEncryptionAlgorithm(), dkt.derivedKey);
            if (c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2)) {
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          xencDesiredNextSibling,
                                          elementsToEncrypt,
                                          dktEncKey,
                                          KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
            } else {
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          xencDesiredNextSibling,
                                          elementsToEncrypt,
                                          dktEncKey,
                                          KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY));
            }
        } else {
            // Reference the EncryptedKey directly
            final KeyInfoDetails keyInfoDetails = KeyInfoDetails.makeEncryptedKeySha1Ref(eksha);

            final String encryptionAlgorithm = c.dreq.getEncryptionAlgorithm();
            XencUtil.XmlEncKey encKey = generateXmlEncKey(encryptionAlgorithm);
            encKey = new XencUtil.XmlEncKey(encKey.getAlgorithm(), ekkey);
            addEncryptedReferenceList(c,
                                      securityHeader,
                                      xencDesiredNextSibling,
                                      elementsToEncrypt,
                                      encKey,
                                      keyInfoDetails);
        }
    }

    private void encryptWithGeneratedEncryptedKeyToken( final Context c,
                                                        final Element securityHeader,
                                                        final Element xencDesiredNextSibling,
                                                        final Element addedEncKey,
                                                        final XencUtil.XmlEncKey addedEncKeyXmlEncKey,
                                                        final Element[] elementsToEncrypt ) throws DecoratorException, GeneralSecurityException {
        if (c.dreq.isUseDerivedKeys()) {
            // Derive a new key and use for encryption
            DerivedKeyToken dkt = addDerivedKeyToken(c,
                                                     securityHeader,
                                                     xencDesiredNextSibling,
                                                     KeyInfoDetails.makeUriReference(getOrCreateWsuId(c, addedEncKey, null), SoapConstants.VALUETYPE_ENCRYPTED_KEY),
                                                     getKeyLengthInBytesForAlgorithm(addedEncKeyXmlEncKey.getAlgorithm()),
                                                     addedEncKeyXmlEncKey.getSecretKey().getEncoded(),
                                                     "DerivedKey");
            String dktId = getOrCreateWsuId(c, dkt.dkt, "DerivedKey-Enc");
            XencUtil.XmlEncKey dktEncKey = new XencUtil.XmlEncKey(addedEncKeyXmlEncKey.getAlgorithm(), dkt.derivedKey);
            if (c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2)) {
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          xencDesiredNextSibling,
                                          elementsToEncrypt,
                                          dktEncKey,
                                          KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
            } else {
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          xencDesiredNextSibling,
                                          elementsToEncrypt,
                                          dktEncKey,
                                          KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY));
            }
        } else {
            // Encrypt using the EncryptedKey we already added
            String encKeyId = getOrCreateWsuId(c, addedEncKey, null);
            addEncryptedReferenceList(c,
                                      addedEncKey,
                                      xencDesiredNextSibling,
                                      elementsToEncrypt,
                                      addedEncKeyXmlEncKey,
                                      KeyInfoDetails.makeUriReference(encKeyId, SoapConstants.VALUETYPE_ENCRYPTED_KEY));
        }
    }

    private void encryptWithSecureConversationToken( final Context c,
                                                     final Element securityHeader,
                                                     final Element xencDesiredNextSibling,
                                                     final Element sct,
                                                     final DecorationRequirements.SecureConversationSession session,
                                                     final Element[] elementsToEncrypt ) throws DecoratorException, GeneralSecurityException {
        // Encrypt using Secure Conversation session
        if (session == null)
            throw new DecoratorException("Encryption is requested with SecureConversationSession, but session is null");
        DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c, securityHeader, xencDesiredNextSibling, session, sct);
        XencUtil.XmlEncKey encKey;
        if (derivedKeyToken.derivedKey.length == 32) {
            encKey = new XencUtil.XmlEncKey(XencUtil.AES_256_CBC, derivedKeyToken.derivedKey);
        } else if (derivedKeyToken.derivedKey.length == 16) {
            encKey = new XencUtil.XmlEncKey(XencUtil.AES_128_CBC, derivedKeyToken.derivedKey);
        } else {
            throw new DecoratorException("unexpected key length");
        }

        String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, null);
        if (derivedKeyToken.derivedKey.length == 32) {
            addEncryptedReferenceList(c,
                                      securityHeader,
                                      xencDesiredNextSibling,
                                      elementsToEncrypt,
                                      encKey,
                                      KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
        } else {
            addEncryptedReferenceList(c,
                                      securityHeader,
                                      xencDesiredNextSibling,
                                      elementsToEncrypt,
                                      encKey,
                                      KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY));
        }
    }

    private SignatureInfo processGeneratedEncryptedKeySigningToken( final Context c,
                                                                    final Element securityHeader,
                                                                    final Set<Element> signList,
                                                                    final Element[] addedEncKeyHolder,
                                                                    final XencUtil.XmlEncKey[] addedEncKeyXmlEncKeyHolder ) throws GeneralSecurityException, DecoratorException {
        final SignatureInfo signatureInfo;

        // create a new EncryptedKey and sign with that
        final String encryptionAlgorithm = c.dreq.getEncryptionAlgorithm();

        // If we've been given a secret key to use, use it instead of making a new one
        final XencUtil.XmlEncKey addedEncKeyXmlEncKey;
        if (c.dreq.getEncryptedKey() != null) {
            addedEncKeyXmlEncKey = new XencUtil.XmlEncKey(encryptionAlgorithm, c.dreq.getEncryptedKey());
        } else {
            addedEncKeyXmlEncKey = generateXmlEncKey(encryptionAlgorithm);
        }
        addedEncKeyXmlEncKeyHolder[0] = addedEncKeyXmlEncKey;

        final Element addedEncKey = addEncryptedKey(c,
                                      securityHeader,
                                      c.dreq.getRecipientCertificate(),
                                      new Element[0],
                                      addedEncKeyXmlEncKey,
                                      c.dreq.getKeyEncryptionAlgorithm(),
                                      null);
        addedEncKeyHolder[0] = addedEncKey;

        String encKeyId = getOrCreateWsuId(c, addedEncKey, null);
        if (c.dreq.isProtectTokens() && !signList.isEmpty())
            signList.add(addedEncKey);

        if (c.dreq.isUseDerivedKeys()) {
            // Derive a new key for signing
            DerivedKeyToken derivedKeyToken =
                    addDerivedKeyToken(c,
                                       securityHeader,
                                       null,
                                       KeyInfoDetails.makeUriReference(encKeyId, SoapConstants.VALUETYPE_ENCRYPTED_KEY),
                                       getKeyLengthInBytesForAlgorithm(c.dreq.getEncryptionAlgorithm()),
                                       addedEncKeyXmlEncKey.getSecretKey().getEncoded(),
                                       "DerivedKey");
            String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
            String valueType = c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2) ?
                SoapConstants.VALUETYPE_DERIVEDKEY2 :
                SoapConstants.VALUETYPE_DERIVEDKEY;

            signatureInfo = new SignatureInfo(
                new XencUtil.XmlEncKey(c.dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey(),
                KeyInfoDetails.makeUriReference(dktId, valueType ));

            maybeSignDerivedKeyToken(signList, c.dreq, derivedKeyToken);
        } else {
            // No derived key -- use the raw EncryptedKey directly
            signatureInfo = new SignatureInfo(
                addedEncKeyXmlEncKey.getSecretKey(),
                KeyInfoDetails.makeUriReference(encKeyId, SoapConstants.VALUETYPE_ENCRYPTED_KEY) );
        }

        return signatureInfo;
    }

    private SignatureInfo processSamlSigningToken( final DecorationRequirements dreq,
                                                   final Set<Element> signList,
                                                   final SamlAssertion saml,
                                                   final Element samlElement ) throws InvalidDocumentFormatException, DecoratorException {
        final SignatureInfo signatureInfo;// sign with SAML token
        final boolean saml11 = SamlConstants.NS_SAML.equals(samlElement.getNamespaceURI());
        final String assId = saml11 ?
                samlElement.getAttribute("AssertionID") :
                samlElement.getAttribute("ID");
        if (assId == null || assId.length() < 1)
            throw new InvalidDocumentFormatException("Unable to decorate: SAML Assertion has missing or empty AssertionID/ID");

        String samlValueType = saml11 ?
            SoapConstants.VALUETYPE_SAML_ASSERTIONID2 :
            SoapConstants.VALUETYPE_SAML_ASSERTIONID3;

        final KeyInfoDetails signatureKeyInfo;
        if ( SyspropUtil.getBoolean(PROPERTY_SAML_USE_URI_REF) ) {
            signatureKeyInfo = KeyInfoDetails.makeUriReference(assId, samlValueType);
        } else {
            signatureKeyInfo = KeyInfoDetails.makeKeyId(assId, false, samlValueType);
        }

        signatureInfo = new SignatureInfo(
            saml.getMessageSigningCertificate(),
            dreq.getSenderMessageSigningPrivateKey(),
            signatureKeyInfo );

        if (signatureInfo.senderSigningKey == null)
            throw new DecoratorException("Signing is requested with saml:Assertion, but senderPrivateKey is null");

        if (dreq.isProtectTokens() && !signList.isEmpty())
            signList.add(samlElement);
        return signatureInfo;
    }

    private SignatureInfo processKerberosSHA1SigningToken( final Context c,
                                                           final Element securityHeader,
                                                           final Set<Element> signList ) throws NoSuchAlgorithmException, InvalidKeyException, DecoratorException {
        final SignatureInfo signatureInfo;// Derive key from kerberos session referenced using KerberosSHA1
        c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
        String kerbSha1 = KerberosUtils.getBase64Sha1(c.dreq.getKerberosTicket().getGSSAPReqTicket());
        KeyInfoDetails kerbShaRef = KeyInfoDetails.makeKeyId(kerbSha1, true, SoapConstants.VALUETYPE_KERBEROS_APREQ_SHA1);
        final DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c,
                                                                   securityHeader,
                                                                   null,
                                                                   kerbShaRef,
                                                                   getKeyLengthInBytesForAlgorithm(c.dreq.getEncryptionAlgorithm()),
                                                                   c.dreq.getKerberosTicket().getKey(),
                                                                   "DerivedKey");
        String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
        signatureInfo = new SignatureInfo(
            new XencUtil.XmlEncKey(c.dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey(),
            KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2) );
        maybeSignDerivedKeyToken(signList, c.dreq, derivedKeyToken);
        return signatureInfo;
    }

    private SignatureInfo processKerberosSigningToken( final Context c,
                                                       final Element securityHeader,
                                                       final Set<Element> signList,
                                                       final Element addedKerberosBst ) throws DecoratorException, NoSuchAlgorithmException, InvalidKeyException {
        final SignatureInfo signatureInfo;// Derive key from kerberos session key using direct URI reference
        assert c.dreq.getKerberosTicket() != null;
        c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
        KeyInfoDetails kerbUriRef = KeyInfoDetails.makeUriReference(
                getOrCreateWsuId(c, addedKerberosBst, null),
                SoapConstants.VALUETYPE_KERBEROS_GSS_AP_REQ);
        final DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c,
                                                                   securityHeader,
                                                                   null,
                                                                   kerbUriRef,
                                                                   getKeyLengthInBytesForAlgorithm(c.dreq.getEncryptionAlgorithm()),
                                                                   c.dreq.getKerberosTicket().getKey(),
                                                                   "DerivedKey");
        String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
        signatureInfo = new SignatureInfo(
            new XencUtil.XmlEncKey(c.dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey(),
            KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2) );
        maybeSignDerivedKeyToken(signList, c.dreq, derivedKeyToken);
        return signatureInfo;
    }

    private SignatureInfo processEncryptedKeySigningToken( final Context c,
                                                           final Element securityHeader,
                                                           final Set<Element> signList ) throws NoSuchAlgorithmException, InvalidKeyException, DecoratorException {
        final SignatureInfo signatureInfo;// Use a reference to an implicit EncryptedKey that the recipient is assumed to already possess
        // (possibly because we got it from them originally)
        if (c.dreq.isUseDerivedKeys()) {
            // Derive a key from the implicit ephemeral key
            DerivedKeyToken derivedKeyToken =
                    addDerivedKeyToken(c,
                                       securityHeader,
                                       null,
                                       KeyInfoDetails.makeEncryptedKeySha1Ref(c.dreq.getEncryptedKeySha1()),
                                       getKeyLengthInBytesForAlgorithm(c.dreq.getEncryptionAlgorithm()),
                                       c.dreq.getEncryptedKey(),
                                       "DerivedKey");
            String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
            String valueType = c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2) ?
                    SoapConstants.VALUETYPE_DERIVEDKEY2 :
                    SoapConstants.VALUETYPE_DERIVEDKEY;

            signatureInfo = new SignatureInfo(
                new XencUtil.XmlEncKey(c.dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey(),
                KeyInfoDetails.makeUriReference(dktId, valueType) );

            maybeSignDerivedKeyToken(signList, c.dreq, derivedKeyToken);
        } else {
            // Use the implicit ephemeral key directly
            signatureInfo = new SignatureInfo(
                new SecretKeySpec(c.dreq.getEncryptedKey(), "SHA1"),
                KeyInfoDetails.makeEncryptedKeySha1Ref(c.dreq.getEncryptedKeySha1()) );
        }
        return signatureInfo;
    }

    private SignatureInfo processSecureConversationSigningToken( final DecorationRequirements dreq,
                                                                 final Context c,
                                                                 final Element securityHeader,
                                                                 final Set<Element> signList,
                                                                 final Element sct,
                                                                 final DecorationRequirements.SecureConversationSession session ) throws DecoratorException, NoSuchAlgorithmException, InvalidKeyException {
        final SignatureInfo signatureInfo;
        if (session == null)
            throw new DecoratorException("Signing is requested with SecureConversationSession, but session is null");
        DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c, securityHeader, null, session, sct);
        String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
        String valueType = c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2) ?
                SoapConstants.VALUETYPE_DERIVEDKEY2 :
                SoapConstants.VALUETYPE_DERIVEDKEY;

        maybeSignDerivedKeyToken(signList, dreq, derivedKeyToken);

        signatureInfo = new SignatureInfo(
            new AesKey(derivedKeyToken.derivedKey, derivedKeyToken.derivedKey.length * 8),
            KeyInfoDetails.makeUriReference(dktId, valueType) );
        return signatureInfo;
    }

    /**
     * If there are any L7a or WSA headers in the message, and we are signing anything else, then sign them too
     */
    private void addAddressingElements( final Document soapMsg, final Set<Element> signList ) throws InvalidDocumentFormatException {
        {
            Element messageId = SoapUtil.getL7aMessageIdElement(soapMsg);
            if (messageId != null && !signList.isEmpty())
                signList.add(messageId);
            Element relatesTo = SoapUtil.getL7aRelatesToElement(soapMsg);
            if (relatesTo != null && !signList.isEmpty())
                signList.add(relatesTo);
        }
        {
            Element messageId = SoapUtil.getWsaMessageIdElement(soapMsg, null);
            if (messageId != null && !signList.isEmpty())
                signList.add(messageId);
            Element relatesTo = SoapUtil.getWsaRelatesToElement(soapMsg);
            if (relatesTo != null && !signList.isEmpty())
                signList.add(relatesTo);
        }
    }

    private Boolean getMustUnderstand( final DecorationRequirements dreq ) {
        Boolean mustUnderstand = true;

        if ( dreq.getMustUnderstand() != null ) {
            switch (dreq.getMustUnderstand()) {
                case DEFAULT:
                    mustUnderstand = SoapUtil.isSecHdrDefaultsToMustUnderstand();
                    break;
                case NO:
                    mustUnderstand = false;
                    break;
                case SKIP:
                    mustUnderstand = null;
                    break;
                case YES:
                    mustUnderstand = true;
                    break;
            }
        }

        return mustUnderstand;
    }

    private SignatureInfo buildX509SignatureInfo( final DecorationRequirements dreq,
                                                  final Pair<X509Certificate, KeyInfoDetails> senderCertKeyInfo ) throws DecoratorException {
        final SignatureInfo signatureInfo = new SignatureInfo(
                senderCertKeyInfo.left,
                dreq.getSenderMessageSigningPrivateKey(),
                senderCertKeyInfo.right );

        if (signatureInfo.senderSigningKey == null)
            throw new DecoratorException("Signing is requested with sender cert, but senderPrivateKey is null");
        if (signatureInfo.senderSigningCert == null)
            throw new DecoratorException("Signing is requested with sender cert, but senderSigningCert is null");
        if (signatureInfo.signatureKeyInfo == null)
            throw new DecoratorException("Signing is requested with sender cert, but signatureKeyInfo is null");

        return signatureInfo;
    }

    private boolean isEncrypted(Set<Element> cryptList, Node node) {
        Node n = node;
        while(n != null) {
            if (cryptList.contains(n))
                return true;
            n = n.getParentNode();
        }
        return false;
    }

    private DecorationResult produceResult(final Context c) {
        return new DecorationResult() {
            private String encryptedKeySha1 = null;
            private String actor = null;

            @Override
            public String getEncryptedKeySha1() {
                if (encryptedKeySha1 != null)
                    return encryptedKeySha1;
                if (c.lastEncryptedKeyBytes == null)
                    return null;
                return encryptedKeySha1 = XencUtil.computeEncryptedKeySha1(c.lastEncryptedKeyBytes);
            }

            @Override
            public SecretKey getEncryptedKeySecretKey() {
                return c.lastEncryptedKeySecretKey;
            }

            @Override
            public Map<String, Boolean> getSignatures() {
                return c.signatures;
            }

            @Override
            public Set<String> getEncryptedSignatureValues() {
                return c.encryptedSignatures;
            }

            @Override
            public String getSecurityHeaderActor() {
                if (actor == null) {
                    actor = c.dreq.getSecurityHeaderActor();
                }
                return actor;
            }

            @Override
            public void setSecurityHeaderActor(String newActor) {
                actor = newActor;
            }
        };
    }

    private void maybeSignDerivedKeyToken(Set<Element> signList, DecorationRequirements dreq, DerivedKeyToken derivedKeyToken) {
        if (dreq.isProtectTokens() && !signList.isEmpty() && PROTECTTOKENS_SIGNS_DERIVED_KEYS)
            signList.add(derivedKeyToken.dkt);
    }

    private Element addSignatureConfirmation(Element securityHeader, String signatureConfirmation) {
        String wsse11Ns = SoapConstants.SECURITY11_NAMESPACE;
        Element sc = DomUtils.createAndAppendElementNS(securityHeader, "SignatureConfirmation", wsse11Ns, "wsse11");
        if (signatureConfirmation != null)
            sc.setAttribute("Value", signatureConfirmation);
        return sc;
    }

    private Element addSamlSecurityToken(Element securityHeader, Element senderSamlToken) {
        Document factory = securityHeader.getOwnerDocument();
        Element saml;
        if (senderSamlToken.getOwnerDocument() == factory)
            saml = senderSamlToken;
        else
            saml = (Element)factory.importNode(senderSamlToken, true);
        securityHeader.appendChild(saml);
        return saml;
    }

    private static class DerivedKeyToken {
        Element dkt;
        byte[] derivedKey;

        DerivedKeyToken(Element dkt, byte[] derivedKey) {
            this.dkt = dkt;
            this.derivedKey = derivedKey;
        }
    }

    /**
     * Create a new DerivedKeyToken derived from a WS-SecureConversation session.
     *
     * @param c                     decoration context.  Must not be null.
     * @param securityHeader        security header being created.  Must not be null.
     * @param desiredNextSibling    next sibling, or null to append new element to securityHeader
     * @param session               WS-SC session to use.  Must not be null.
     * @return the newly-added DerivedKeyToken.  Never null.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private DerivedKeyToken addDerivedKeyToken(Context c,
                                               Element securityHeader,
                                               Element desiredNextSibling,
                                               DecorationRequirements.SecureConversationSession session,
                                               Element securityContextToken)
      throws DecoratorException, NoSuchAlgorithmException, InvalidKeyException
    {
        // fla 18 Aug, 2004
        // NOTE This method of reffering to the SCT uses a Reference URI that contains the Identifier value
        // instead of the actual #wsuid of the SCT.
        // We do this for better interop with .net clients (WSE 2.0)
        // we may want to support different methods based on the user agent
        // the alternative would be : ref.setAttribute("URI", "#" + getOrCreateWsuId(c, sct, null));
        final byte[] derivationSourceSecretKey = session.getSecretKey();

        if (c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2)) {
            final String derivationSourceUri = getOrCreateWsuId(c, securityContextToken, "SecurityContextToken");
            final String derivationSourceValueType = SoapConstants.VALUETYPE_SECURECONV2;
            return addDerivedKeyToken(c,
                                  securityHeader,
                                  desiredNextSibling,
                                  KeyInfoDetails.makeUriReference(derivationSourceUri, derivationSourceValueType),
                                  NEW_DERIVED_KEY_LENGTH,
                                  derivationSourceSecretKey,
                                  "WS-SecureConversation");
        } else {
            final String derivationSourceUri = session.getId();
            final String derivationSourceValueType = SoapConstants.VALUETYPE_SECURECONV;
            return addDerivedKeyToken(c,
                                  securityHeader,
                                  desiredNextSibling,
                                  KeyInfoDetails.makeUriReferenceRaw(derivationSourceUri, derivationSourceValueType),
                                  OLD_DERIVED_KEY_LENGTH,
                                  derivationSourceSecretKey,
                                  "WS-SecureConversation");
        }
    }

    /**
     * Create a new DerivedKeyToken derived from a WS-SecureConversation session.
     *
     * @param c                     decoration context.  Must not be null.
     * @param securityHeader        security header being created.  Must not be null.
     * @param desiredNextSibling    next sibling, or null to append new element to securityHeader
     * @param keyInfoDetail         info for SecurityTokenReference referring back to the derivation source.  Must not be null.
     * @param derivationSourceSecretKey  raw bytes of secret key material from which to derive a new key.  Must not be null or empty.
     * @param derivationLabel            the string to use as the Label parameter in key derivation.  Must not be null or empty.
     * @return the newly-added DerivedKeyToken.  Never null.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private DerivedKeyToken addDerivedKeyToken(Context c,
                                               Element securityHeader,
                                               Element desiredNextSibling,
                                               KeyInfoDetails keyInfoDetail,
                                               int length,
                                               byte[] derivationSourceSecretKey,
                                               String derivationLabel)
            throws NoSuchAlgorithmException, InvalidKeyException
    {
        NamespaceFactory namespaceFactory = c.nsf;
        Document factory = securityHeader.getOwnerDocument();
        String wsseNs = securityHeader.getNamespaceURI();
        String wsse = securityHeader.getPrefix();
        if (wsse == null) wsse = "wsse";
        boolean isWse3 = SoapConstants.WSSC_NAMESPACE2.equals(namespaceFactory.getWsscNs());

        Element dkt;
        if (desiredNextSibling == null)
            dkt = DomUtils.createAndAppendElementNS(securityHeader,
                                                   SoapConstants.WSSC_DK_EL_NAME,
                                                   namespaceFactory.getWsscNs(),
                                                   "wssc");
        else
            dkt = DomUtils.createAndInsertBeforeElementNS(desiredNextSibling,
                                                         SoapConstants.WSSC_DK_EL_NAME,
                                                         namespaceFactory.getWsscNs(),
                                                         "wssc");
        String wssc = dkt.getPrefix() == null ? "" : dkt.getPrefix() + ":";
        if(isWse3) {
            dkt.setAttribute("Algorithm", SoapConstants.ALGORITHM_PSHA2); // WSE 3.0 does not use an attribute NS
        }
        else {
            dkt.setAttributeNS(namespaceFactory.getWsscNs(), wssc + "Algorithm", SoapConstants.ALGORITHM_PSHA);
        }

        keyInfoDetail.populateExistingKeyInfoElement(c.nsf, dkt);

        // Gather derived key params
        byte[] nonce = new byte[length];
        rand.nextBytes(nonce);

        // Encode derived key params for the recipient
        Element generationEl = DomUtils.createAndAppendElementNS(dkt, "Generation", namespaceFactory.getWsscNs(), "wssc");
        generationEl.appendChild(DomUtils.createTextNode(factory, "0"));
        Element lengthEl = DomUtils.createAndAppendElementNS(dkt, "Length", namespaceFactory.getWsscNs(), "wssc");
        lengthEl.appendChild(DomUtils.createTextNode(factory, Integer.toString(length)));
        Element labelEl = DomUtils.createAndAppendElementNS(dkt, "Label", namespaceFactory.getWsscNs(), "wssc");
        labelEl.appendChild(DomUtils.createTextNode(factory, derivationLabel));
        Element nonceEl = isWse3 ? DomUtils.createAndAppendElementNS(dkt, "Nonce", namespaceFactory.getWsscNs(), "wssc")
                                 : DomUtils.createAndAppendElementNS(dkt, "Nonce", wsseNs, wsse);
        nonceEl.appendChild(DomUtils.createTextNode(factory, HexUtils.encodeBase64(nonce, true)));

        // Derive a copy of the key for ourselves
        byte[] seed = new byte[derivationLabel.length() + nonce.length];
        System.arraycopy(derivationLabel.getBytes(), 0, seed, 0, derivationLabel.length());
        System.arraycopy(nonce, 0, seed, derivationLabel.length(), nonce.length);
        byte[] derivedKey = new SecureConversationKeyDeriver().pSHA1(derivationSourceSecretKey, seed, length);

        return new DerivedKeyToken(dkt, derivedKey);
    }

    private Element addSecurityContextToken(Context c, Element securityHeader, String id) {
        NamespaceFactory namespaceFactory = c.nsf;
        Element sct = DomUtils.createAndAppendElementNS(securityHeader,
                                                       SoapConstants.SECURITY_CONTEXT_TOK_EL_NAME,
                                                       namespaceFactory.getWsscNs(),
                                                       "wssc");
        Element identifier = DomUtils.createAndAppendElementNS(sct,
                                                              "Identifier",
                                                              namespaceFactory.getWsscNs(),
                                                              "wssc");
        identifier.appendChild(DomUtils.createTextNode(identifier, id));
        return sct;
    }

    /**
     * Add a ds:Signature element to the Security header and return it.
     *
     * @param c                      the processing context.  Must not be null.
     * @param senderSigningKey       the Key that should be used to compute the signature.  May be RSA public or private key, or an AES symmetric key.
     * @param senderSigningCert      if X509 certificate associated with senderSigningKey, or null if senderSigningKey is not associated with a cert.
     *                               Must be provided if senderSigningKey is an RSA or DSA key.
     * @param messageDigestAlgorithm the message digest algorithm to use.  A null value means the default digest will be used for the signing key type.
     * @param elementsToSign         an array of elements that should be signed.  Must be non-null references to elements in the Document being processed.  Must not be null or empty.
     * @param securityHeader         the Security header to which the new ds:Signature element should be added.  May not be null.
     * @param keyInfoDetails         the KeyInfoDetails to use to create the KeyInfo.  Must not be null.
     * @param suppressSamlStrDereference true if Signature references to SAML Assertions should be generated using
     *                                   non-standards-compliant direct wsu:Ids instead of using the standards-compliant
     *                                   STR Dereference Transform.
     *
     * @return the ds:Signature element, which has already been appended to the Security header.
     *
     * @throws DecoratorException             if the signature could not be created with this message and these decoration requirements.
     * @throws InvalidDocumentFormatException if the message format is too invalid to overlook.
     */
    private Element addSignature(final Context c,
                                 Key senderSigningKey,
                                 X509Certificate senderSigningCert,
                                 String messageDigestAlgorithm,
                                 Element[] elementsToSign,
                                 String[] partsToSign,
                                 boolean signPartHeaders,
                                 Element securityHeader,
                                 KeyInfoDetails keyInfoDetails,
                                 boolean suppressSamlStrDereference)
            throws DecoratorException, InvalidDocumentFormatException, IOException, CertificateEncodingException {
        final String DS_PREFIX = "ds";

        if (elementsToSign == null || elementsToSign.length < 1) return null;

        final Document domFactory = securityHeader.getOwnerDocument();
        Element keyInfoElement = keyInfoDetails.createKeyInfoElement(domFactory, c.nsf, DS_PREFIX);
        boolean signingKeyInfoWithStrXform = false;

        Element keyInfoStr = null;
        if (c.dreq.isProtectTokens() && keyInfoDetails.isX509ValueReference()) {
            // KeyInfo is valueref to X.509.  We must include the KeyInfo in the signature with an STRTransform so that a virtual
            // BST gets included in the signature representing the signing X.509 token.
            int num = elementsToSign.length;
            Element[] newElementsToSign = new Element[num + 1];
            System.arraycopy(elementsToSign, 0, newElementsToSign, 0, num);
            keyInfoStr = DomUtils.findOnlyOneChildElementByName(keyInfoElement, securityHeader.getNamespaceURI(), SoapConstants.SECURITYTOKENREFERENCE_EL_NAME);
            if (keyInfoStr != null) {
                newElementsToSign[num] = keyInfoStr;
                elementsToSign = newElementsToSign;
                signingKeyInfoWithStrXform = true;
            }
        }

        // make sure all elements already have an id
        final int numToSign = elementsToSign.length;
        String[] signedIds = new String[numToSign];
        for (int i = 0; i < numToSign; i++) {
            Element eleToSign = elementsToSign[i];
            if (!suppressSamlStrDereference && "Assertion".equals(eleToSign.getLocalName()) &&
                 (SamlConstants.NS_SAML2.equals(eleToSign.getNamespaceURI()) ||
                  SamlConstants.NS_SAML.equals(eleToSign.getNamespaceURI()))) {
                // use STR-Transform for SAML assertions
                continue;
            }
            signedIds[i] = getOrCreateWsuId(c, eleToSign, null);
        }

        SupportedSignatureMethods signaturemethod;
        if (senderSigningKey instanceof RSAPrivateKey || "RSA".equals(senderSigningKey.getAlgorithm())) {
            if ("SHA-256".equals(messageDigestAlgorithm))
                signaturemethod = SupportedSignatureMethods.RSA_SHA256;
            else
                signaturemethod = SupportedSignatureMethods.RSA_SHA1;

        } else if (senderSigningKey instanceof ECPrivateKey || "EC".equals(senderSigningKey.getAlgorithm()) || "ECDSA".equals(senderSigningKey.getAlgorithm())) {
            if ("SHA-256".equals(messageDigestAlgorithm))
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA256;
            else if ("SHA-512".equals(messageDigestAlgorithm))
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA512;
            else if ("SHA-1".equals(messageDigestAlgorithm))
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA1;
            else
                // SHA-384 is the chosen default for GD
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA384;

        } else if (senderSigningKey instanceof DSAPrivateKey) {
            signaturemethod = SupportedSignatureMethods.DSA_SHA1;
        } else if (senderSigningKey instanceof SecretKey) {
            signaturemethod = SupportedSignatureMethods.HMAC_SHA1;
        } else {
            throw new DecoratorException("Private Key type not supported " +
                    senderSigningKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementsToSign[0].getOwnerDocument(),
                                                           signaturemethod.getMessageDigestIdentifier(),
                                                           Canonicalizer.EXCLUSIVE,
                                                           signaturemethod.getAlgorithmIdentifier());
        template.setIndentation(false);
        template.setPrefix(DS_PREFIX);
        final Map<Node,Node> strTransformsNodeToNode = new HashMap<Node, Node>();
        for (int i = 0; i < numToSign; i++) {
            final Element element = elementsToSign[i];
            final String id = signedIds[i];

            boolean addedCanon = false;
            final Reference ref;
            if (!suppressSamlStrDereference && "Assertion".equals(element.getLocalName()) &&
                 (SamlConstants.NS_SAML2.equals(element.getNamespaceURI()) || SamlConstants.NS_SAML.equals(element.getNamespaceURI()))) {
                // Bug #1434 -- unable to refer to SAML assertion directly using its AssertionID -- need intermediate STR with wsu:Id
                final boolean saml11 = SamlConstants.NS_SAML.equals(element.getNamespaceURI());
                final String assId = saml11 ?
                        element.getAttribute("AssertionID") :
                        element.getAttribute("ID");
                if (assId == null || assId.length() < 1)
                    throw new InvalidDocumentFormatException("Unable to decorate: SAML Assertion has missing or empty AssertionID/ID");
                String samlValueType = saml11 ?
                    SoapConstants.VALUETYPE_SAML_ASSERTIONID2 :
                    SoapConstants.VALUETYPE_SAML_ASSERTIONID3;
                Element str = addSamlSecurityTokenReference(securityHeader, assId, samlValueType);
                ref = template.createReference("#" + getOrCreateWsuId(c, str, "SamlSTR"));
                Element strTransform = createStrTransform(c, domFactory, DS_PREFIX);
                ref.addTransform(strTransform);
                addedCanon = true;
                strTransformsNodeToNode.put(str, element);
            } else if (signingKeyInfoWithStrXform && keyInfoStr == element) {
                // Signing the SecurityTokenReference inside the KeyInfo, and we already know we'll need STRTransform
                ref = template.createReference("#" + id);
                Element strTransform = createStrTransform(c, domFactory, DS_PREFIX);
                ref.addTransform(strTransform);
                addedCanon = true;
                final X509SigningSecurityTokenImpl bst = X509BinarySecurityTokenImpl.createBinarySecurityToken(domFactory,
                        c.dreq.getSenderMessageSigningCertificate(), securityHeader.getPrefix(), securityHeader.getNamespaceURI());
                strTransformsNodeToNode.put(keyInfoStr, bst.asElement());
            } else
                ref = template.createReference("#" + id);

            if (DomUtils.isElementAncestor(securityHeader, element)) {
                logger.fine("Per policy, breaking Basic Security Profile rules with enveloped signature" +
                        " of element " + element.getLocalName() + " with Id=\"" + id + "\"");
                ref.addTransform(Transform.ENVELOPED);
            }

            // Note that c14n is not required when using STR-Transform, this can be removed
            // once 4.0 is the earliest version in use.
            if (!addedCanon)
                ref.addTransform(Transform.C14N_EXCLUSIVE);
            template.addReference(ref);
        }
        for (final String partIdentifier : partsToSign) {
            final String id = "cid:" + partIdentifier;

            final Reference ref = template.createReference(id);
            if (signPartHeaders)
                ref.addTransform( SoapConstants.TRANSFORM_ATTACHMENT_COMPLETE);
            else
                ref.addTransform( SoapConstants.TRANSFORM_ATTACHMENT_CONTENT);
            template.addReference(ref);
        }
        Element emptySignatureElement = template.getSignatureElement();

        // Ensure that CanonicalizationMethod has required c14n subelemen
        final Element signedInfoElement = template.getSignedInfoElement();
        NodeList transforms = signedInfoElement.getElementsByTagNameNS(signedInfoElement.getNamespaceURI(), "Transform");
        for (int i = 0; i < transforms.getLength(); ++i)
            if (Transform.C14N_EXCLUSIVE.equals(((Element)transforms.item(i)).getAttribute("Algorithm")))
                DsigUtil.addInclusiveNamespacesToElement((Element)transforms.item(i));

        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
            @Override
            public Element resolveID(Document doc, String s) {
                Element e = c.idToElementCache.get(s);
                if (e != null)
                    return e;
                e = SoapUtil.getElementByWsuId(doc, s);
                if (e != null)
                    c.idToElementCache.put(s, e);
                return e;
            }
        });
        sigContext.setEntityResolver(c.attachmentResolver);
        sigContext.setAlgorithmFactory(new WssProcessorAlgorithmFactory(strTransformsNodeToNode));
        try {
            KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, senderSigningCert, senderSigningKey);
            sigContext.sign(emptySignatureElement, senderSigningKey);
        } catch (KeyUsageException e) {
            throw new DecoratorException("Unable to create XML signature due to signing cert key usage restrictions: " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new DecoratorException("Unable to create XML signature: unable to parse signing cert: " + ExceptionUtils.getMessage(e), e);
        } catch (XSignatureException e) {
            String msg = e.getMessage();
            if (msg != null && msg.indexOf("Found a relative URI") >= 0)       // Bug #1209
                throw new InvalidDocumentFormatException("Unable to sign this message due to a relative namespace URI.", e);
            throw new DecoratorException(e);
        }

        Element signatureElement = (Element)securityHeader.appendChild(emptySignatureElement);
        signatureElement.appendChild(keyInfoElement);

        return signatureElement;
    }

    private Element createStrTransform(Context c, Document domFactory, String DS_PREFIX) {
        Element strTransform = domFactory.createElementNS( SoapConstants.DIGSIG_URI, "ds:Transform");
        strTransform.setAttribute("Algorithm", SoapConstants.TRANSFORM_STR);
        Element strParams = DomUtils.createAndAppendElementNS(strTransform, "TransformationParameters", c.nsf.getWsseNs(), "wsse");
        Element cannonParam = DomUtils.createAndAppendElementNS(strParams, "CanonicalizationMethod", SoapConstants.DIGSIG_URI, DS_PREFIX);
        cannonParam.setAttribute("Algorithm", Transform.C14N_EXCLUSIVE);
        return strTransform;
    }

    private Element addSamlSecurityTokenReference(Element securityHeader, String assertionId, String valueType) {
        String wsseNs = securityHeader.getNamespaceURI();
        String wsse = securityHeader.getPrefix();
        Element str = DomUtils.createAndAppendElementNS(securityHeader, SoapConstants.SECURITYTOKENREFERENCE_EL_NAME, wsseNs, wsse);
        Element keyid = DomUtils.createAndAppendElementNS(str, SoapConstants.KEYIDENTIFIER_EL_NAME, wsseNs, wsse);
        keyid.setAttribute("ValueType", valueType);
        keyid.appendChild(DomUtils.createTextNode(keyid, assertionId));
        return str;
    }

    private static Element createUsernameToken(Element securityHeader, UsernameToken ut) {
        // What this element looks like:
        // <wsse:UsernameToken>
        //    <wsse:Username>username</wsse:Username>
        //    <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">password</wsse:Password>
        // </wsse:UsernameToken>
        // create elements
        Element token = ut.asElement(securityHeader,                    
                                     securityHeader.getNamespaceURI(),
                                     securityHeader.getPrefix());
        securityHeader.appendChild(token);
        return token;
    }


    /**
     * Encrypts one or more document elements using a caller-supplied key (probaly from a DKT),
     * and appends a ReferenceList to the Security header before the specified desiredNextSibling element
     * (probably the Signature).
     */
    private Element addEncryptedReferenceList(Context c,
                                              Element newParent,
                                              Element desiredNextSibling,
                                              Element[] elementsToEncrypt,
                                              XencUtil.XmlEncKey encKey,
                                              KeyInfoDetails keyInfoDetails)
      throws GeneralSecurityException, DecoratorException {
        String xencNs = SoapConstants.XMLENC_NS;

        // Put the ReferenceList in the right place
        Element referenceList;
        if (desiredNextSibling == null) {
            referenceList = DomUtils.createAndAppendElementNS(newParent,
                SoapConstants.REFLIST_EL_NAME,
                xencNs, "xenc");
        } else {
            referenceList = DomUtils.createAndInsertBeforeElementNS(desiredNextSibling,
                SoapConstants.REFLIST_EL_NAME,
                xencNs, "xenc");
        }
        String xenc = referenceList.getPrefix();

        int numElementsEncrypted = 0;
        for (Element element : elementsToEncrypt) {
            if (DomUtils.elementIsEmpty(element)) {
                logger.fine("Element \"" + element.getNodeName() + "\" is empty; will not encrypt it");
                continue;
            }

            Element encryptedElement;
            try {
                encryptedElement = XencUtil.encryptElement(element, encKey);
            } catch (XencUtil.XencException e) {
                throw new DecoratorException(e.getMessage(), e);
            }

            Element dataReference = DomUtils.createAndAppendElementNS(referenceList, "DataReference", xencNs, xenc);
            dataReference.setAttribute("URI", "#" + getOrCreateWsuId(c, encryptedElement, element.getLocalName()));

            keyInfoDetails.createAndAppendKeyInfoElement(c.nsf, encryptedElement);

            numElementsEncrypted++;
        }

        if (numElementsEncrypted < 1) {
            // None of the elements needed to be encrypted.  Abort the addition of the ReferenceList.
            Node parent = referenceList.getParentNode();
            if (parent != null)
                parent.removeChild(referenceList);
            return null;
        }

        return referenceList;
    }

    /**
     * Encrypts one or more document elements using a recipient cert,
     * and appends an EncryptedKey to the Security header before the specified desiredNextSibling element
     * (probably the Signature).
     */
    private Element addEncryptedKey(Context c,
                                    Element securityHeader,
                                    X509Certificate recipientCertificate,
                                    Element[] elementsToEncrypt,
                                    XencUtil.XmlEncKey encKey,
                                    String algorithm,
                                    Element desiredNextSibling)
      throws GeneralSecurityException, DecoratorException {

        Document soapMsg = securityHeader.getOwnerDocument();

        String xencNs = SoapConstants.XMLENC_NS;

        // Put the encrypted key in the right place
        Element encryptedKey;
        if (desiredNextSibling == null) {
            encryptedKey = DomUtils.createAndAppendElementNS(securityHeader,
                SoapConstants.ENCRYPTEDKEY_EL_NAME,
                xencNs, "xenc");
        } else {
            encryptedKey = DomUtils.createAndInsertBeforeElementNS(desiredNextSibling,
                SoapConstants.ENCRYPTEDKEY_EL_NAME,
                xencNs, "xenc");
        }
        String xenc = encryptedKey.getPrefix();

        Element encryptionMethod = DomUtils.createAndAppendElementNS(encryptedKey, "EncryptionMethod", xencNs, xenc);

        byte[] recipSki = CertUtils.getSKIBytesFromCert(recipientCertificate);

        KeyInfoDetails keyInfo = recipSki != null
                ? KeyInfoDetails.makeKeyId(recipSki, SoapConstants.VALUETYPE_SKI)
                : KeyInfoDetails.makeKeyId(recipientCertificate.getEncoded(), SoapConstants.VALUETYPE_X509);
        keyInfo.createAndAppendKeyInfoElement(c.nsf, encryptedKey);

        Element cipherData = DomUtils.createAndAppendElementNS(encryptedKey, "CipherData", xencNs, xenc);
        Element cipherValue = DomUtils.createAndAppendElementNS(cipherData, "CipherValue", xencNs, xenc);
        final SecretKey secretKey = encKey.getSecretKey();
        c.lastEncryptedKeySecretKey = secretKey;
        final byte[] encryptedKeyBytes;
        if ( SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2.equals(algorithm)) {
            byte[] params = new byte[0];

            encryptionMethod.setAttribute("Algorithm", SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2);
            encryptedKeyBytes = XencUtil.encryptKeyWithRsaOaepMGF1SHA1(secretKey.getEncoded(),
                                              recipientCertificate,
                                              recipientCertificate.getPublicKey(),
                                              params);

            if (params.length > 0) {
                Element oaepParamsEle = DomUtils.createAndAppendElementNS(encryptionMethod, "OAEPparams", xencNs, xenc);
                oaepParamsEle.appendChild(DomUtils.createTextNode(oaepParamsEle, HexUtils.encodeBase64(params)));
            }

            Element digestMethodEle = DomUtils.createAndAppendElementNS(encryptionMethod, "DigestMethod", SoapConstants.DIGSIG_URI, "ds");
            digestMethodEle.setAttribute("Algorithm", SoapConstants.DIGSIG_URI+"sha1");
        } else {
            encryptionMethod.setAttribute("Algorithm", SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO);
            encryptedKeyBytes = XencUtil.encryptKeyWithRsaAndPad(secretKey.getEncoded(),
                                              recipientCertificate,
                                              recipientCertificate.getPublicKey());
        }
        c.lastEncryptedKeyBytes = encryptedKeyBytes;
        final String base64 = HexUtils.encodeBase64(encryptedKeyBytes, true);
        cipherValue.appendChild(DomUtils.createTextNode(soapMsg, base64));
        Element referenceList = DomUtils.createAndAppendElementNS(encryptedKey, SoapConstants.REFLIST_EL_NAME, xencNs, xenc);

        int numElementsEncrypted = 0;
        for (Element element : elementsToEncrypt) {
            if (DomUtils.elementIsEmpty(element)) {
                logger.fine("Element \"" + element.getNodeName() + "\" is empty; will not encrypt it");
                continue;
            }
            Element encryptedElement;
            try {
                encryptedElement = XencUtil.encryptElement(element, encKey);
            } catch (XencUtil.XencException e) {
                throw new DecoratorException(e.getMessage(), e);
            }

            Element dataReference = DomUtils.createAndAppendElementNS(referenceList, "DataReference", xencNs, xenc);
            dataReference.setAttribute("URI", "#" + getOrCreateWsuId(c, encryptedElement, element.getLocalName()));
            numElementsEncrypted++;
        }

        if (numElementsEncrypted < 1 && referenceList != null) {
            // Remove the empty reference list, but leave the EncryptedKey in place since it might be needed
            referenceList.getParentNode().removeChild(referenceList);
        }

        return encryptedKey;
    }

    /**
     * Get the wsu:Id for the specified element.  If it doesn't already have a wsu:Id attribute a new one
     * is created for the element.
     *
     * @param c
     * @param element
     * @param basename Optional.  If non-null, will be used as the start of the Id string
     */
    private String getOrCreateWsuId(Context c, Element element, String basename) throws DecoratorException {
        String id = SoapUtil.getElementWsuId(element, false);
        if (id == null) {
            id = createWsuId(c, element, basename == null ? element.getLocalName() : basename);
        }
        return id;
    }

    /**
     * Generate a wsu:Id for the specified element, adds it to the element, and returns it.
     * Uses the specified basename as the start of the Id.
     *
     * @param c
     * @param element
     */
    private String createWsuId(Context c, Element element, String basename) throws DecoratorException {
        byte[] randbytes = new byte[16];
        rand.nextBytes(randbytes);
        String id = basename + "-" + c.count++ + "-" + HexUtils.hexDump(randbytes);

        if (c.idToElementCache.get(id) != null)
            throw new DecoratorException("Duplicate wsu:ID generated: " + id); // can't happen

        c.idToElementCache.put(id, element);

        final String wsuNs = c.nsf.getWsuNs();
        SoapUtil.setWsuId(element, wsuNs, id);

        return id;
    }

    private AttachmentEntityResolver buildAttachmentEntityResolver(final Context context,
                                                                   final DecorationRequirements decorationRequirements)
            throws IOException {
        // only get MIME knob if we'll need it
        MimeKnob mimeKnob = decorationRequirements.getPartsToSign().isEmpty() ?
            null :
            context.message.getKnob(MimeKnob.class);

        PartIterator iterator = mimeKnob==null ? null : mimeKnob.getParts();

        return new AttachmentEntityResolver(iterator, XmlUtil.getXss4jEntityResolver());
    }

    private Element addX509BinarySecurityToken(Element securityHeader, X509Certificate certificate, Context c)
      throws CertificateEncodingException
    {
        Element element = DomUtils.createAndAppendElementNS(securityHeader,
                                                           SoapConstants.BINARYSECURITYTOKEN_EL_NAME,
                                                           securityHeader.getNamespaceURI(), "wsse");
        element.setAttribute("ValueType", c.nsf.getValueType( SoapConstants.VALUETYPE_X509, element));
        element.setAttribute("EncodingType", c.nsf.getEncodingType( SoapConstants.ENCODINGTYPE_BASE64BINARY, element));
        element.appendChild(DomUtils.createTextNode(element, HexUtils.encodeBase64(certificate.getEncoded(), true)));
        return element;
    }

    private Element addKerberosSecurityTokenReference(Element securityHeader, String kerberosTicketId) {
        String wsseNs = securityHeader.getNamespaceURI();
        String wsse = securityHeader.getPrefix();
        Element str = DomUtils.createAndAppendElementNS(securityHeader, SoapConstants.SECURITYTOKENREFERENCE_EL_NAME, wsseNs, wsse);
        Element keyid = DomUtils.createAndAppendElementNS(str, SoapConstants.KEYIDENTIFIER_EL_NAME, wsseNs, wsse);
        keyid.setAttribute("EncodingType", SoapConstants.ENCODINGTYPE_BASE64BINARY);
        keyid.setAttribute("ValueType", SoapConstants.VALUETYPE_KERBEROS_APREQ_SHA1);
        keyid.appendChild(DomUtils.createTextNode(keyid, kerberosTicketId));
        return str;
    }

    private Element addKerberosBinarySecurityToken(Element securityHeader, KerberosGSSAPReqTicket kerberosTicket) {
        Document factory = securityHeader.getOwnerDocument();
        Element element = factory.createElementNS(securityHeader.getNamespaceURI(),
            SoapConstants.BINARYSECURITYTOKEN_EL_NAME);
        element.setPrefix(securityHeader.getPrefix());

        element.setAttribute("ValueType", SoapConstants.VALUETYPE_KERBEROS_GSS_AP_REQ);
        element.setAttribute("EncodingType", SoapConstants.ENCODINGTYPE_BASE64BINARY);

        element.appendChild(DomUtils.createTextNode(factory, HexUtils.encodeBase64(kerberosTicket.toByteArray(), true)));
        securityHeader.appendChild(element);
        return element;
    }

    private Element createSecurityHeader(Document message,
                                         Context context,
                                         String actor,
                                         Boolean mustUnderstand,
                                         boolean useExisting)
            throws DecoratorException, InvalidDocumentFormatException
    {
        final Element resultSecurity;
        Element oldSecurity = SoapUtil.getSecurityElement(message, actor);

        if (oldSecurity != null) {
            if(!useExisting) {
                String error;
                if (actor != null) {
                    error = "This message already has a security header for actor " + actor;
                } else {
                    error = "This message already has a security header for the default actor";
                }
                throw new DecoratorException(error);
            }

            String activeWsuNS = DomUtils.findActiveNamespace(oldSecurity, SoapConstants.WSU_URIS_ARRAY);
            if(activeWsuNS==null) {
                DomUtils.getOrCreatePrefixForNamespace(oldSecurity, context.nsf.getWsuNs(), "wsu");
                activeWsuNS = context.nsf.getWsuNs();
            }

            // update context to reflect active namespaces
            context.nsf.setWsseNs(oldSecurity.getNamespaceURI());
            context.nsf.setWsuNs(activeWsuNS);

            resultSecurity = oldSecurity;
        }
        else {
            Element securityHeader;
            securityHeader = SoapUtil.makeSecurityElement(message, context.nsf.getWsseNs(), actor, mustUnderstand);
            // Make sure wsu is declared to save duplication
            DomUtils.getOrCreatePrefixForNamespace(securityHeader, context.nsf.getWsuNs(), "wsu");
            resultSecurity = securityHeader;
        }

        return resultSecurity;
    }

    /**
     * Create the <code>EncriptionKey</code> based un the xenc algorithm name and using random key material.
     *
     * @param xEncAlgorithm the algorithm name such as http://www.w3.org/2001/04/xmlenc#tripledes-cbc
     * @return the encription key instance holding the secret key and the algorithm name
     * @throws NoSuchAlgorithmException if the algorithm URI is not recognized, or
     *                                  if the provided keyBytes is too short for the specified algorithm
     */
    private XencUtil.XmlEncKey generateXmlEncKey(String xEncAlgorithm) throws NoSuchAlgorithmException {
        byte[] keyBytes = new byte[32]; // (max for aes 256)
        rand.nextBytes(keyBytes);
        XencUtil.XmlEncKey xek = new XencUtil.XmlEncKey(xEncAlgorithm, keyBytes);
        // Ensure algorithm and length are valid
        xek.getSecretKey();
        return xek;
    }

    private int getKeyLengthInBytesForAlgorithm(String xEncAlgorithm) {
        int length = 16;
        if (XencUtil.AES_128_CBC.equals(xEncAlgorithm)) {
            length = 16;
        } else if (XencUtil.AES_192_CBC.equals(xEncAlgorithm)) {
            length = 24;
        } else if (XencUtil.AES_256_CBC.equals(xEncAlgorithm)) {
            length = 32;
        } else if (XencUtil.TRIPLE_DES_CBC.equals(xEncAlgorithm)) {
            length = 24;
        }
        return length;
    }

    private static class SignatureInfo {
        private final X509Certificate senderSigningCert; // for key usage enforcement
        private final Key senderSigningKey;
        private final KeyInfoDetails signatureKeyInfo;

        SignatureInfo( final X509Certificate senderSigningCert,
                       final Key senderSigningKey,
                       final KeyInfoDetails signatureKeyInfo ) {
            this.senderSigningCert = senderSigningCert;
            this.senderSigningKey = senderSigningKey;
            this.signatureKeyInfo = signatureKeyInfo;
        }

        SignatureInfo( final Key senderSigningKey,
                       final KeyInfoDetails signatureKeyInfo ) {
            this( null, senderSigningKey, signatureKeyInfo );
        }
    }
}
