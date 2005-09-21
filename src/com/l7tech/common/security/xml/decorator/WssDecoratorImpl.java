/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.common.security.xml.decorator;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.DesKey;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.security.xml.KeyInfoElement;
import com.l7tech.common.security.xml.SecureConversationKeyDeriver;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssDecoratorImpl implements WssDecorator {
    private static final Logger logger = Logger.getLogger(WssDecorator.class.getName());

    public static final int TIMESTAMP_TIMOUT_MILLIS = 300000;
    private static final int DERIVED_KEY_LENGTH = 16;

    public WssDecoratorImpl() {
    }

    private static class Context {
        SecureRandom rand = new SecureRandom();
        long count = 0;
        Map idToElementCache = new HashMap();
        String wsseNS = SoapUtil.SECURITY_NAMESPACE;
        String wsuNS = SoapUtil.WSU_NAMESPACE;

        String getBase64EncodingTypeUri() {
            return wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)
                    ? SoapUtil.ENCODINGTYPE_BASE64BINARY
                    : SoapUtil.ENCODINGTYPE_BASE64BINARY_2;     // lyonsm: ??? what is this for?  It hardcodes wsse: prefix!
        }
    }

    /**
     * Decorate a soap message with WSS style security.
     *
     * @param message the soap message to decorate
     */
    public void decorateMessage(Document message, DecorationRequirements dreq)
      throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException {
        Context c = new Context();

        c.wsseNS = dreq.getPreferredSecurityNamespace();
        c.wsuNS = dreq.getPreferredWSUNamespace();

        Element securityHeader = createSecurityHeader(message, c.wsseNS, c.wsuNS,
            dreq.getSecurityHeaderActor());
        Set signList = dreq.getElementsToSign();

        // If we aren't signing the entire message, find extra elements to sign
        if (dreq.isSignTimestamp() || !signList.isEmpty()) {
            int timeoutMillis = dreq.getTimestampTimeoutMillis();
            if (timeoutMillis < 1)
                timeoutMillis = TIMESTAMP_TIMOUT_MILLIS;
            Element timestamp = SoapUtil.addTimestamp(securityHeader,
                c.wsuNS,
                dreq.getTimestampCreatedDate(), // null ok
                timeoutMillis);
            signList.add(timestamp);
        }

        Element xencDesiredNextSibling = null;
        if (dreq.getSignatureConfirmation() != null) {
            Element sc = addSignatureConfirmation(securityHeader, dreq.getSignatureConfirmation());
            signList.add(sc);
            xencDesiredNextSibling = sc;
        }

        // If there are any WSA headers in the message, and we are signing anything else, then sign them too
        Element messageId = SoapUtil.getL7aMessageIdElement(message);
        if (messageId != null && !signList.isEmpty())
            signList.add(messageId);
        Element relatesTo = SoapUtil.getL7aRelatesToElement(message);
        if (relatesTo != null && !signList.isEmpty())
            signList.add(relatesTo);

        if (dreq.getUsernameTokenCredentials() != null)
            createUsernameToken(securityHeader, dreq.getUsernameTokenCredentials());

        byte[] senderSki = null;
        Element bst = null;
        if (dreq.getSenderMessageSigningCertificate() != null && !signList.isEmpty()) {
            if (dreq.isSuppressBst()) {
                // Use keyinfo reference target of a SKI
                X509Certificate senderCert = dreq.getSenderMessageSigningCertificate();
                senderSki = CertUtils.getSKIBytesFromCert(senderCert);
                if (senderSki == null) {
                    // Supposed to refer to sender cert by its SKI, but it has no SKI
                    throw new DecoratorException("suppressBst is requested, but the sender cert has no SubjectKeyIdentifier");
                }
            } else {
                // Use keyinfo reference target of a BinarySecurityToken
                bst = addX509BinarySecurityToken(securityHeader, dreq.getSenderMessageSigningCertificate(), c);
            }
        }
        // At this point, if we are signing using a sender cert, we have either recipSki or bst but not both

        Element sct = null;
        DecorationRequirements.SecureConversationSession session =
          dreq.getSecureConversationSession();
        if (session != null) {
            if (session.getId() == null)
                throw new DecoratorException("SeureConversation Session ID must not be null");
            sct = addSecurityContextToken(securityHeader, session.getId());
        }

        Element saml = null;
        if (dreq.getSenderSamlToken() != null) {
            saml = addSamlSecurityToken(securityHeader, dreq.getSenderSamlToken());
            if (dreq.isIncludeSamlTokenInSignature()) {
                signList.add(saml);
            }
        }

        Element signature = null;
        Element addedEncKey = null;
        XencUtil.XmlEncKey addedEncKeyXmlEncKey = null;
        if (dreq.getElementsToSign().size() > 0) {
            Key senderSigningKey = null;
            Element keyInfoReferenceTarget = null;
            String keyInfoKeyIdValue = null;
            String keyInfoValueTypeURI = null;
            if (sct != null) {
                // No BST; must be WS-SecureConversation
                keyInfoValueTypeURI = SoapUtil.VALUETYPE_DERIVEDKEY;
                if (session == null)
                    throw new IllegalArgumentException("Signing is requested with SecureConversationSession, but session is null");
                DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c, securityHeader, null, session);
                keyInfoReferenceTarget = derivedKeyToken.dkt;
                senderSigningKey = new AesKey(derivedKeyToken.derivedKey, derivedKeyToken.derivedKey.length * 8);
            } else if (dreq.getEncryptedKey() != null &&
                       dreq.getEncryptedKeySha1() != null)
            {
                // Use a reference to an implicit EncryptedKey that the recipient is assumed to already possess
                // (possibly because we got it from them originally)
                keyInfoValueTypeURI = SoapUtil.VALUETYPE_ENCRYPTED_KEY_SHA1;
                keyInfoKeyIdValue = dreq.getEncryptedKeySha1();
                senderSigningKey = dreq.getEncryptedKey();
            } else if (senderSki != null) {
                senderSigningKey = dreq.getSenderMessageSigningPrivateKey();
                if (senderSigningKey == null)
                    throw new IllegalArgumentException("Signing is requested with sender cert, but senderPrivateKey is null");
            } else if (bst != null) {
                // sign with X509 Binary Security Token
                keyInfoReferenceTarget = bst;
                if (c.wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)) {
                    keyInfoValueTypeURI = SoapUtil.VALUETYPE_X509;
                } else {
                    keyInfoValueTypeURI = SoapUtil.VALUETYPE_X509_2;
                }
                senderSigningKey = dreq.getSenderMessageSigningPrivateKey();
                if (senderSigningKey == null)
                    throw new IllegalArgumentException("Signing is requested with sender cert, but senderPrivateKey is null");
            } else if (saml != null) {
                // sign with SAML token
                keyInfoReferenceTarget = saml;
                keyInfoValueTypeURI = SoapUtil.VALUETYPE_SAML;
                senderSigningKey = dreq.getSenderMessageSigningPrivateKey();
                if (senderSigningKey == null)
                    throw new IllegalArgumentException("Signing is requested with saml:Assertion, but senderPrivateKey is null");
            } else if (dreq.getRecipientCertificate() != null) {
                // create a new EncryptedKey and sign with that
                String encryptionAlgorithm = dreq.getEncryptionAlgorithm();

                addedEncKeyXmlEncKey = generate(encryptionAlgorithm, c);
                addedEncKey = addEncryptedKey(c,
                                              securityHeader,
                                              dreq.getRecipientCertificate(),
                                              new Element[0],
                                              addedEncKeyXmlEncKey,
                                              null);
                keyInfoReferenceTarget = addedEncKey;
                keyInfoValueTypeURI = null;
                senderSigningKey = addedEncKeyXmlEncKey.getSecretKey();
            } else
                throw new IllegalArgumentException("Signing is requested, but there is no senderCertificate or WS-SecureConversation session");

            signature = addSignature(c,
                senderSigningKey,
                (Element[])(dreq.getElementsToSign().toArray(new Element[0])),
                securityHeader,
                senderSki,
                keyInfoReferenceTarget,
                keyInfoKeyIdValue,
                keyInfoValueTypeURI);
            if (xencDesiredNextSibling == null)
                xencDesiredNextSibling = signature;
        }

        if (dreq.getElementsToEncrypt().size() > 0) {
            if (addedEncKey != null && addedEncKeyXmlEncKey != null) {
                // Encrypt using the EncryptedKey we already added
                Element keyInfoReferenceTarget = addedEncKey;
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          addedEncKeyXmlEncKey,
                                          (Element[])(dreq.getElementsToEncrypt().toArray(new Element[0])),
                                          xencDesiredNextSibling,
                                          keyInfoReferenceTarget,
                                          null,
                                          null);
            } else if (sct != null) {
                // Encrypt using Secure Conversation session
                if (session == null)
                    throw new IllegalArgumentException("Encryption is requested with SecureConversationSession, but session is null");
                DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c, securityHeader, xencDesiredNextSibling, session);
                Element keyInfoReferenceTarget = derivedKeyToken.dkt;
                String keyInfoValueTypeURI = SoapUtil.VALUETYPE_DERIVEDKEY;
                XencUtil.XmlEncKey encKey = new XencUtil.XmlEncKey(XencUtil.AES_128_CBC, new AesKey(derivedKeyToken.derivedKey, 128));
                addEncryptedReferenceList(c,
                  securityHeader,
                  encKey,
                  (Element[])(dreq.getElementsToEncrypt().toArray(new Element[0])),
                  xencDesiredNextSibling,
                  keyInfoReferenceTarget,
                  null,
                  keyInfoValueTypeURI);
            } else if (dreq.getEncryptedKeySha1() != null &&
                       dreq.getEncryptedKey() != null)
            {
                // Encrypt using a reference to an implicit EncryptedKey that the recipient is assumed
                // already to possess (perhaps because we got it from them originally)
                String keyInfoKeyIdValue = dreq.getEncryptedKeySha1();
                String keyInfoValueTypeURI = SoapUtil.VALUETYPE_ENCRYPTED_KEY_SHA1;
                final SecretKey key = dreq.getEncryptedKey();
                final String alg;
                switch (key.getEncoded().length) {
                    case 256 / 8: alg = XencUtil.AES_256_CBC; break;
                    case 192 / 8: alg = XencUtil.AES_192_CBC; break;
                    case 128 / 8: alg = XencUtil.AES_128_CBC; break;
                    default: throw new DecoratorException("Unsupported AES key length: " + key.getEncoded().length);
                }
                XencUtil.XmlEncKey encKey = new XencUtil.XmlEncKey(alg, key);
                addEncryptedReferenceList(c,
                                          securityHeader,
                                          encKey,
                                          (Element[])(dreq.getElementsToEncrypt().toArray(new Element[0])),
                                          xencDesiredNextSibling,
                                          null,
                                          keyInfoKeyIdValue,
                                          keyInfoValueTypeURI);
            } else if (dreq.getRecipientCertificate() != null) {
                // Encrypt to recipient's certificate
                String encryptionAlgorithm = dreq.getEncryptionAlgorithm();

                XencUtil.XmlEncKey encKey = generate(encryptionAlgorithm, c);
                addEncryptedKey(c,
                  securityHeader,
                  dreq.getRecipientCertificate(),
                  (Element[])(dreq.getElementsToEncrypt().toArray(new Element[0])),
                  encKey, xencDesiredNextSibling);
            } else
                throw new IllegalArgumentException("Encryption is requested, but there is no recipientCertificate or SecureConversation session.");

        }

        // Decoration is done.

        // Final cleanup: if we are about to emit an empty Security header, remove it now
        if (XmlUtil.elementIsEmpty(securityHeader)) {
            final Element soapHeader = (Element)securityHeader.getParentNode();
            soapHeader.removeChild(securityHeader);

            // If we are about to emit an empty SOAP header, remove it now
            if (XmlUtil.elementIsEmpty(soapHeader))
                soapHeader.getParentNode().removeChild(soapHeader);
        }

    }

    private Element addSignatureConfirmation(Element securityHeader, String signatureConfirmation) {
        String wsse11Ns = SoapUtil.SECURITY11_NAMESPACE;
        Element sc = XmlUtil.createAndAppendElementNS(securityHeader, "SignatureConfirmation", wsse11Ns, "wsse11");
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

    private DerivedKeyToken addDerivedKeyToken(Context c,
                                               Element securityHeader,
                                               Element desiredNextSibling,
                                               DecorationRequirements.SecureConversationSession session)
      throws NoSuchAlgorithmException, InvalidKeyException {
        Document factory = securityHeader.getOwnerDocument();
        String wsseNs = securityHeader.getNamespaceURI();
        String wsse = securityHeader.getPrefix();
        if (wsse == null) wsse = "wsse";

        Element dkt;
        if (desiredNextSibling == null)
            dkt = XmlUtil.createAndAppendElementNS(securityHeader,
                SoapUtil.WSSC_DK_EL_NAME,
                SoapUtil.WSSC_NAMESPACE,
                "wssc");
        else
            dkt = XmlUtil.createAndInsertBeforeElementNS(desiredNextSibling,
                SoapUtil.WSSC_DK_EL_NAME,
                SoapUtil.WSSC_NAMESPACE,
                "wssc");
        String wssc = dkt.getPrefix() == null ? "" : dkt.getPrefix() + ":";
        dkt.setAttributeNS(SoapUtil.WSSC_NAMESPACE, wssc + "Algorithm", SoapUtil.ALGORITHM_PSHA);
        Element str = XmlUtil.createAndAppendElementNS(dkt, SoapUtil.SECURITYTOKENREFERENCE_EL_NAME, wsseNs, wsse);
        Element ref = XmlUtil.createAndAppendElementNS(str, "Reference", wsseNs, wsse);

        // fla 18 Aug, 2004
        // NOTE This method of reffering to the SCT uses a Reference URI that contains the Identifier value
        // instead of the actual #wsuid of the SCT.
        // We do this for better interop with .net clients (WSE 2.0)
        // we may want to support different methods based on the user agent
        // the alternative would be : ref.setAttribute("URI", "#" + getOrCreateWsuId(c, sct, null));
        ref.setAttribute("URI", session.getId());
        ref.setAttribute("ValueType", SoapUtil.VALUETYPE_SECURECONV);

        // Gather derived key params
        int length = DERIVED_KEY_LENGTH;
        byte[] nonce = new byte[length];
        c.rand.nextBytes(nonce);
        String label = "WS-SecureConversation";

        // Encode derived key params for the recipient
        Element generationEl = XmlUtil.createAndAppendElementNS(dkt, "Generation", SoapUtil.WSSC_NAMESPACE, "wssc");
        generationEl.appendChild(XmlUtil.createTextNode(factory, "0"));
        Element lengthEl = XmlUtil.createAndAppendElementNS(dkt, "Length", SoapUtil.WSSC_NAMESPACE, "wssc");
        lengthEl.appendChild(XmlUtil.createTextNode(factory, Integer.toString(length)));
        Element labelEl = XmlUtil.createAndAppendElementNS(dkt, "Label", SoapUtil.WSSC_NAMESPACE, "wssc");
        labelEl.appendChild(XmlUtil.createTextNode(factory, label));
        Element nonceEl = XmlUtil.createAndAppendElementNS(dkt, "Nonce", wsseNs, wsse);
        nonceEl.appendChild(XmlUtil.createTextNode(factory, HexUtils.encodeBase64(nonce, true)));

        // Derive a copy of the key for ourselves
        byte[] seed = new byte[label.length() + nonce.length];
        System.arraycopy(label.getBytes(), 0, seed, 0, label.length());
        System.arraycopy(nonce, 0, seed, label.length(), nonce.length);
        byte[] derivedKey = new SecureConversationKeyDeriver().pSHA1(session.getSecretKey(), seed, length);

        return new DerivedKeyToken(dkt, derivedKey);
    }

    private Element addSecurityContextToken(Element securityHeader, String id) {
        Element sct = XmlUtil.createAndAppendElementNS(securityHeader,
            SoapUtil.SECURITY_CONTEXT_TOK_EL_NAME,
            SoapUtil.WSSC_NAMESPACE,
            "wssc");
        Element identifier = XmlUtil.createAndAppendElementNS(sct,
            "Identifier",
            SoapUtil.WSSC_NAMESPACE,
            "wssc");
        identifier.appendChild(XmlUtil.createTextNode(identifier, id));
        return sct;
    }

    /**
     * Add a ds:Signature element to the Security header and return it.
     *
     * @param c                      the processing context.  Must not be null.
     * @param senderSigningKey       the Key that should be used to compute the signature.  May be RSA public or private key, or an AES symmetric key.
     * @param elementsToSign         an array of elements that should be signed.  Must be non-null references to elements in the Document being processed.  Must not be null or empty.
     * @param securityHeader         the Security header to which the new ds:Signature element should be added.  May not be null.
     * @param senderSki              the SubjectKeyIdentifier to list in the KeyInfo, or null.  If this present, keyInfoReferenceTarget and keyInfoValueTypeURI are ignored.
     *                               senderSki and keyInfoReferenceTarget must not both be null.
     * @param keyInfoReferenceTarget the Element to which the KeyInfo should refer, or null if senderSki is provided instead.  Ignored if senderSki is provided.
     *                               senderSki and keyInfoReferenceTarget must not both be null.
     *                               If this is provided and no senderSki is provided, keyInfoValueTypeURI must be provided as well.
     * @param keyInfoKeyIdValue  to content of a KeyInfo/SecurityTokenReference/KeyIdentifier element, or null if keyInfoReferenceTarget is provided.
     * @param keyInfoValueTypeURI    the value type URL to use for the KeyInfo reference to keyInfoReferenceTarget.  Must not be null if keyInfoReferenceTarget != null.
     * @return the ds:Signature element, which has already been appended to the Security header.
     * @throws DecoratorException             if the signature could not be created with this message and these decoration requirements.
     * @throws InvalidDocumentFormatException if the message format is too invalid to overlook.
     */
    private Element addSignature(final Context c, Key senderSigningKey,
                                 Element[] elementsToSign, Element securityHeader,
                                 byte[] senderSki,
                                 Element keyInfoReferenceTarget,
                                 String keyInfoKeyIdValue,
                                 String keyInfoValueTypeURI) throws DecoratorException, InvalidDocumentFormatException
    {

        if (elementsToSign == null || elementsToSign.length < 1) return null;

        // make sure all elements already have an id
        String[] signedIds = new String[elementsToSign.length];
        for (int i = 0; i < elementsToSign.length; i++) {
            signedIds[i] = getOrCreateWsuId(c, elementsToSign[i], null);
        }

        String signaturemethod = null;
        if (senderSigningKey instanceof RSAPrivateKey)
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
        template.setPrefix("ds");
        final Map strTransformsNodeToNode = new HashMap();
        for (int i = 0; i < elementsToSign.length; i++) {
            final Element element = elementsToSign[i];
            final String id = signedIds[i];

            final Reference ref;
            if ("Assertion".equals(element.getLocalName()) && SamlConstants.NS_SAML.equals(element.getNamespaceURI())) {
                // Bug #1434 -- unable to refer to SAML assertion directly using its AssertionID -- need intermediate STR with wsu:Id
                final String assId = element.getAttribute("AssertionID");
                if (assId == null || assId.length() < 1)
                    throw new InvalidDocumentFormatException("Unable to decorate: SAML Assertion has missing or empty AssertionID");
                Element str = addSamlSecurityTokenReference(securityHeader, assId);
                ref = template.createReference("#" + getOrCreateWsuId(c, str, "SamlSTR"));
                ref.addTransform(SoapUtil.TRANSFORM_STR); // need SecurityTokenReference transform to go through indirection
                strTransformsNodeToNode.put(str, element);
            } else
                ref = template.createReference("#" + id);

            if (XmlUtil.isElementAncestor(securityHeader, element)) {
                logger.fine("Per policy, breaking Basic Security Profile rules with enveloped signature" +
                  " of element " + element.getLocalName() + " with Id=\"" + id + "\"");
                ref.addTransform(Transform.ENVELOPED);
            }

            ref.addTransform(Transform.C14N_EXCLUSIVE);
            template.addReference(ref);
        }
        Element emptySignatureElement = template.getSignatureElement();

        // Ensure that CanonicalizationMethod has required c14n subelemen
        final Element signedInfoElement = template.getSignedInfoElement();
        Element c14nMethod = XmlUtil.findFirstChildElementByName(signedInfoElement,
            SoapUtil.DIGSIG_URI,
            "CanonicalizationMethod");
        DsigUtil.addInclusiveNamespacesToElement(c14nMethod);

        NodeList transforms = signedInfoElement.getElementsByTagNameNS(signedInfoElement.getNamespaceURI(), "Transform");
        for (int i = 0; i < transforms.getLength(); ++i)
            if (Transform.C14N_EXCLUSIVE.equals(((Element)transforms.item(i)).getAttribute("Algorithm")))
                DsigUtil.addInclusiveNamespacesToElement((Element)transforms.item(i));

        // Before signing, ensure that wsu:Id is present on keyinfo reference target, if there is one
        if (keyInfoReferenceTarget != null)
            getOrCreateWsuId(c, keyInfoReferenceTarget, null);

        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                Element e = (Element)c.idToElementCache.get(s);
                if (e != null)
                    return e;
                e = SoapUtil.getElementByWsuId(doc, s);
                if (e != null)
                    c.idToElementCache.put(s, e);
                return e;
            }
        });
        sigContext.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
                throw new SAXException("Unsupported external entity reference publicId=" + publicId +
                  ", systemId=" + systemId);
            }
        });
        sigContext.setAlgorithmFactory(new AlgorithmFactoryExtn() {
            public Transform getTransform(String s) throws NoSuchAlgorithmException {
                if (SoapUtil.TRANSFORM_STR.equals(s))
                    return new Transform() {
            public String getURI() {
                return SoapUtil.TRANSFORM_STR;
            }

            public void transform(TransformContext c) throws TransformException {
                Node source = c.getNode();
                if (source == null) throw new TransformException("Source node is null");
                final Node result = (Node)strTransformsNodeToNode.get(source);
                if (result == null) throw new TransformException("Destination node is null");
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
                return super.getTransform(s);
            }
        });
        try {
            sigContext.sign(emptySignatureElement, senderSigningKey);
        } catch (XSignatureException e) {
            String msg = e.getMessage();
            if (msg != null && msg.indexOf("Found a relative URI") >= 0)       // Bug #1209
                throw new InvalidDocumentFormatException("Unable to sign this message due to a relative namespace URI.", e);
            throw new DecoratorException(e);
        }

        Element signatureElement = (Element)securityHeader.appendChild(emptySignatureElement);

        try {
            KeyInfoElement.addDsigKeyInfo(signatureElement,
                           securityHeader.getNamespaceURI(),
                           securityHeader.getPrefix(),
                           c.getBase64EncodingTypeUri(),
                           senderSki,
                           keyInfoReferenceTarget,
                           keyInfoReferenceTarget != null ? getOrCreateWsuId(c, keyInfoReferenceTarget, null) : null,
                           keyInfoValueTypeURI,
                           keyInfoKeyIdValue);
        } catch (KeyInfoElement.KeyInfoElementException e) {
            throw new DecoratorException(e.getMessage(), e);
        }

        return signatureElement;
    }

    private Element addSamlSecurityTokenReference(Element securityHeader, String assertionId) {
        String wsseNs = securityHeader.getNamespaceURI();
        String wsse = securityHeader.getPrefix();
        Element str = XmlUtil.createAndAppendElementNS(securityHeader, SoapUtil.SECURITYTOKENREFERENCE_EL_NAME, wsseNs, wsse);
        Element keyid = XmlUtil.createAndAppendElementNS(str, SoapUtil.KEYIDENTIFIER_EL_NAME, wsseNs, wsse);
        keyid.setAttribute("ValueType", SoapUtil.VALUETYPE_SAML_ASSERTIONID);
        keyid.appendChild(XmlUtil.createTextNode(keyid, assertionId));
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
                                              Element securityHeader,
                                              XencUtil.XmlEncKey encKey,
                                              Element[] elementsToEncrypt,
                                              Element desiredNextSibling,
                                              Element keyInfoReferenceTarget,
                                              String keyInfoKeyIdValue,
                                              String keyInfoValueTypeURI)
      throws GeneralSecurityException, DecoratorException {
        String xencNs = SoapUtil.XMLENC_NS;

        // Put the ReferenceList in the right place
        Element referenceList;
        if (desiredNextSibling == null) {
            referenceList = XmlUtil.createAndAppendElementNS(securityHeader,
                SoapUtil.REFLIST_EL_NAME,
                xencNs, "xenc");
        } else {
            referenceList = XmlUtil.createAndInsertBeforeElementNS(desiredNextSibling,
                SoapUtil.REFLIST_EL_NAME,
                xencNs, "xenc");
        }
        String xenc = referenceList.getPrefix();

        int numElementsEncrypted = 0;
        for (int i = 0; i < elementsToEncrypt.length; i++) {
            Element element = elementsToEncrypt[i];

            if (XmlUtil.elementIsEmpty(element)) {
                logger.fine("Element \"" + element.getNodeName() + "\" is empty; will not encrypt it");
                continue;
            }

            Element encryptedElement = null;
            try {
                encryptedElement = XencUtil.encryptElement(element, encKey);
            } catch (XencUtil.XencException e) {
                throw new DecoratorException(e.getMessage(), e);
            }

            Element dataReference = XmlUtil.createAndAppendElementNS(referenceList, "DataReference", xencNs, xenc);
            dataReference.setAttribute("URI", "#" + getOrCreateWsuId(c, encryptedElement, element.getLocalName()));

            try {
                KeyInfoElement.addXencKeyInfo(securityHeader,
                           encryptedElement,
                           keyInfoReferenceTarget,
                           keyInfoReferenceTarget != null ? getOrCreateWsuId(c, keyInfoReferenceTarget, null) : null,
                           keyInfoKeyIdValue,
                           keyInfoValueTypeURI);
            } catch (KeyInfoElement.KeyInfoElementException e) {
                throw new DecoratorException(e.getMessage(), e);
            }

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
                                    XencUtil.XmlEncKey encKey, Element desiredNextSibling)
      throws GeneralSecurityException, DecoratorException {

        Document soapMsg = securityHeader.getOwnerDocument();

        String xencNs = SoapUtil.XMLENC_NS;

        // Put the encrypted key in the right place
        Element encryptedKey;
        if (desiredNextSibling == null) {
            encryptedKey = XmlUtil.createAndAppendElementNS(securityHeader,
                SoapUtil.ENCRYPTEDKEY_EL_NAME,
                xencNs, "xenc");
        } else {
            encryptedKey = XmlUtil.createAndInsertBeforeElementNS(desiredNextSibling,
                SoapUtil.ENCRYPTEDKEY_EL_NAME,
                xencNs, "xenc");
        }
        String xenc = encryptedKey.getPrefix();

        Element encryptionMethod = XmlUtil.createAndAppendElementNS(encryptedKey, "EncryptionMethod", xencNs, xenc);
        encryptionMethod.setAttribute("Algorithm", SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO);

        byte[] recipSki = CertUtils.getSKIBytesFromCert(recipientCertificate);
        if (recipSki != null) {
            if (c.wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)) {
                addKeyInfoToEncryptedKey(encryptedKey, recipSki, SoapUtil.VALUETYPE_SKI, c);
            } else {
                addKeyInfoToEncryptedKey(encryptedKey, recipSki, SoapUtil.VALUETYPE_SKI_2, c);
            }
        } else {
            if (c.wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)) {
                addKeyInfoToEncryptedKey(encryptedKey, recipientCertificate.getEncoded(), SoapUtil.VALUETYPE_X509, c);
            } else {
                addKeyInfoToEncryptedKey(encryptedKey, recipientCertificate.getEncoded(), SoapUtil.VALUETYPE_X509_2, c);
            }
        }
        Element cipherData = XmlUtil.createAndAppendElementNS(encryptedKey, "CipherData", xencNs, xenc);
        Element cipherValue = XmlUtil.createAndAppendElementNS(cipherData, "CipherValue", xencNs, xenc);
        final String base64 = XencUtil.encryptKeyWithRsaAndPad(encKey.getSecretKey().getEncoded(), recipientCertificate.getPublicKey(), c.rand);
        cipherValue.appendChild(XmlUtil.createTextNode(soapMsg, base64));
        Element referenceList = XmlUtil.createAndAppendElementNS(encryptedKey, SoapUtil.REFLIST_EL_NAME, xencNs, xenc);

        int numElementsEncrypted = 0;
        for (int i = 0; i < elementsToEncrypt.length; i++) {
            Element element = elementsToEncrypt[i];
            if (XmlUtil.elementIsEmpty(element)) {
                logger.fine("Element \"" + element.getNodeName() + "\" is empty; will not encrypt it");
                continue;
            }
            Element encryptedElement = null;
            try {
                encryptedElement = XencUtil.encryptElement(element, encKey);
            } catch (XencUtil.XencException e) {
                throw new DecoratorException(e.getMessage(), e);
            }

            Element dataReference = XmlUtil.createAndAppendElementNS(referenceList, "DataReference", xencNs, xenc);
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
        String id = SoapUtil.getElementWsuId(element);
        if (id == null) {
            id = createWsuId(c, element, basename == null ? element.getLocalName() : basename);
        }
        return id;
    }

    /**
     * Add a KeyInfo that refers to a cert by its SKI to an EncryptedKey element.
     */
    private void addKeyInfoToEncryptedKey(Element encryptedKey, byte[] idBytes, String valueType, Context c) {
        String wsseNs = encryptedKey.getParentNode().getNamespaceURI();
        String wssePrefix = encryptedKey.getParentNode().getPrefix();
        KeyInfoElement.addKeyInfoToElement(encryptedKey, wsseNs, wssePrefix, valueType, idBytes, c.getBase64EncodingTypeUri());
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

        final String wsuNs = c.wsuNS;
        SoapUtil.setWsuId(element, wsuNs, id);

        return id;
    }

    private Element addX509BinarySecurityToken(Element securityHeader, X509Certificate certificate, Context c)
      throws CertificateEncodingException {
        Document factory = securityHeader.getOwnerDocument();
        Element element = factory.createElementNS(securityHeader.getNamespaceURI(),
            SoapUtil.BINARYSECURITYTOKEN_EL_NAME);
        element.setPrefix(securityHeader.getPrefix());

        if (c.wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)) {
            element.setAttribute("ValueType", SoapUtil.VALUETYPE_X509);
            element.setAttribute("EncodingType", SoapUtil.ENCODINGTYPE_BASE64BINARY);
        } else {
            element.setAttribute("ValueType", SoapUtil.VALUETYPE_X509_2);
            element.setAttribute("EncodingType", SoapUtil.ENCODINGTYPE_BASE64BINARY_2);
        }
        element.appendChild(XmlUtil.createTextNode(factory, HexUtils.encodeBase64(certificate.getEncoded(), true)));
        securityHeader.appendChild(element);
        return element;
    }

    private Element createSecurityHeader(Document message,
                                         String wsseNS,
                                         String wsuNs,
                                         String actor) throws InvalidDocumentFormatException {
        Element oldSecurity = SoapUtil.getSecurityElement(message, actor);

        if (oldSecurity != null) {
            String error;
            if (actor != null) {
                error = "This message already has a security header for actor " + actor;
            } else {
                error = "This message already has a security header for the default actor";
            }
            logger.warning(error);
            throw new InvalidDocumentFormatException(error);
        }

        Element securityHeader = null;
        if (oldSecurity == null) {
            securityHeader = SoapUtil.makeSecurityElement(message, wsseNS, actor);
        } else {
            securityHeader = oldSecurity;
        }
        // Make sure wsu is declared to save duplication
        XmlUtil.getOrCreatePrefixForNamespace(securityHeader, wsuNs, "wsu");
        return securityHeader;
    }

    /**
     * Create the <code>EncriptionKey</code> based un the xenc algorithm name.
     *
     * @param xEncAlgorithm the algorithm name such as http://www.w3.org/2001/04/xmlenc#tripledes-cbc
     * @param c             the context fro random generatir source
     * @return the encription key instance holding the secret key and the algorithm name
     * @throws IllegalArgumentException thrown for unknown algorithm
     */
    private XencUtil.XmlEncKey generate(String xEncAlgorithm, Context c) {
        byte[] keyBytes = new byte[32]; // (max for aes 256)
        if (XencUtil.TRIPLE_DES_CBC.equals(xEncAlgorithm)) {
            c.rand.nextBytes(keyBytes);
            return new XencUtil.XmlEncKey(xEncAlgorithm, new DesKey(keyBytes, 192));
        } else if (XencUtil.AES_128_CBC.equals(xEncAlgorithm)) {
            c.rand.nextBytes(keyBytes);
            return new XencUtil.XmlEncKey(xEncAlgorithm, new AesKey(keyBytes, 128));
        } else if (XencUtil.AES_192_CBC.equals(xEncAlgorithm)) {
            c.rand.nextBytes(keyBytes);
            return new XencUtil.XmlEncKey(xEncAlgorithm, new AesKey(keyBytes, 192));
        } else if (XencUtil.AES_256_CBC.equals(xEncAlgorithm)) {
            c.rand.nextBytes(keyBytes);
            return new XencUtil.XmlEncKey(xEncAlgorithm, new AesKey(keyBytes, 256));
        }
        throw new IllegalArgumentException("Unknown algorithm " + xEncAlgorithm);
    }
}