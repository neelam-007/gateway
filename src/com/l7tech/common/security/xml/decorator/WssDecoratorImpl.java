/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.decorator;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.EncryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.CipherData;
import com.ibm.xml.enc.type.CipherValue;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.xml.SecureConversationKeyDeriver;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
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

    private static class Context {
        SecureRandom rand = new SecureRandom();
        long count = 0;
        Map idToElementCache = new HashMap();
        String wsseNS = SoapUtil.SECURITY_NAMESPACE;
        String wsuNS = SoapUtil.WSU_NAMESPACE;
    }

    /**
     * Decorate a soap message with WSS style security.
     *
     * @param message the soap message to decorate
     */
    public void decorateMessage(Document message, DecorationRequirements decorationRequirements)
      throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException {
        Context c = new Context();

        c.wsseNS = decorationRequirements.getPreferredSecurityNamespace();
        c.wsuNS = decorationRequirements.getPreferredWSUNamespace();

        Element securityHeader = createSecurityHeader(message, c.wsseNS, c.wsuNS,
                                                      decorationRequirements.getSecurityHeaderActor());
        Set signList = decorationRequirements.getElementsToSign();

        // If we aren't signing the entire message, find extra elements to sign
        if (decorationRequirements.isSignTimestamp() || !signList.isEmpty()) {
            int timeoutMillis = decorationRequirements.getTimestampTimeoutMillis();
            if (timeoutMillis < 1)
                timeoutMillis = TIMESTAMP_TIMOUT_MILLIS;
            Element timestamp = SoapUtil.addTimestamp(securityHeader,
              c.wsuNS,
              decorationRequirements.getTimestampCreatedDate(), // null ok
              timeoutMillis);
            signList.add(timestamp);
        }

        // If there are any WSA headers in the message, and we are signing anything else, then sign them too
        Element messageId = SoapUtil.getL7aMessageIdElement(message);
        if (messageId != null && !signList.isEmpty())
            signList.add(messageId);
        Element relatesTo = SoapUtil.getL7aRelatesToElement(message);
        if (relatesTo != null && !signList.isEmpty())
            signList.add(relatesTo);

        if (decorationRequirements.getUsernameTokenCredentials() != null &&
          decorationRequirements.getUsernameTokenCredentials().getFormat() == CredentialFormat.CLEARTEXT) {
            createUsernameToken(securityHeader, decorationRequirements.getUsernameTokenCredentials());
        }

        Element bst = null;
        if (decorationRequirements.getSenderMessageSigningCertificate() != null && !signList.isEmpty())
            bst = addX509BinarySecurityToken(securityHeader, decorationRequirements.getSenderMessageSigningCertificate(), c);

        Element sct = null;
        DecorationRequirements.SecureConversationSession session =
          decorationRequirements.getSecureConversationSession();
        if (session != null) {
            if (session.getId() == null)
                throw new DecoratorException("SeureConversation Session ID must not be null");
            sct = addSecurityContextToken(securityHeader, session.getId());
        }

        Element saml = null;
        if (decorationRequirements.getSenderSamlToken() != null) {
            saml = addSamlSecurityToken(securityHeader, decorationRequirements.getSenderSamlToken());
            if (decorationRequirements.isIncludeSamlTokenInSignature()) {
                signList.add(saml);
            }
        }

        Element signature = null;
        if (decorationRequirements.getElementsToSign().size() > 0) {
            Key senderSigningKey = null;
            Element keyInfoReferenceTarget = null;
            String keyInfoValueTypeURI = null;
            if (sct != null) {
                // No BST; must be WS-SecureConversation
                keyInfoValueTypeURI = SoapUtil.VALUETYPE_DERIVEDKEY;
                if (session == null)
                    throw new IllegalArgumentException("Signing is requested with SecureConversationSession, but session is null");
                DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c, securityHeader, null, session);
                keyInfoReferenceTarget = derivedKeyToken.dkt;
                senderSigningKey = new AesKey(derivedKeyToken.derivedKey, derivedKeyToken.derivedKey.length * 8);
            } else if (bst != null) {
                // sign with X509 Binary Security Token
                keyInfoReferenceTarget = bst;
                if (c.wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)) {
                    keyInfoValueTypeURI = SoapUtil.VALUETYPE_X509;
                } else {
                    keyInfoValueTypeURI = SoapUtil.VALUETYPE_X509_2;
                }
                senderSigningKey = decorationRequirements.getSenderMessageSigningPrivateKey();
                if (senderSigningKey == null)
                    throw new IllegalArgumentException("Signing is requested with sender cert, but senderPrivateKey is null");
            } else if (saml != null) {
                // sign with SAML token
                keyInfoReferenceTarget = saml;
                keyInfoValueTypeURI = SoapUtil.VALUETYPE_SAML;
                senderSigningKey = decorationRequirements.getSenderMessageSigningPrivateKey();
                if (senderSigningKey == null)
                    throw new IllegalArgumentException("Signing is requested with saml:Assertion, but senderPrivateKey is null");
            } else
                throw new IllegalArgumentException("Signing is requested, but there is no senderCertificate or WS-SecureConversation session");

            signature = addSignature(c,
              senderSigningKey,
              (Element[])(decorationRequirements.getElementsToSign().toArray(new Element[0])),
              securityHeader,
              keyInfoReferenceTarget,
              keyInfoValueTypeURI);
        }

        if (decorationRequirements.getElementsToEncrypt().size() > 0) {
            if (sct != null) {
                // Encrypt using Secure Conversation session
                if (session == null)
                    throw new IllegalArgumentException("Encryption is requested with SecureConversationSession, but session is null");
                DerivedKeyToken derivedKeyToken = addDerivedKeyToken(c, securityHeader, signature, session);
                Element keyInfoReferenceTarget = derivedKeyToken.dkt;
                String keyInfoValueTypeURI = SoapUtil.VALUETYPE_DERIVEDKEY;
                addEncryptedReferenceList(c,
                  securityHeader,
                  derivedKeyToken.derivedKey,
                  (Element[])(decorationRequirements.getElementsToEncrypt().toArray(new Element[0])),
                  signature,
                  keyInfoReferenceTarget,
                  keyInfoValueTypeURI);
            } else if (decorationRequirements.getRecipientCertificate() != null) {
                // Encrypt to recipient's certificate
                addEncryptedKey(c,
                  securityHeader,
                  decorationRequirements.getRecipientCertificate(),
                  (Element[])(decorationRequirements.getElementsToEncrypt().toArray(new Element[0])),
                  signature);
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

    private Element addSignature(final Context c, Key senderSigningKey,
                                 Element[] elementsToSign, Element securityHeader,
                                 Element keyInfoReferenceTarget,
                                 String keyInfoValueTypeURI) throws DecoratorException, InvalidDocumentFormatException {

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
        addInclusiveNamespacesToElement(c14nMethod);

        NodeList transforms = signedInfoElement.getElementsByTagNameNS(signedInfoElement.getNamespaceURI(), "Transform");
        for (int i = 0; i < transforms.getLength(); ++i)
            if (Transform.C14N_EXCLUSIVE.equals(((Element)transforms.item(i)).getAttribute("Algorithm")))
                addInclusiveNamespacesToElement((Element)transforms.item(i));

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        // add following KeyInfo
        // <KeyInfo>
        //  <wsse:SecurityTokenReference>
        //      <wsse:Reference	URI="#bstId"
        //                      ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3" />
        //      </wsse:SecurityTokenReference>
        // </KeyInfo>
        String bstId = getOrCreateWsuId(c, keyInfoReferenceTarget, null);
        String wssePrefix = securityHeader.getPrefix();
        Element keyInfoEl = securityHeader.getOwnerDocument().createElementNS(emptySignatureElement.getNamespaceURI(),
          "KeyInfo");
        keyInfoEl.setPrefix("ds");
        Element secTokRefEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
          SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        secTokRefEl.setPrefix(wssePrefix);
        Element refEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
          "Reference");
        refEl.setPrefix(wssePrefix);
        secTokRefEl.appendChild(refEl);
        keyInfoEl.appendChild(secTokRefEl);
        refEl.setAttribute("URI", "#" + bstId);
        refEl.setAttribute("ValueType", keyInfoValueTypeURI);


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
                            c.setContent(new NodeList() {
                                public int getLength() {
                                    return 1;
                                }

                                public Node item(int index) {
                                    return result;
                                }
                            });
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
        signatureElement.appendChild(keyInfoEl);

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

    /**
     * Add a c14n:InclusiveNamespaces child element to the specified element with an empty PrefixList.
     */
    private void addInclusiveNamespacesToElement(Element element) {
        Element inclusiveNamespaces = XmlUtil.createAndAppendElementNS(element,
          "InclusiveNamespaces",
          Transform.C14N_EXCLUSIVE,
          "c14n");
        inclusiveNamespaces.setAttribute("PrefixList", "");
    }

    private static Element createUsernameToken(Element securityHeader, LoginCredentials creds) {
        // What this element looks like:
        // <wsse:UsernameToken>
        //    <wsse:Username>username</wsse:Username>
        //    <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">password</wsse:Password>
        // </wsse:UsernameToken>
        // create elements
        UsernameTokenImpl ut = new UsernameTokenImpl(creds.getLogin(),
                                                     creds.getCredentials());
        Element token = ut.asElement(securityHeader.getOwnerDocument(),
                                     securityHeader.getNamespaceURI(),
                                     securityHeader.getPrefix());
        securityHeader.appendChild(token);
        return token;
    }


    /**
     * Appends a KeyInfo to the specified parent Element, referring to keyInfoReferenceTarget.
     */
    private Element addKeyInfo(Context c,
                               Element securityHeader,
                               Element parent,
                               Element keyInfoReferenceTarget,
                               String keyInfoValueTypeURI) {
        Element keyInfo = XmlUtil.createAndAppendElementNS(parent,
          "KeyInfo",
          SoapUtil.DIGSIG_URI,
          "dsig");
        Element str = XmlUtil.createAndAppendElementNS(keyInfo,
          SoapUtil.SECURITYTOKENREFERENCE_EL_NAME,
          securityHeader.getNamespaceURI(),
          "wsse");
        Element ref = XmlUtil.createAndAppendElementNS(str,
          "Reference",
          securityHeader.getNamespaceURI(),
          "wsse");
        String uri = getOrCreateWsuId(c, keyInfoReferenceTarget, null);
        ref.setAttribute("URI", uri);
        ref.setAttribute("ValueType", keyInfoValueTypeURI);
        return keyInfo;
    }

    /**
     * Encrypts one or more document elements using a caller-supplied key (probaly from a DKT),
     * and appends a ReferenceList to the Security header before the specified desiredNextSibling element
     * (probably the Signature).
     */
    private Element addEncryptedReferenceList(Context c,
                                              Element securityHeader,
                                              byte[] keyBytes,
                                              Element[] elementsToEncrypt,
                                              Element desiredNextSibling,
                                              Element keyInfoReferenceTarget,
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

            Element encryptedElement = encryptElement(element, keyBytes);

            Element dataReference = XmlUtil.createAndAppendElementNS(referenceList, "DataReference", xencNs, xenc);
            dataReference.setAttribute("URI", "#" + getOrCreateWsuId(c, encryptedElement, element.getLocalName()));

            addKeyInfo(c, securityHeader, encryptedElement, keyInfoReferenceTarget, keyInfoValueTypeURI);
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
                                    Element desiredNextSibling)
      throws GeneralSecurityException, DecoratorException {
        Document soapMsg = securityHeader.getOwnerDocument();

        byte[] keyBytes = new byte[16];
        c.rand.nextBytes(keyBytes);

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

        byte[] recipSki = recipientCertificate.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
        if (recipSki != null && recipSki.length > 4) {
            byte[] goodSki = new byte[recipSki.length - 4];
            System.arraycopy(recipSki, 4, goodSki, 0, goodSki.length);
            if (c.wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)) {
                addKeyInfo(encryptedKey, goodSki, SoapUtil.VALUETYPE_SKI);
            } else {
                addKeyInfo(encryptedKey, goodSki, SoapUtil.VALUETYPE_SKI_2);
            }
        } else {
            if (c.wsseNS.equals(SoapUtil.SECURITY_NAMESPACE)) {
                addKeyInfo(encryptedKey, recipientCertificate.getEncoded(), SoapUtil.VALUETYPE_X509);
            } else {
                addKeyInfo(encryptedKey, recipientCertificate.getEncoded(), SoapUtil.VALUETYPE_X509_2);
            }
        }
        Element cipherData = XmlUtil.createAndAppendElementNS(encryptedKey, "CipherData", xencNs, xenc);
        Element cipherValue = XmlUtil.createAndAppendElementNS(cipherData, "CipherValue", xencNs, xenc);
        final String base64 = XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipientCertificate.getPublicKey(), c.rand);
        cipherValue.appendChild(XmlUtil.createTextNode(soapMsg, base64));
        Element referenceList = XmlUtil.createAndAppendElementNS(encryptedKey, SoapUtil.REFLIST_EL_NAME, xencNs, xenc);

        int numElementsEncrypted = 0;
        for (int i = 0; i < elementsToEncrypt.length; i++) {
            Element element = elementsToEncrypt[i];
            if (XmlUtil.elementIsEmpty(element)) {
                logger.fine("Element \"" + element.getNodeName() + "\" is empty; will not encrypt it");
                continue;
            }
            Element encryptedElement = encryptElement(element, keyBytes);

            Element dataReference = XmlUtil.createAndAppendElementNS(referenceList, "DataReference", xencNs, xenc);
            dataReference.setAttribute("URI", "#" + getOrCreateWsuId(c, encryptedElement, element.getLocalName()));
            numElementsEncrypted++;
        }

        if (numElementsEncrypted < 1) {
            // None of the elements needed to be encrypted.  Abort the addition of the EncryptedKey.
            Node parent = encryptedKey.getParentNode();
            if (parent != null)
                parent.removeChild(encryptedKey);
            return null;
        }

        return encryptedKey;
    }

    /**
     * Encrypt the specified element.  Returns the new EncryptedData element.
     *
     * @param element
     * @param keyBytes
     * @return the EncryptedData element that replaces the specified element.
     */
    private Element encryptElement(Element element, byte[] keyBytes)
      throws DecoratorException, GeneralSecurityException {
        Document soapMsg = element.getOwnerDocument();

        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());

        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(EncryptionMethod.AES128_CBC);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setType(EncryptedData.CONTENT);
        Element encDataElement = null;
        try {
            encDataElement = encData.createElement(soapMsg, true);
        } catch (StructureException e) {
            throw new DecoratorException(e);
        }

        // Create encryption context and encrypt the header subtree
        EncryptionContext ec = new EncryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        af.setProvider(JceProvider.getSymmetricJceProvider().getName());
        ec.setAlgorithmFactory(af);
        ec.setEncryptedType(encDataElement, EncryptedData.CONTENT, null, null);

        ec.setData(element);
        ec.setKey(new AesKey(keyBytes, 128));

        try {
            ec.encrypt();
            ec.replace();
        } catch (KeyInfoResolvingException e) {
            throw new DecoratorException(e); // can't happen
        } catch (StructureException e) {
            throw new DecoratorException(e); // shouldn't happen
        } catch (IOException e) {
            throw new DecoratorException(e); // shouldn't happen
        }

        Element encryptedData = ec.getEncryptedTypeAsElement();
        return encryptedData;
    }

    /**
     * Get the wsu:Id for the specified element.  If it doesn't already have a wsu:Id attribute a new one
     * is created for the element.
     *
     * @param c
     * @param element
     * @param basename Optional.  If non-null, will be used as the start of the Id string
     * @return
     */
    private String getOrCreateWsuId(Context c, Element element, String basename) {
        String id = SoapUtil.getElementWsuId(element);
        if (id == null) {
            id = createWsuId(c, element, basename == null ? element.getLocalName() : basename);
        }
        return id;
    }

    private void addKeyInfo(Element encryptedKey, byte[] idBytes, String valueType) {
        Document soapMsg = encryptedKey.getOwnerDocument();
        String wsseNs = encryptedKey.getParentNode().getNamespaceURI();
        String wssePrefix = encryptedKey.getParentNode().getPrefix();

        Element keyInfo = XmlUtil.createAndAppendElementNS(encryptedKey, "KeyInfo", SoapUtil.DIGSIG_URI, "dsig");
        Element securityTokenRef = XmlUtil.createAndAppendElementNS(keyInfo, SoapUtil.SECURITYTOKENREFERENCE_EL_NAME,
          wsseNs, wssePrefix);
        Element keyId = XmlUtil.createAndAppendElementNS(securityTokenRef, SoapUtil.KEYIDENTIFIER_EL_NAME,
          wsseNs, wssePrefix);
        keyId.setAttribute("ValueType", valueType);
        String recipSkiB64 = HexUtils.encodeBase64(idBytes, true);
        keyId.appendChild(XmlUtil.createTextNode(soapMsg, recipSkiB64));
    }

    /**
     * Generate a wsu:Id for the specified element, adds it to the element, and returns it.
     * Uses the specified basename as the start of the Id.
     *
     * @param c
     * @param element
     * @return
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
}
