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
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a parsed KeyInfo element, perhaps from a ds:Signature.  For creating a new KeyInfo element,
 * see {@link KeyInfoDetails}.
 */
public class KeyInfoElement implements ParsedElement {
    private static final Logger logger = Logger.getLogger(KeyInfoElement.class.getName());
    private final Element element;
    private final X509Certificate cert;

    /** Exception throws if a certificate resolver is required to parse a KeyInfo, but none is available. */
    public static class MissingResolverException extends Exception {
        private MissingResolverException() {}
        private MissingResolverException(String message) { super(message); }
        private MissingResolverException(String message, Throwable cause) { super(message, cause); }
        private MissingResolverException(Throwable cause) { super(cause); }
    }

    /**
     * Parse the specified KeyInfo element.
     * @param keyinfo the element to parse.  Must not be null.  Must be a ds:KeyInfo element in proper namespace.
     * @param securityTokenResolver resolver for X.509 sha1 thumbprints, or null to disable thumbprint support.
     * @throws NullPointerException if it's null
     * @throws IllegalArgumentException if it isn't a ds:KeyInfo element
     * @throws SAXException if the format of this KeyInfo is invalid or not supported
     * @throws MissingResolverException if this KeyInfo refers to an X509 SHA1 thumbprint or keyname, but no certificate resolver was supplied.
     *
     */
    public static KeyInfoElement parse(Element keyinfo, SecurityTokenResolver securityTokenResolver)
            throws SAXException, MissingResolverException
    {
        return new KeyInfoElement(keyinfo, securityTokenResolver);
    }


    private KeyInfoElement(Element keyinfo,
                           SecurityTokenResolver securityTokenResolver)
            throws SAXException, MissingResolverException
    {
        if (!"KeyInfo".equals(keyinfo.getLocalName())) throw new IllegalArgumentException("Element is not a KeyInfo element");
        if (!SoapUtil.DIGSIG_URI.equals(keyinfo.getNamespaceURI())) throw new IllegalArgumentException("KeyInfo element is not in dsig namespace");
        try {
            this.element = keyinfo;
            // TODO There can be multiple KeyIdentifiers in a single SecurityTokenReference
            //      We should fix this, but at least it isn't a security hole.
            Element x509Data = XmlUtil.findOnlyOneChildElementByName(keyinfo, SoapUtil.DIGSIG_URI, "X509Data");
            if (x509Data != null) {
                // Use X509Data
                Element x509CertEl = XmlUtil.findOnlyOneChildElementByName(x509Data, SoapUtil.DIGSIG_URI, "X509Certificate");
                if (x509CertEl == null) throw new SAXException("KeyInfo has no X509Data/X509Certificate");
                String certBase64 = XmlUtil.getTextValue(x509CertEl);
                byte[] certBytes = HexUtils.decodeBase64(certBase64, true);
                cert = CertUtils.decodeCert(certBytes);
                if (cert == null) throw new SAXException("KeyInfo includes certificate which cannot be recovered"); // can't happen
            } else {
                // No x509Data -- look for SecurityTokenReference next
                Element str = XmlUtil.findOnlyOneChildElementByName(keyinfo, SoapUtil.SECURITY_URIS_ARRAY, "SecurityTokenReference");
                if (str != null) {
                    // Use SecurityTokenReference
                    if (securityTokenResolver == null) throw new MissingResolverException("KeyInfo uses SecurityTokenReference but no certificate resolver is available");
                    Element keyid = XmlUtil.findOnlyOneChildElementByName(str, str.getNamespaceURI(), "KeyIdentifier");
                    if (keyid == null) throw new SAXException("KeyInfo has SecurityTokenReference but no KeyIdentifier");
                    String vt = keyid.getAttribute("ValueType");

                    X509Certificate gotCert;
                    String value = XmlUtil.getTextValue(keyid);
                    if (value == null || value.length() < 1) throw new SAXException("KeyInfo contains an empty KeyIdentifier");
                    if (vt == null) {
                        throw new SAXException("KeyInfo has null STR/KeyIdentifier ValueType");
                    } else if (vt.endsWith(SoapUtil.VALUETYPE_X509_THUMB_SHA1_SUFFIX)) {
                        gotCert = securityTokenResolver.lookup(value);
                    } else if (vt.endsWith(SoapUtil.VALUETYPE_SKI_SUFFIX)) {
                        gotCert = securityTokenResolver.lookupBySki(value);
                    } else {
                        throw new SAXException("KeyInfo uses STR/KeyIdentifier ValueType other than ThumbprintSHA1: " + vt);
                    }

                    if (gotCert == null) throw new SAXException("KeyInfo KeyIdentifier thumbprint did not match any X.509 certificate known to this recipient");
                    cert = gotCert;
                } else {
                    // No x509data or securitytokenreference -- try last-ditch KeyName lookup before giving up
                    Element keyNameEl = XmlUtil.findOnlyOneChildElementByName(keyinfo, SoapUtil.DIGSIG_URI, "KeyName");
                    if (keyNameEl == null) throw new SAXException("KeyInfo has no X509Data, KeyName, or SecurityTokenReference");
                    String keyName = XmlUtil.getTextValue(keyNameEl).trim();
                    if (keyName == null || keyName.length() < 1)
                        throw new SAXException("KeyInfo contains KeyName but it is empty");
                    // Use KeyName
                    if (securityTokenResolver == null) throw new MissingResolverException("KeyInfo uses KeyName but no key name resolver is available");
                    X509Certificate gotCert = securityTokenResolver.lookupByKeyName(keyName);
                    if (gotCert == null) throw new SAXException("KeyInfo KeyName did not match any X.509 certificate known to this recipient");
                    cert = gotCert;
                }
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

    /**
     * Make sure that the specified KeyInfo in fact is referring to the specified certificate.
     *
     * @param keyInfo    the KeyInfo element to check.  Must not be null.
     * @param cert       the X.509 certificate we expect it to be referring to.  Must not be null.
     * @throws InvalidDocumentFormatException   If we can't figure out the KeyInfo format.
     * @throws UnexpectedKeyInfoException       If we understood the KeyInfo, but it is not referring to our certificate.
     * @throws CertificateException             If we need the encoded form of the certificate but it is invalid.
     */
    public static void assertKeyInfoMatchesCertificate(Element keyInfo, X509Certificate cert)
            throws InvalidDocumentFormatException, UnexpectedKeyInfoException, CertificateException
    {
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
        byte[] keyIdValueBytes;
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
            byte[] ski = CertUtils.getSKIBytesFromCert(cert);
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
            byte[] certSha1 = HexUtils.getSha1Digest(certBytes);
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

    public static class UnsupportedKeyInfoFormatException extends InvalidDocumentFormatException {
        public UnsupportedKeyInfoFormatException(String message) { super(message); }
        public UnsupportedKeyInfoFormatException() {}
        public UnsupportedKeyInfoFormatException(Throwable cause) { super(cause); }
        public UnsupportedKeyInfoFormatException(String message, Throwable cause) { super(message, cause); }
    }
}
