/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.security.xml.decorator;

import com.ibm.xml.dsig.*;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.security.keys.AesKey;
import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.util.*;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3.x2000.x09.xmldsig.X509IssuerSerialType;
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
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    private static final Logger logger = Logger.getLogger(WssDecorator.class.getName());

    public static final String PROPERTY_SUPPRESS_NANOSECONDS = "com.l7tech.server.timestamp.omitNanos";
    public static final String PROPERTY_SAML_USE_URI_REF = "com.l7tech.server.saml.useUriReference";

    public static final int TIMESTAMP_TIMOUT_MILLIS = 300000;
    private static final int NEW_DERIVED_KEY_LENGTH = 32;
    private static final int OLD_DERIVED_KEY_LENGTH = 16;

    private static Random random = new SecureRandom();

    public WssDecoratorImpl() {
    }

    private static class Context {
        SecureRandom rand = new SecureRandom();
        long count = 0;
        Message message;
        Map<String, Element> idToElementCache = new HashMap<String, Element>();
        NamespaceFactory nsf = new NamespaceFactory();
        byte[] lastEncryptedKeyBytes = null;
        SecretKey lastEncryptedKeySecretKey = null;
        AttachmentEntityResolver attachmentResolver;

        String getBase64EncodingTypeUri() {
            return SoapConstants.SECURITY_NAMESPACE.equals(nsf.getWsseNs())
                    ? SoapConstants.ENCODINGTYPE_BASE64BINARY
                    : SoapConstants.ENCODINGTYPE_BASE64BINARY_2;     // lyonsm: ??? what is this for?  It hardcodes wsse: prefix!
        }
    }

    /**
     * @return random extra microseconds to add to the timestamp to make it more unique, or zero to not bother.
     */
    private static long getExtraTime() {
        return SyspropUtil.getBoolean(PROPERTY_SUPPRESS_NANOSECONDS) ? -1L : (long)random.nextInt(1000000);
    }

    /**
     * Decorate a soap message with WSS style security.
     *
     * @param message the soap message to decorate
     */
    public DecorationResult decorateMessage(Message message, DecorationRequirements dreq)
      throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException, SAXException, IOException {
        final Context c = new Context();
        c.message = message;
        c.nsf = dreq.getNamespaceFactory();
        c.attachmentResolver = buildAttachmentEntityResolver(c, dreq);

        // get writeable document after getting MIME part iterator
        final Document soapMsg = message.getXmlKnob().getDocumentWritable();

        Element securityHeader = createSecurityHeader(soapMsg, c,
                dreq.getSecurityHeaderActor(), dreq.isSecurityHeaderReusable());
        Set<Element> signList = dreq.getElementsToSign();
        Set<Element> cryptList = dreq.getElementsToEncrypt();
        Set<String> signPartList = dreq.getPartsToSign();

        Element timestamp = null;
        int timeoutMillis = dreq.getTimestampTimeoutMillis();
        if (timeoutMillis < 1)
            timeoutMillis = TIMESTAMP_TIMOUT_MILLIS;
        if (dreq.isIncludeTimestamp()) {
            Date createdDate = dreq.getTimestampCreatedDate();
            // Have to add some uniqueness to this timestamp
            timestamp = SoapUtil.addTimestamp(securityHeader,
                c.nsf.getWsuNs(),
                createdDate, // null ok
                getExtraTime(),
                timeoutMillis);
        }

        // If we aren't signing the entire message, find extra elements to sign
        if (dreq.isSignTimestamp() || !signList.isEmpty() || !signPartList.isEmpty()) {
            if (timestamp == null)
                timestamp = SoapUtil.addTimestamp(securityHeader,
                    c.nsf.getWsuNs(),
                    dreq.getTimestampCreatedDate(), // null ok
                    getExtraTime(),
                    timeoutMillis);
            signList.add(timestamp);
        }

        Element xencDesiredNextSibling = null;
        if (dreq.getSignatureConfirmation() != null) {
            Element sc = addSignatureConfirmation(securityHeader, dreq.getSignatureConfirmation());
            signList.add(sc);
            xencDesiredNextSibling = sc;
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
                signList.add(usernameToken);
                cryptList.add(addedUsernameTokenHolder);
            }
        }

        Element saml = null;
        if (dreq.getSenderSamlToken() != null) {
            saml = addSamlSecurityToken(securityHeader, dreq.getSenderSamlToken());
            if (dreq.isIncludeSamlTokenInSignature()) {
                signList.add(saml);
            }
        }

        // If there are any L7a or WSA headers in the message, and we are signing anything else, then sign them too
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

        // Add sender cert
        // note [bugzilla #2551] dont include a x509 BST if this is gonna use a sc context
        // note [bugzilla #3907] dont include a x509 BST if using kerberos
        KeyInfoDetails senderCertKeyInfo = null;
        KeyInfoType keyInfoType = null;
        if (dreq.getSenderMessageSigningCertificate() != null &&
            !signList.isEmpty() &&
            dreq.getSecureConversationSession() == null &&
            dreq.getKerberosTicket() == null) {
            switch(dreq.getKeyInfoInclusionType()) {
                case CERT:
                    // Use keyinfo reference target of a BinarySecurityToken
                    Element x509Bst = addX509BinarySecurityToken(securityHeader, dreq.getSenderMessageSigningCertificate(), c);
                    String bstId = getOrCreateWsuId(c, x509Bst, null);
                    senderCertKeyInfo = KeyInfoDetails.makeUriReference(bstId, SoapConstants.VALUETYPE_X509);
                    break;
                case STR_SKI:
                    // Use keyinfo reference target of a SKI
                    X509Certificate senderCert = dreq.getSenderMessageSigningCertificate();
                    byte[] senderSki = CertUtils.getSKIBytesFromCert(senderCert);
                    if (senderSki == null) {
                        // Supposed to refer to sender cert by its SKI, but it has no SKI
                        throw new DecoratorException("suppressBst is requested, but the sender cert has no SubjectKeyIdentifier");
                    }
                    senderCertKeyInfo = KeyInfoDetails.makeKeyId(senderSki, SoapConstants.VALUETYPE_SKI);
                    break;
                case ISSUER_SERIAL:
                    KeyInfoType kit = KeyInfoType.Factory.newInstance();
                    X509DataType xdt = kit.addNewX509Data();
                    X509IssuerSerialType xist = xdt.addNewX509IssuerSerial();
                    xist.setX509IssuerName(dreq.getSenderMessageSigningCertificate().getIssuerDN().getName());
                    xist.setX509SerialNumber(dreq.getSenderMessageSigningCertificate().getSerialNumber());
                    keyInfoType = kit;
                    break;
                default:
                    throw new DecoratorException("Unsupported KeyInfoInclusionType: " + dreq.getKeyInfoInclusionType());
            }
        }

        // Add kerberos ticket reference
        if (dreq.isIncludeKerberosTicketId() && dreq.getKerberosTicketId() != null) {
            addKerberosSecurityTokenReference(securityHeader, dreq.getKerberosTicketId());
        }

        // Add Kerberos ticket
        Element addedKerberosBst = null;
        if (dreq.isIncludeKerberosTicket() && dreq.getKerberosTicket() != null) {
            addedKerberosBst = addKerberosBinarySecurityToken(securityHeader, dreq.getKerberosTicket().getGSSAPReqTicket());
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
        }

        final Element signature;
        Element addedEncKey = null;
        XencUtil.XmlEncKey addedEncKeyXmlEncKey = null;
        if (signList.size() > 0) {
            Key senderSigningKey;
            final KeyInfoDetails signatureKeyInfo;
            if (sct != null) {
                // No BST; must be WS-SecureConversation
                if (session == null)
                    throw new IllegalArgumentException("Signing is requested with SecureConversationSession, but session is null");
                DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c, securityHeader, null, session, sct);
                String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
                senderSigningKey = new AesKey(derivedKeyToken.derivedKey, derivedKeyToken.derivedKey.length * 8);
                if (c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2)) {
                    signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2);
                } else {
                    signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY);
                }
            } else if (dreq.getEncryptedKey() != null &&
                       dreq.getEncryptedKeySha1() != null)
            {
                // Use a reference to an implicit EncryptedKey that the recipient is assumed to already possess
                // (possibly because we got it from them originally)
                if (dreq.isUseDerivedKeys()) {
                    // Derive a key from the implicit ephemeral key
                    DerivedKeyToken derivedKeyToken =
                            addDerivedKeyToken(c,
                                               securityHeader,
                                               null,
                                               KeyInfoDetails.makeEncryptedKeySha1Ref(dreq.getEncryptedKeySha1()),
                                               getKeyLengthInBytesForAlgorithm(dreq.getEncryptionAlgorithm()),
                                               dreq.getEncryptedKey(),
                                               "DerivedKey");
                    senderSigningKey = new XencUtil.XmlEncKey(dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey();
                    String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
                    if (c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2)) {
                        signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2);
                    } else {
                        signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY);
                    }
                } else {
                    // Use the implicit ephemeral key directly
                    senderSigningKey = new SecretKeySpec(dreq.getEncryptedKey(), "SHA1");
                    signatureKeyInfo = KeyInfoDetails.makeEncryptedKeySha1Ref(dreq.getEncryptedKeySha1());
                }
            } else if (addedKerberosBst != null) {
                // Derive key from kerberos session key using direct URI reference
                assert dreq.getKerberosTicket() != null;
                c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
                KeyInfoDetails  kerbUriRef = KeyInfoDetails.makeUriReference(
                        getOrCreateWsuId(c, addedKerberosBst, null),
                        SoapConstants.VALUETYPE_KERBEROS_GSS_AP_REQ);
                final DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c,
                                                                           securityHeader,
                                                                           null,
                                                                           kerbUriRef,
                                                                           getKeyLengthInBytesForAlgorithm(dreq.getEncryptionAlgorithm()),
                                                                           dreq.getKerberosTicket().getKey(),
                                                                           "DerivedKey");
                senderSigningKey = new XencUtil.XmlEncKey(dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey();
                String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
                signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2);
            } else if (dreq.getKerberosTicket() != null) {
                // Derive key from kerberos session referenced using KerberosSHA1
                c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
                String kerbSha1 = KerberosUtils.getBase64Sha1(dreq.getKerberosTicket().getGSSAPReqTicket());
                KeyInfoDetails kerbShaRef = KeyInfoDetails.makeKeyId(kerbSha1, true, SoapConstants.VALUETYPE_KERBEROS_APREQ_SHA1);
                final DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c,
                                                                           securityHeader,
                                                                           null,
                                                                           kerbShaRef,
                                                                           getKeyLengthInBytesForAlgorithm(dreq.getEncryptionAlgorithm()),
                                                                           dreq.getKerberosTicket().getKey(),
                                                                           "DerivedKey");
                senderSigningKey = new XencUtil.XmlEncKey(dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey();
                String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
                signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2);
            } else if (senderCertKeyInfo != null || keyInfoType != null) {
                senderSigningKey = dreq.getSenderMessageSigningPrivateKey();
                if (senderSigningKey == null)
                    throw new IllegalArgumentException("Signing is requested with sender cert, but senderPrivateKey is null");
                signatureKeyInfo = senderCertKeyInfo;
            } else if (saml != null) {
                // sign with SAML token
                senderSigningKey = dreq.getSenderMessageSigningPrivateKey();
                if (senderSigningKey == null)
                    throw new IllegalArgumentException("Signing is requested with saml:Assertion, but senderPrivateKey is null");

                final boolean saml11 = SamlConstants.NS_SAML.equals(saml.getNamespaceURI());
                final String assId = saml11 ?
                        saml.getAttribute("AssertionID") :
                        saml.getAttribute("ID");
                if (assId == null || assId.length() < 1)
                    throw new InvalidDocumentFormatException("Unable to decorate: SAML Assertion has missing or empty AssertionID/ID");

                String samlValueType = saml11 ?
                    SoapConstants.VALUETYPE_SAML_ASSERTIONID2 :
                    SoapConstants.VALUETYPE_SAML_ASSERTIONID3;

                if ( Boolean.getBoolean(PROPERTY_SAML_USE_URI_REF) ) {
                    signatureKeyInfo = KeyInfoDetails.makeUriReference(assId, samlValueType);
                } else {
                    signatureKeyInfo = KeyInfoDetails.makeKeyId(assId, false, samlValueType);
                }
            } else if (dreq.getRecipientCertificate() != null) {
                // create a new EncryptedKey and sign with that
                String encryptionAlgorithm = dreq.getEncryptionAlgorithm();

                // If we've been given a secret key to use, use it instead of making a new one
                if (dreq.getEncryptedKey() != null) {
                    addedEncKeyXmlEncKey = new XencUtil.XmlEncKey(encryptionAlgorithm, dreq.getEncryptedKey());
                } else {
                    addedEncKeyXmlEncKey = generateXmlEncKey(encryptionAlgorithm, c);
                }

                addedEncKey = addEncryptedKey(c,
                                              securityHeader,
                                              dreq.getRecipientCertificate(),
                                              new Element[0],
                                              addedEncKeyXmlEncKey,
                                              dreq.getKeyEncryptionAlgorithm(),
                                              null);
                String encKeyId = getOrCreateWsuId(c, addedEncKey, null);

                if (dreq.isUseDerivedKeys()) {
                    // Derive a new key for signing
                    DerivedKeyToken derivedKeyToken =
                            addDerivedKeyToken(c,
                                               securityHeader,
                                               null,
                                               KeyInfoDetails.makeUriReference(encKeyId, SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1),
                                               getKeyLengthInBytesForAlgorithm(dreq.getEncryptionAlgorithm()),
                                               addedEncKeyXmlEncKey.getSecretKey().getEncoded(),
                                               "DerivedKey");
                    senderSigningKey = new XencUtil.XmlEncKey(dreq.getEncryptionAlgorithm(), derivedKeyToken.derivedKey).getSecretKey();
                    String dktId = getOrCreateWsuId(c, derivedKeyToken.dkt, "DerivedKey-Sig");
                    if (c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2)) {
                        signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2);
                    } else {
                        signatureKeyInfo = KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY);
                    }
                } else {
                    // No derived key -- use the raw EncryptedKey directly
                    senderSigningKey = addedEncKeyXmlEncKey.getSecretKey();
                    signatureKeyInfo = KeyInfoDetails.makeUriReference(encKeyId, SoapConstants.VALUETYPE_ENCRYPTED_KEY);
                }
            } else
                throw new IllegalArgumentException("Signing is requested, but there is no senderCertificate or WS-SecureConversation session");

            signature = addSignature(c,
                senderSigningKey,
                signList.toArray(new Element[0]),
                signPartList.toArray(new String[0]),
                dreq.isSignPartHeaders(),
                securityHeader,
                signatureKeyInfo,
                dreq.isSuppressSamlStrTransform());
            if (xencDesiredNextSibling == null)
                xencDesiredNextSibling = signature;
        }

        if (cryptList.size() > 0) {
            if (sct != null) {
                // Encrypt using Secure Conversation session
                if (session == null)
                    throw new IllegalArgumentException("Encryption is requested with SecureConversationSession, but session is null");
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
                                              cryptList.toArray(new Element[0]),
                                              encKey,
                                              KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
                } else {
                    addEncryptedReferenceList(c,
                                              securityHeader,
                                              xencDesiredNextSibling,
                                              cryptList.toArray(new Element[0]),
                                              encKey,
                                              KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY));
                }
            } else if (addedEncKey != null && addedEncKeyXmlEncKey != null) {
                if (dreq.isUseDerivedKeys()) {
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
                                                  cryptList.toArray(new Element[0]),
                                                  dktEncKey,
                                                  KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
                    } else {
                        addEncryptedReferenceList(c,
                                                  securityHeader,
                                                  xencDesiredNextSibling,
                                                  cryptList.toArray(new Element[0]),
                                                  dktEncKey,
                                                  KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY));
                    }
                } else {
                    // Encrypt using the EncryptedKey we already added
                    String encKeyId = getOrCreateWsuId(c, addedEncKey, null);
                    addEncryptedReferenceList(c,
                                              addedEncKey,
                                              xencDesiredNextSibling,
                                              cryptList.toArray(new Element[0]),
                                              addedEncKeyXmlEncKey,
                                              KeyInfoDetails.makeUriReference(encKeyId, SoapConstants.VALUETYPE_ENCRYPTED_KEY));
                }
            } else if (dreq.getEncryptedKeySha1() != null &&
                       dreq.getEncryptedKey() != null)
            {
                final String eksha = dreq.getEncryptedKeySha1();
                final byte[] ekkey = dreq.getEncryptedKey();

                // Encrypt using a reference to an implicit EncryptedKey that the recipient is assumed
                // already to possess (perhaps because we got it from them originally)
                if (dreq.isUseDerivedKeys()) {
                    // Derive a new key and use it for encryption
                    DerivedKeyToken dkt = addDerivedKeyToken(c,
                                                             securityHeader,
                                                             xencDesiredNextSibling,
                                                             KeyInfoDetails.makeEncryptedKeySha1Ref(eksha),
                                                             getKeyLengthInBytesForAlgorithm(dreq.getEncryptionAlgorithm()),
                                                             ekkey,
                                                             "DerivedKey");
                    String dktId = getOrCreateWsuId(c, dkt.dkt, "DerivedKey-Enc");
                    XencUtil.XmlEncKey dktEncKey = new XencUtil.XmlEncKey(dreq.getEncryptionAlgorithm(), dkt.derivedKey);
                    if (c.nsf.getWsscNs().equals( SoapConstants.WSSC_NAMESPACE2)) {
                        addEncryptedReferenceList(c,
                                                  securityHeader,
                                                  xencDesiredNextSibling,
                                                  cryptList.toArray(new Element[0]),
                                                  dktEncKey,
                                                  KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
                    } else {
                        addEncryptedReferenceList(c,
                                                  securityHeader,
                                                  xencDesiredNextSibling,
                                                  cryptList.toArray(new Element[0]),
                                                  dktEncKey,
                                                  KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY));
                    }
                } else {
                    // Reference the EncryptedKey directly
                    final KeyInfoDetails keyInfoDetails = KeyInfoDetails.makeEncryptedKeySha1Ref(eksha);

                    final String encryptionAlgorithm = dreq.getEncryptionAlgorithm();
                    XencUtil.XmlEncKey encKey = generateXmlEncKey(encryptionAlgorithm, c);
                    encKey = new XencUtil.XmlEncKey(encKey.getAlgorithm(), ekkey);
                    addEncryptedReferenceList(c,
                                              securityHeader,
                                              xencDesiredNextSibling,
                                              cryptList.toArray(new Element[0]),
                                              encKey,
                                              keyInfoDetails);
                }
            } else if (addedKerberosBst != null) {
                // Derive key using direct URI reference
                c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
                KeyInfoDetails kerbUriRef = KeyInfoDetails.makeUriReference(
                        getOrCreateWsuId(c, addedKerberosBst, null),
                        SoapConstants.VALUETYPE_KERBEROS_GSS_AP_REQ);
                DerivedKeyToken dkt = addDerivedKeyToken(c,
                                                         securityHeader,
                                                         xencDesiredNextSibling,
                                                         kerbUriRef,
                                                         getKeyLengthInBytesForAlgorithm(dreq.getEncryptionAlgorithm()),
                                                         dreq.getKerberosTicket().getKey(),
                                                         "DerivedKey");
                String dktId = getOrCreateWsuId(c, dkt.dkt, "DerivedKey-Enc");
                XencUtil.XmlEncKey dktEncKey = new XencUtil.XmlEncKey(dreq.getEncryptionAlgorithm(), dkt.derivedKey);
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          xencDesiredNextSibling,
                                          cryptList.toArray(new Element[0]),
                                          dktEncKey,
                                          KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));
            } else if (dreq.getKerberosTicket() != null) {
                // Derive key using KerberosSHA1 reference
                c.nsf.setWsscNs( SoapConstants.WSSC_NAMESPACE2);
                String kerbSha1 = KerberosUtils.getBase64Sha1(dreq.getKerberosTicket().getGSSAPReqTicket());
                KeyInfoDetails kerbShaRef = KeyInfoDetails.makeKeyId(kerbSha1, true, SoapConstants.VALUETYPE_KERBEROS_APREQ_SHA1);
                DerivedKeyToken dkt = addDerivedKeyToken(c,
                                                         securityHeader,
                                                         xencDesiredNextSibling,
                                                         kerbShaRef,
                                                         getKeyLengthInBytesForAlgorithm(dreq.getEncryptionAlgorithm()),
                                                         dreq.getKerberosTicket().getKey(),
                                                         "DerivedKey");
                String dktId = getOrCreateWsuId(c, dkt.dkt, "DerivedKey-Enc");
                XencUtil.XmlEncKey dktEncKey = new XencUtil.XmlEncKey(dreq.getEncryptionAlgorithm(), dkt.derivedKey);
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          xencDesiredNextSibling,
                                          cryptList.toArray(new Element[0]),
                                          dktEncKey,
                                          KeyInfoDetails.makeUriReference(dktId, SoapConstants.VALUETYPE_DERIVEDKEY2));

            } else if (dreq.getRecipientCertificate() != null) {
                // Encrypt to recipient's certificate
                String encryptionAlgorithm = dreq.getEncryptionAlgorithm();

                XencUtil.XmlEncKey encKey = generateXmlEncKey(encryptionAlgorithm, c);
                addEncryptedKey(c,
                                securityHeader,
                                dreq.getRecipientCertificate(),
                                cryptList.toArray(new Element[0]),
                                encKey,
                                dreq.getKeyEncryptionAlgorithm(),                        
                                xencDesiredNextSibling);
            } else
                throw new IllegalArgumentException("Encryption is requested, but there is no recipientCertificate or SecureConversation session.");

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

        return new DecorationResult() {
            private String encryptedKeySha1 = null;

            public String getEncryptedKeySha1() {
                if (encryptedKeySha1 != null)
                    return encryptedKeySha1;
                if (c.lastEncryptedKeyBytes == null)
                    return null;
                return encryptedKeySha1 = XencUtil.computeEncryptedKeySha1(c.lastEncryptedKeyBytes);
            }

            public SecretKey getEncryptedKeySecretKey() {
                return c.lastEncryptedKeySecretKey;
            }
        };
    }

    private Element addSignatureConfirmation(Element securityHeader, String signatureConfirmation) {
        String wsse11Ns = SoapConstants.SECURITY11_NAMESPACE;
        Element sc = DomUtils.createAndAppendElementNS(securityHeader, "SignatureConfirmation", wsse11Ns, "wsse11");
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
      throws NoSuchAlgorithmException, InvalidKeyException
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
        c.rand.nextBytes(nonce);

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
                                 Element[] elementsToSign,
                                 String[] partsToSign,
                                 boolean signPartHeaders,
                                 Element securityHeader,
                                 KeyInfoDetails keyInfoDetails,
                                 boolean suppressSamlStrDereference)
            throws DecoratorException, InvalidDocumentFormatException, IOException
    {

        if (elementsToSign == null || elementsToSign.length < 1) return null;

        // make sure all elements already have an id
        String[] signedIds = new String[elementsToSign.length];
        for (int i = 0; i < elementsToSign.length; i++) {
            Element eleToSign = elementsToSign[i];
            if (!suppressSamlStrDereference && "Assertion".equals(eleToSign.getLocalName()) &&
                 (SamlConstants.NS_SAML2.equals(eleToSign.getNamespaceURI()) ||
                  SamlConstants.NS_SAML.equals(eleToSign.getNamespaceURI()))) {
                // use STR-Transform for SAML assertions
                continue;
            }
            signedIds[i] = getOrCreateWsuId(c, eleToSign, null);
        }

        String signaturemethod;
        if (senderSigningKey instanceof RSAPrivateKey || "RSA".equals(senderSigningKey.getAlgorithm()))
            signaturemethod = SignatureMethod.RSA;
        else if (senderSigningKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else if (senderSigningKey instanceof SecretKey)
            signaturemethod = SignatureMethod.HMAC;
        else {
            throw new DecoratorException("Private Key type not supported " +
                    senderSigningKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementsToSign[0].getOwnerDocument(),
                                                           XSignature.SHA1, Canonicalizer.EXCLUSIVE, signaturemethod);
        template.setIndentation(false);
        template.setPrefix("ds");
        final Map<Node,Node> strTransformsNodeToNode = new HashMap<Node, Node>();
        for (int i = 0; i < elementsToSign.length; i++) {
            final Element element = elementsToSign[i];
            final String id = signedIds[i];

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
                // need SecurityTokenReference transform to go through indirection
                Element strTransform = elementsToSign[0].getOwnerDocument().createElementNS( SoapConstants.DIGSIG_URI, "ds:Transform");
                strTransform.setAttribute("Algorithm", SoapConstants.TRANSFORM_STR);
                Element strParams = DomUtils.createAndAppendElementNS(strTransform, "TransformationParameters", c.nsf.getWsseNs(), "wsse");
                Element cannonParam = DomUtils.createAndAppendElementNS(strParams, "CanonicalizationMethod", SoapConstants.DIGSIG_URI, "ds");
                cannonParam.setAttribute("Algorithm", Transform.C14N_EXCLUSIVE);
                ref.addTransform(strTransform);
                strTransformsNodeToNode.put(str, element);
            } else
                ref = template.createReference("#" + id);

            if (DomUtils.isElementAncestor(securityHeader, element)) {
                logger.fine("Per policy, breaking Basic Security Profile rules with enveloped signature" +
                        " of element " + element.getLocalName() + " with Id=\"" + id + "\"");
                ref.addTransform(Transform.ENVELOPED);
            }

            // Note that c14n is not required when using STR-Transform, this can be removed
            // once 4.0 is the earliest version in use.
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
            sigContext.sign(emptySignatureElement, senderSigningKey);
        } catch (XSignatureException e) {
            String msg = e.getMessage();
            if (msg != null && msg.indexOf("Found a relative URI") >= 0)       // Bug #1209
                throw new InvalidDocumentFormatException("Unable to sign this message due to a relative namespace URI.", e);
            throw new DecoratorException(e);
        }

        Element signatureElement = (Element)securityHeader.appendChild(emptySignatureElement);
        keyInfoDetails.createAndAppendKeyInfoElement(c.nsf, signatureElement);

        return signatureElement;
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
        Element token = ut.asElement(securityHeader.getOwnerDocument(),
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
                                              recipientCertificate.getPublicKey(),
                                              c.rand);
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
    private String getOrCreateWsuId(Context c, Element element, String basename) {
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
    private String createWsuId(Context c, Element element, String basename) {
        byte[] randbytes = new byte[16];
        c.rand.nextBytes(randbytes);
        String id = basename + "-" + c.count++ + "-" + HexUtils.hexDump(randbytes);

        if (c.idToElementCache.get(id) != null)
            throw new IllegalStateException("Duplicate wsu:ID generated: " + id); // can't happen

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
            (MimeKnob) context.message.getKnob(MimeKnob.class);

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
                                         boolean useExisting)
            throws InvalidDocumentFormatException
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
                logger.warning(error);
                throw new InvalidDocumentFormatException(error);
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
            securityHeader = SoapUtil.makeSecurityElement(message, context.nsf.getWsseNs(), actor);
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
     * @param c             the context fro random generatir source
     * @return the encription key instance holding the secret key and the algorithm name
     * @throws NoSuchAlgorithmException if the algorithm URI is not recognized, or
     *                                  if the provided keyBytes is too short for the specified algorithm
     */
    private XencUtil.XmlEncKey generateXmlEncKey(String xEncAlgorithm, Context c) throws NoSuchAlgorithmException {
        byte[] keyBytes = new byte[32]; // (max for aes 256)
        c.rand.nextBytes(keyBytes);
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
}