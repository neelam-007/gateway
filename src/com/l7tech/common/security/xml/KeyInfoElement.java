/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.security.token.ParsedElement;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TooManyChildElementsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class KeyInfoElement implements ParsedElement {
    private static final Logger logger = Logger.getLogger(KeyInfoElement.class.getName());
    private final Element element;
    private final X509Certificate cert;

    public static class KeyInfoElementException extends Exception {
        public KeyInfoElementException() {}
        public KeyInfoElementException(String message) { super(message); }
        public KeyInfoElementException(String message, Throwable cause) { super(message, cause); }
        public KeyInfoElementException(Throwable cause) { super(cause); }
    }

    /**
     * Parse the specified KeyInfo element.
     * @param keyinfo the element to parse.  Must not be null.  Must be a ds:KeyInfo element in proper namespace.
     * @param thumbprintResolver resolver for X.509 sha1 thumbprints, or null to disable thumbprint support.
     * @throws NullPointerException if it's null
     * @throws IllegalArgumentException if it isn't a ds:KeyInfo element
     * @throws SAXException if the format of this KeyInfo is invalid or not supported
     * @throws KeyInfoElementException if this KeyInfo refers to an X509 SHA1 thumbprint, but no thumbprint
     *                                 resolver was supplied.
     */
    public static KeyInfoElement parse(Element keyinfo, ThumbprintResolver thumbprintResolver)
            throws SAXException, KeyInfoElementException
    {
        return new KeyInfoElement(keyinfo, thumbprintResolver);
    }


    private KeyInfoElement(Element keyinfo,
                           ThumbprintResolver thumbprintResolver)
            throws SAXException, KeyInfoElementException
    {
        if (!"KeyInfo".equals(keyinfo.getLocalName())) throw new IllegalArgumentException("Element is not a KeyInfo element");
        if (!SoapUtil.DIGSIG_URI.equals(keyinfo.getNamespaceURI())) throw new IllegalArgumentException("KeyInfo element is not in dsig namespace");
        try {
            this.element = keyinfo;
            Element x509Data = XmlUtil.findOnlyOneChildElementByName(keyinfo, SoapUtil.DIGSIG_URI, "X509Data");
            if (x509Data == null) {
                Element str = XmlUtil.findOnlyOneChildElementByName(keyinfo, SoapUtil.SECURITY_URIS_ARRAY, "SecurityTokenReference");
                if (str == null) throw new SAXException("KeyInfo has no X509Data or SecurityTokenReference");
                // Use SecurityTokenReference
                if (thumbprintResolver == null) throw new KeyInfoElementException("KeyInfo uses SecurityTokenReference but no thumbprint resolver is available");
                // TODO there can be multiple KeyIdentifiers in a single SecurityTokenReference
                // TODO there can be multiple KeyIdentifiers in a single SecurityTokenReference
                // TODO there can be multiple KeyIdentifiers in a single SecurityTokenReference
                // TODO there can be multiple KeyIdentifiers in a single SecurityTokenReference
                Element keyid = XmlUtil.findOnlyOneChildElementByName(str, str.getNamespaceURI(), "KeyIdentifier");
                if (keyid == null) throw new SAXException("KeyInfo has SecurityTokenReference but no KeyIdentifier");
                String vt = keyid.getAttribute("ValueType");
                if (vt == null || !vt.endsWith(SoapUtil.VALUETYPE_X509_THUMB_SHA1_SUFFIX))
                    throw new SAXException("KeyInfo uses STR/KeyIdentifier ValueType other than ThumbprintSHA1: " + vt);
                String value = XmlUtil.getTextValue(keyid);
                if (value == null || value.length() < 1) throw new SAXException("KeyInfo contains an empty KeyIdentifier");

                X509Certificate gotCert = thumbprintResolver.lookup(value);
                if (gotCert == null) throw new SAXException("KeyInfo KeyIdentifier thumbprint did not match any X.509 certificate known to this recipient");
                cert = gotCert;
            } else {
                // Use X509Data
                Element x509CertEl = XmlUtil.findOnlyOneChildElementByName(x509Data, SoapUtil.DIGSIG_URI, "X509Certificate");
                if (x509CertEl == null) throw new SAXException("KeyInfo has no X509Data/X509Certificate");
                String certBase64 = XmlUtil.getTextValue(x509CertEl);
                byte[] certBytes = HexUtils.decodeBase64(certBase64, true);
                cert = CertUtils.decodeCert(certBytes);
                if (cert == null) throw new SAXException("KeyInfo includes certificate which cannot be recovered"); // can't happen
            }
        } catch (TooManyChildElementsException e) {
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException("Invalid base64 sequence: " + e.getMessage(), e);
        } catch (CertificateException e) {
            throw new SAXException("Invalid X.509 certificate: " + e.getMessage(), e);
        }
    }

    public Element asElement() {
        return element;
    }

    /** @return the X.509 certificate this KeyInfo referred to or included.  Currently this can never be null. */
    public X509Certificate getCertificate() {
        if (cert == null) throw new IllegalStateException("KeyInfo target certificate is null");
        return cert;
    }

    /**
     * Checks if the specified EncryptedType's KeyInfo is addressed to the specified recipient certificate.
     * @param encryptedType the EncryptedKey or EncryptedData element.  Must include a KeyInfo child.
     * @param recipientCert
     * @throws com.l7tech.common.xml.InvalidDocumentFormatException  if there was a problem with the encryptedType, or the KeyInfo didn't match.
     * @throws com.l7tech.common.security.xml.UnexpectedKeyInfoException      if the keyinfo did not match the recipientCert
     * @throws java.security.GeneralSecurityException        if there was a problem with the recipient certificate or a certificate
     *                                         embedded within the encryptedType.
     */
    public static void checkKeyInfo(Element encryptedType, X509Certificate recipientCert)
            throws UnexpectedKeyInfoException, InvalidDocumentFormatException, GeneralSecurityException
    {
        // bugzilla #1582
        if (recipientCert == null) {
            // if we dont have a recipient cert, then obviously, this is not meant for us. this would happen for example
            // when the agent is processing a response from the ssg that has an encryptedkey in it but the client account
            // does not have a client cert. (this is possible if the encryption is meant for upstream client)
            throw new UnexpectedKeyInfoException("No recipient cert to compare with. This is obvioulsy not meant for us.");
        }

        Element kinfo = XmlUtil.findOnlyOneChildElementByName(encryptedType, SoapUtil.DIGSIG_URI, SoapUtil.KINFO_EL_NAME);
        if (kinfo == null) throw new InvalidDocumentFormatException(encryptedType.getLocalName() + " includes no KeyInfo element");
        assertKeyInfoMatchesCertificate(kinfo, recipientCert);
    }

    public static void assertKeyInfoMatchesCertificate(Element keyInfo, X509Certificate cert) throws InvalidDocumentFormatException, UnexpectedKeyInfoException, CertificateException {
        Element str = XmlUtil.findOnlyOneChildElementByName(keyInfo,
                                                            SoapUtil.SECURITY_URIS_ARRAY,
                                                            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        if (str == null) throw new UnsupportedKeyInfoFormatException("KeyInfo includes no SecurityTokenReference");
        Element ki = XmlUtil.findOnlyOneChildElementByName(str,
                                                           SoapUtil.SECURITY_URIS_ARRAY,
                                                           SoapUtil.KEYIDENTIFIER_EL_NAME);
        if (ki == null) throw new UnsupportedKeyInfoFormatException("KeyInfo's SecurityTokenReference includes no KeyIdentifier element");
        String valueType = ki.getAttribute("ValueType");
        String keyIdentifierValue = XmlUtil.getTextValue(ki);
        byte[] keyIdValueBytes = new byte[0];
        try {
            keyIdValueBytes = HexUtils.decodeBase64(keyIdentifierValue, true);
        } catch (IOException e) {
            throw new InvalidDocumentFormatException("Unable to parse base64 Key Identifier", e);
        }
        if (keyIdValueBytes == null || keyIdValueBytes.length < 1) throw new InvalidDocumentFormatException("KeyIdentifier was empty");
        if (valueType == null || valueType.length() <= 0) {
            logger.fine("The KeyId Value Type is not specified. We will therefore assume it is a Subject Key Identifier.");
            valueType = SoapUtil.VALUETYPE_SKI;
        }
        if (valueType.endsWith(SoapUtil.VALUETYPE_SKI_SUFFIX)) {
            // If not typed, assume it's a ski
            byte[] ski = cert.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
            if (ski == null) {
                // TODO try making up both RFC 3280 SKIs and see if they match (Bug #1001)
                //throw new CertificateException("Unable to verify that KeyInfo is addressed to us -- " +
                //                               "our own certificate does not contain a Subject Key Identifier.");
                // TODO should reject the message until we implement fix as above
                logger.log(Level.INFO, "Unable to verify that KeyInfo is addressed to us; skipping SKI check");
                return;
            } else {
                // trim if necessary
                byte[] ski2 = ski;
                if (ski.length > keyIdValueBytes.length) {
                    ski2 = new byte[keyIdValueBytes.length];
                    System.arraycopy(ski, ski.length-keyIdValueBytes.length,
                                     ski2, 0, keyIdValueBytes.length);
                }
                if (Arrays.equals(keyIdValueBytes, ski2)) {
                    logger.fine("the Key SKI is recognized. This key is for us for sure!");
                    /* FALLTHROUGH */
                } else {
                    String msg = "This KeyInfo declares a specific SKI, " +
                            "but our certificate's SKI does not match.";
                    logger.fine(msg);
                    throw new UnexpectedKeyInfoException(msg);
                }
            }
        } else if (valueType.endsWith(SoapUtil.VALUETYPE_X509_SUFFIX)) {
            // It seems to be a complete certificate
            X509Certificate referencedCert = CertUtils.decodeCert(keyIdValueBytes);
            if (CertUtils.certsAreEqual(cert, referencedCert)) {
                logger.fine("The Key recipient cert is recognized");
                /* FALLTHROUGH */

            } else {
                String msg = "This KeyInfo declares a specific cert, " +
                        "but our certificate does not match.";
                logger.warning(msg);
                throw new UnexpectedKeyInfoException(msg);
            }
        } else if (valueType.endsWith(SoapUtil.VALUETYPE_X509_THUMB_SHA1_SUFFIX))
        {
            // TODO replace this with a cert cache lookup by SHA1 thumbprint
            byte[] certBytes = cert.getEncoded();
            MessageDigest sha1 = HexUtils.getSha1();
            sha1.reset();
            byte[] certSha1 = sha1.digest(certBytes);
            if (Arrays.equals(certSha1, keyIdValueBytes)) {
                logger.fine("The cert SHA1 thumbprint was recognized.  The cert is ours for sure.");
                /* FALLTHROUGH */
            } else {
                String msg = "This KeyInfo declares a specific cert SHA1 thumbprint, " +
                        "but our certificate's thumbprint does not match.";
                logger.fine(msg);
                throw new UnexpectedKeyInfoException(msg);
            }
        } else if (valueType.endsWith(SoapUtil.VALUETYPE_ENCRYPTED_KEY_SHA1_SUFFIX)) {
            // TODO - to support this, need to be able to look up a (long-)previously-processed EncryptedKey by its hash
            throw new UnsupportedKeyInfoFormatException("The EncryptedKey's KeyInfo uses an unsupported " +
                                                     "ValueType: " + valueType);
        } else
            throw new UnsupportedKeyInfoFormatException("The EncryptedKey's KeyInfo uses an unsupported " +
                                                     "ValueType: " + valueType);
    }

    /**
     * Add a KeyInfo that refers to a cert by its SKI to any parent element.  Caller must supply the namespaces.
     *
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     *
     */
    public static void addKeyInfoToElement(Element keyInfoParent,
                                           String wsseNs,
                                           String wssePrefix,
                                           String valueType,
                                           byte[] idBytes,
                                           String base64EncodingTypeUri)
    {
        Document soapMsg = keyInfoParent.getOwnerDocument();

        Element cipherData = XmlUtil.findFirstChildElementByName(keyInfoParent, SoapUtil.XMLENC_NS, "CipherData");
        // If there's a cipherdata, but keyinfo before it
        final Element keyInfo;
        if (cipherData == null)
            keyInfo = XmlUtil.createAndAppendElementNS(keyInfoParent, "KeyInfo", SoapUtil.DIGSIG_URI, "dsig");
        else
            keyInfo = XmlUtil.createAndInsertBeforeElementNS(cipherData, "KeyInfo", SoapUtil.DIGSIG_URI, "dsig");
        Element securityTokenRef = XmlUtil.createAndAppendElementNS(keyInfo, SoapUtil.SECURITYTOKENREFERENCE_EL_NAME,
            wsseNs, wssePrefix);
        Element keyId = XmlUtil.createAndAppendElementNS(securityTokenRef, SoapUtil.KEYIDENTIFIER_EL_NAME,
            wsseNs, wssePrefix);

        keyId.setAttribute("ValueType", valueType);
        keyId.setAttribute("EncodingType", base64EncodingTypeUri);

        String recipSkiB64 = HexUtils.encodeBase64(idBytes, true);
        keyId.appendChild(XmlUtil.createTextNode(soapMsg, recipSkiB64));
    }

    /**
     * Add a KeyInfo to the specified dsig:Signature element.
     *
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     *
     * @param sigElement      the ds:Signature element to which the KeyInfo should be added.
     * @param wsseNs          wsse namespace URI to use.
     * @param wssePrefix      wsse prefix to use.
     * @param base64EncodingTypeUri  URI to use for EncodingType of Base64Binary.
     * @param senderSki              If non-null, use a SKI reference to identify sender cert.
     * @param keyInfoReferenceTarget the Element to which the KeyInfo should refer, or null if senderSki is provided instead.  Ignored if senderSki is provided.
     *                               senderSki and keyInfoReferenceTarget must not both be null.
     *                               If this is provided and no senderSki is provided, keyInfoValueTypeURI must be provided as well.
     * @param keyInfoReferenceTargetWsuId  if keyInfoReferenceTarget is supplied, this must be the reference target's already-set wsu:Id.
     * @param keyInfoValueTypeURI    the value type URL to use for the KeyInfo reference to keyInfoReferenceTarget.  Must not be null if keyInfoReferenceTarget != null.
     * @param keyInfoKeyIdValue  to content of a KeyInfo/SecurityTokenReference/KeyIdentifier element, or null if keyInfoReferenceTarget is provided.
     * @throws KeyInfoElementException if a KeyInfo cannot be built with the specified arguments.
     */
    public static void addDsigKeyInfo(Element sigElement,
                                      String wsseNs,
                                      String wssePrefix,
                                      String base64EncodingTypeUri,
                                      byte[] senderSki,
                                      Element keyInfoReferenceTarget,
                                      String keyInfoReferenceTargetWsuId,
                                      String keyInfoValueTypeURI,
                                      String keyInfoKeyIdValue)
            throws KeyInfoElementException
    {
        // Add the KeyInfo element.
        if (senderSki != null) {
            // Include KeyInfo element in signature that refers to the specified SKI.
            addKeyInfoToElement(sigElement, wsseNs, wssePrefix, SoapUtil.VALUETYPE_SKI, senderSki, base64EncodingTypeUri);
        } else {
            // Include KeyInfo element in signature that refers to the specified target element with the specified value type.
            // add following KeyInfo
            // <KeyInfo>
            //  <wsse:SecurityTokenReference>
            //      <wsse:Reference	URI="#bstId"
            //                      ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3" />
            //      </wsse:SecurityTokenReference>
            // </KeyInfo>
            Element keyInfoEl = sigElement.getOwnerDocument().createElementNS(sigElement.getNamespaceURI(), "KeyInfo");
            keyInfoEl.setPrefix("ds");
            Element secTokRefEl = sigElement.getOwnerDocument().createElementNS(wsseNs, SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
            secTokRefEl.setPrefix(wssePrefix);
            keyInfoEl.appendChild(secTokRefEl);

            if (keyInfoReferenceTarget != null) {
                // Create <KeyInfo><SecurityTokenReference><Reference URI="#Blah" ValueType="ValueTypeURI"></></></>
                Element refEl = sigElement.getOwnerDocument().createElementNS(wsseNs, "Reference");
                refEl.setPrefix(wssePrefix);
                secTokRefEl.appendChild(refEl);
                refEl.setAttribute("URI", "#" + keyInfoReferenceTargetWsuId);
                refEl.setAttribute("ValueType", keyInfoValueTypeURI);
                sigElement.appendChild(keyInfoEl);
            } else if (keyInfoKeyIdValue != null) {
                // Create <KeyInfo><SecurityTokenReference><KeyIdentifier ValueType="ValueTypeURI">b64blah==</></></>
                Element kidEl = sigElement.getOwnerDocument().createElementNS(wsseNs, "KeyIdentifier");
                kidEl.setPrefix(wssePrefix);
                secTokRefEl.appendChild(kidEl);
                kidEl.setAttribute("ValueType", keyInfoValueTypeURI);
                kidEl.appendChild(XmlUtil.createTextNode(kidEl, keyInfoKeyIdValue));
                sigElement.appendChild(keyInfoEl);
            } else {
                throw new KeyInfoElementException("Signing requested, but theres no sender SKI, KeyInfo Reference target, or KeyIdentifier value");
            }
        }
    }

    /**
     * Appends a KeyInfo to the specified parent Element, referring to keyInfoReferenceTarget.
     *
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method
     * TODO merge the three add*KeyInfo*() methods into one useful factory method 
     *
     */
    public static Element addXencKeyInfo(Element securityHeader,
                                         Element parent,
                                         Element keyInfoReferenceTarget,
                                         String keyInfoReferenceTargetWsuId,
                                         String keyInfoKeyIdValue,
                                         String keyInfoValueTypeURI)
            throws KeyInfoElementException
    {
        Element cipherData = XmlUtil.findFirstChildElementByName(parent, SoapUtil.XMLENC_NS, "CipherData");
        final Element keyInfo;
        if (cipherData == null)
            keyInfo = XmlUtil.createAndAppendElementNS(parent, "KeyInfo", SoapUtil.DIGSIG_URI, "dsig");
        else
            keyInfo = XmlUtil.createAndInsertBeforeElementNS(cipherData, "KeyInfo", SoapUtil.DIGSIG_URI, "dsig");
        final String wsseNs = securityHeader.getNamespaceURI();
        final String wsse = "wsse";
        Element str = XmlUtil.createAndAppendElementNS(keyInfo,
            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME,
            wsseNs,
            wsse);
        if (keyInfoReferenceTarget != null) {
            Element ref = XmlUtil.createAndAppendElementNS(str,
                "Reference",
                wsseNs,
                wsse);
            String uri = keyInfoReferenceTargetWsuId;
            ref.setAttribute("URI", uri);
            ref.setAttribute("ValueType", keyInfoValueTypeURI);
        } else if (keyInfoKeyIdValue != null) {
            Element kid = XmlUtil.createAndAppendElementNS(str, "KeyIdentifier", wsseNs, wsse);
            kid.setAttribute("ValueType", keyInfoValueTypeURI);
            kid.appendChild(XmlUtil.createTextNode(kid, keyInfoKeyIdValue));
        } else {
            throw new KeyInfoElementException("Encryption requested, but theres no KeyInfo Reference target or KeyIdentifier value");
        }
        return keyInfo;
    }

    public static class UnsupportedKeyInfoFormatException extends InvalidDocumentFormatException {
        public UnsupportedKeyInfoFormatException(String message) {
            super(message);
        }

        public UnsupportedKeyInfoFormatException() {
        }

        public UnsupportedKeyInfoFormatException(Throwable cause) {
            super(cause);
        }

        public UnsupportedKeyInfoFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
