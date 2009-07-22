/*
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.util.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.List;
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
    private static final EnumSet<KeyInfoInclusionType> CERT_ONLY = EnumSet.of(KeyInfoInclusionType.CERT);

    /** Exception throws if a certificate resolver is required to parse a KeyInfo, but none is available. */
    @SuppressWarnings({"UnusedDeclaration"})
    public static class MissingResolverException extends Exception {
        private MissingResolverException() {}
        private MissingResolverException(String message) { super(message); }
        private MissingResolverException(String message, Throwable cause) { super(message, cause); }
        private MissingResolverException(Throwable cause) { super(cause); }
    }

    /**
     * Parse the specified KeyInfo element.
     *
     * @param keyinfo the element to parse.  Must not be null.  Must be a ds:KeyInfo element in proper namespace.
     * @param securityTokenResolver resolver for X.509 sha1 thumbprints, or null to disable thumbprint support.
     * @return the parsed KeyInfoElement.  Never null.
     * @throws NullPointerException if it's null
     * @throws IllegalArgumentException if it isn't a ds:KeyInfo element
     * @throws SAXException if the format of this KeyInfo is invalid or not supported
     * @throws MissingResolverException if this KeyInfo refers to an X509 SHA1 thumbprint or keyname, but no certificate resolver was supplied.
     *
     */
    public static KeyInfoElement parse(Element keyinfo, SecurityTokenResolver securityTokenResolver)
            throws SAXException, MissingResolverException
    {
        return parse(keyinfo, securityTokenResolver, CERT_ONLY);
    }

    /**
     * Parse the specified KeyInfo element.
     *
     * @param keyinfo the element to parse.  Must not be null.  Must be a ds:KeyInfo element in proper namespace.
     * @param securityTokenResolver resolver for X.509 sha1 thumbprints, or null to disable thumbprint support.
     * @param allowedTypes which KeyInfoInclusionTypes should be permitted
     * @return the parsed KeyInfoElement.  Never null.
     * @throws NullPointerException if it's null
     * @throws IllegalArgumentException if it isn't a ds:KeyInfo element
     * @throws SAXException if the format of this KeyInfo is invalid or not supported
     * @throws MissingResolverException if this KeyInfo refers to an X509 SHA1 thumbprint or keyname, but no certificate resolver was supplied.
     *
     */
    public static KeyInfoElement parse(Element keyinfo, SecurityTokenResolver securityTokenResolver, EnumSet<KeyInfoInclusionType> allowedTypes)
            throws SAXException, MissingResolverException
    {
        return new KeyInfoElement(keyinfo, securityTokenResolver, allowedTypes);
    }


    private KeyInfoElement(final Element keyinfo,
                           final SecurityTokenResolver securityTokenResolver,
                           final EnumSet<KeyInfoInclusionType> allowedTypes)
            throws SAXException, MissingResolverException
    {
        if (!"KeyInfo".equals(keyinfo.getLocalName())) throw new IllegalArgumentException("Element is not a KeyInfo element");
        if (!SoapConstants.DIGSIG_URI.equals(keyinfo.getNamespaceURI())) throw new IllegalArgumentException("KeyInfo element is not in dsig namespace");
        try {
            this.element = keyinfo;

            Element str = DomUtils.findOnlyOneChildElementByName(keyinfo, SoapConstants.SECURITY_URIS_ARRAY, "SecurityTokenReference");
            if (str != null) {
                // Use SecurityTokenReference
                if (securityTokenResolver == null) throw new MissingResolverException("KeyInfo uses SecurityTokenReference but no certificate resolver is available");

                X509Certificate gotCert;
                Element keyid = DomUtils.findOnlyOneChildElementByName(str, str.getNamespaceURI(), "KeyIdentifier");
                Element x509Data = DomUtils.findOnlyOneChildElementByName(str, SoapConstants.DIGSIG_URI, "X509Data");
                if (keyid != null) {
                    String vt = keyid.getAttribute("ValueType");

                    // TODO There can be multiple KeyIdentifiers in a single SecurityTokenReference
                    //      We should fix this, but at least it isn't a security hole.
                    String value = DomUtils.getTextValue(keyid);
                    if (value == null || value.length() < 1) throw new SAXException("KeyInfo contains an empty KeyIdentifier");
                    if (vt == null) {
                        throw new SAXException("KeyInfo has null STR/KeyIdentifier ValueType");
                    } else if (vt.endsWith( SoapConstants.VALUETYPE_X509_THUMB_SHA1_SUFFIX)) {
                        gotCert = securityTokenResolver.lookup(value);
                    } else if (vt.endsWith( SoapConstants.VALUETYPE_SKI_SUFFIX)) {
                        gotCert = securityTokenResolver.lookupBySki(value);
                    } else {
                        throw new SAXException("KeyInfo uses STR/KeyIdentifier ValueType other than ThumbprintSHA1: " + vt);
                    }
                } else if (x509Data != null) {
                    gotCert = handleX509Data(x509Data, securityTokenResolver, allowedTypes);
                } else {
                    // No x509data or securitytokenreference -- try last-ditch KeyName lookup before giving up
                    Element keyNameEl = DomUtils.findOnlyOneChildElementByName(keyinfo, SoapConstants.DIGSIG_URI, "KeyName");
                    if (keyNameEl == null) throw new SAXException("KeyInfo has no X509Data, KeyName, or SecurityTokenReference");
                    String keyName = DomUtils.getTextValue(keyNameEl).trim();
                    if (keyName == null || keyName.length() < 1)
                        throw new SAXException("KeyInfo contains KeyName but it is empty");
                    // Use KeyName
                    gotCert = securityTokenResolver.lookupByKeyName(keyName);
                    if (gotCert == null) throw new SAXException("KeyInfo KeyName did not match any X.509 certificate known to this recipient");
                }

                if (gotCert == null) throw new SAXException("KeyInfo SecurityTokenReference did not match any X.509 certificate known to this recipient");
                cert = gotCert;
            } else {
                // Finally try the rare but appropriate KeyInfo/X509Data
                Element x509Data = DomUtils.findOnlyOneChildElementByName(keyinfo, SoapConstants.DIGSIG_URI, "X509Data");
                if (x509Data == null)
                    throw new SAXException("KeyInfo did not contain any recognized certificate reference format");

                X509Certificate gotCert = handleX509Data(x509Data, securityTokenResolver, allowedTypes);
                if (gotCert == null) throw new SAXException("KeyInfo includes certificate which cannot be recovered");
                cert = gotCert;
            }
        } catch (InvalidDocumentFormatException e) {
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException("Invalid base64 sequence: " + e.getMessage(), e);
        } catch (CertificateException e) {
            throw new SAXException("Invalid X.509 certificate: " + e.getMessage(), e);
        }
    }

    private X509Certificate handleX509Data(Element x509Data, SecurityTokenResolver securityTokenResolver, EnumSet<KeyInfoInclusionType> allowedTypes)
        throws IOException, CertificateException, MissingResolverException, SAXException, TooManyChildElementsException,
        MissingRequiredElementException {
        // Use X509Data
        Element x509CertEl = getKid(x509Data, allowedTypes, KeyInfoInclusionType.CERT, "X509Certificate");
        Element x509SkiEl = getKid(x509Data, allowedTypes, KeyInfoInclusionType.CERT, "X509SKI");
        Element x509IssuerSerialEl = getKid(x509Data, allowedTypes, KeyInfoInclusionType.CERT, "X509IssuerSerial");
        if (x509CertEl != null) {
            String certBase64 = DomUtils.getTextValue(x509CertEl);
            byte[] certBytes = HexUtils.decodeBase64(certBase64, true);
            return CertUtils.decodeCert(certBytes);
        } else if (x509SkiEl != null) {
            if (securityTokenResolver == null) throw new MissingResolverException("KeyInfo uses X509Data/X509SKI but no certificate resolver is available");
            String skiRaw = DomUtils.getTextValue(x509SkiEl);
            String ski = HexUtils.encodeBase64(HexUtils.decodeBase64(skiRaw, true), true);
            return securityTokenResolver.lookupBySki(ski);
        } else if (x509IssuerSerialEl != null) {
            if (securityTokenResolver == null) throw new MissingResolverException("KeyInfo uses X509Data/X509IssuerSerial but no certificate resolver is available");
            final Element issuerEl = DomUtils.findExactlyOneChildElementByName(x509IssuerSerialEl, SoapConstants.DIGSIG_URI, "X509IssuerName");
            final Element serialEl = DomUtils.findExactlyOneChildElementByName(x509IssuerSerialEl, SoapConstants.DIGSIG_URI, "X509SerialNumber");

            final String issuerVal = DomUtils.getTextValue(issuerEl);
            if (issuerVal.length() == 0) throw new SAXException("X509IssuerName was empty");
            final String serialVal = DomUtils.getTextValue(serialEl);
            if (serialVal.length() == 0) throw new SAXException("X509Serial was empty");
            return securityTokenResolver.lookupByIssuerAndSerial(new X500Principal(issuerVal), new BigInteger(serialVal));
        } else {
            throw new SAXException("KeyInfo X509Data did not contain one of " + allowedTypes);
        }
    }

    private Element getKid(final Element x509Data,
                           final EnumSet<KeyInfoInclusionType> allowedTypes,
                           final KeyInfoInclusionType expectedType,
                           final String elementName)
        throws TooManyChildElementsException
    {
        return allowedTypes.contains(expectedType) ? DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, elementName) : null;
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
     * @param recipientCert the expected recipient certificate.  Required.
     * @throws com.l7tech.util.InvalidDocumentFormatException  if there was a problem with the encryptedType, or the KeyInfo didn't match.
     * @throws com.l7tech.security.xml.UnexpectedKeyInfoException      if the keyinfo did not match the recipientCert
     * @throws java.security.GeneralSecurityException        if there was a problem with the recipient certificate or a certificate
     *                                         embedded within the encryptedType.
     */
    public static void checkKeyInfo(Element encryptedType, X509Certificate recipientCert)
            throws UnexpectedKeyInfoException, InvalidDocumentFormatException, GeneralSecurityException
    {
        SimpleSecurityTokenResolver resolver = new SimpleSecurityTokenResolver();
        resolver.addPrivateKey(new SignerInfo(null, new X509Certificate[] { recipientCert }));
        getTargetPrivateKeyForEncryptedType(encryptedType, resolver, null);
    }

    /**
     * Checks if the specified EncryptedType's KeyInfo is addressed to the specified recipient certificate.
     * An EncryptedType is an abstract type in the XML Encryption schema representing an element that can contain,
     * among other things, zero or more KeyInfo subelements identifying possible recipients able to decrypt
     * the EncryptedType.  Concrete examples are EncryptedKey and EncryptedData.
     *
     * @param encryptedType the EncryptedKey or EncryptedData element.  Must include a KeyInfo child.
     * @param securityTokenResolver resolver for private keys
     * @param certResolver resolver for certificates by identifier
     * @return a SignerInfo containing the matching private key and certificate chain.  Never null.
     * @throws com.l7tech.util.InvalidDocumentFormatException  if there was a problem with the encryptedType, or the KeyInfo didn't match.
     * @throws com.l7tech.security.xml.UnexpectedKeyInfoException      if the keyinfo did not match any known private key
     * @throws java.security.GeneralSecurityException        if there was a problem with the recipient certificate or a certificate
     *                                         embedded within the encryptedType.
     */
    public static SignerInfo getTargetPrivateKeyForEncryptedType(Element encryptedType, SecurityTokenResolver securityTokenResolver, Resolver<String,X509Certificate> certResolver)
            throws UnexpectedKeyInfoException, InvalidDocumentFormatException, GeneralSecurityException
    {
        // bugzilla #1582
        if (securityTokenResolver == null) {
            // if we dont have a recipient cert, then obviously, this is not meant for us. this would happen for example
            // when the agent is processing a response from the ssg that has an encryptedkey in it but the client account
            // does not have a client cert. (this is possible if the encryption is meant for upstream client)
            throw new UnexpectedKeyInfoException("No securityTokenResolver available.  Unable to check if this KeyInfo is intended for us.");
        }

        List<Element> keyInfos = DomUtils.findChildElementsByName(encryptedType, SoapConstants.DIGSIG_URI, SoapConstants.KINFO_EL_NAME);
        if (keyInfos == null || keyInfos.size() < 1)
            throw new InvalidDocumentFormatException(encryptedType.getLocalName() + " includes no KeyInfo element");
        for (Element keyInfo : keyInfos) {
            SignerInfo found = getTargetPrivateKeyForKeyInfo(keyInfo, securityTokenResolver, certResolver);
            if (found != null)
                return found;
        }

        throw new UnexpectedKeyInfoException("KeyInfo did not resolve to any local certificate with a known private key");
    }

    private static final PrivateKey FAKE_KEY = new PrivateKey() {
        public String getAlgorithm() {
            return null;
        }

        public String getFormat() {
            return null;
        }

        public byte[] getEncoded() {
            return new byte[0];
        }
    };

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
        if (null == getTargetPrivateKeyForKeyInfo(keyInfo, new SimpleSecurityTokenResolver(cert, FAKE_KEY), null))
            throw new UnexpectedKeyInfoException("KeyInfo was not recognized referring to the expected certificate");
    }

    /**
     * Attempt to decode an embedded certificate from a KeyInfo.
     *
     * @param keyInfo the KeyInfo element to examine.  Required.
     * @return the X509Certificate that was embedded in this element, or null if none was found.
     * @throws com.l7tech.util.InvalidDocumentFormatException if more than one X509Data or X509Certificate child element is present.
     * @throws java.security.cert.CertificateException if a certificate is present but cannot be decoded.
     */
    public static X509Certificate decodeEmbeddedCert(Element keyInfo) throws InvalidDocumentFormatException, CertificateException {
        // Check for X509Data
        Element x509DataEle = DomUtils.findOnlyOneChildElementByName(keyInfo, SoapConstants.DIGSIG_URI, "X509Data");
        if (x509DataEle == null)
            return null;

        Element x509CertEle = DomUtils.findOnlyOneChildElementByName(x509DataEle, SoapConstants.DIGSIG_URI, "X509Certificate");
        if (x509CertEle == null) {
            logger.log(Level.FINE, "Ignoring X509Data with no X509Certificate child element");
            return null;
        }

        return CertUtils.decodeCert(HexUtils.decodeBase64(DomUtils.getTextValue(x509CertEle)));
    }

    /**
     * Try to look up a SignerInfo (private key and cert chain) corresponding to the specified KeyInfo element.
     *
     * @param keyInfo    the KeyInfo element to check.  Must not be null.
     * @param securityTokenResolver resolver for private keys.  required
     * @param certResolver resolver for certificates by identifier, to resolve references to BSTs in the same message that are carrying certificate bytes,
     *                     or null if no Reference URIs to BSTs within the same message should be followed
     * @return the private key and cert chain for this private key, or null if we didn't recognize this KeyInfo.
     * @throws InvalidDocumentFormatException   If we can't figure out the KeyInfo format.
     * @throws CertificateException             If we need the encoded form of the certificate but it is invalid.
     */
    public static SignerInfo getTargetPrivateKeyForKeyInfo(Element keyInfo, SecurityTokenResolver securityTokenResolver, Resolver<String,X509Certificate> certResolver)
            throws InvalidDocumentFormatException, CertificateException
    {
        Element str = DomUtils.findOnlyOneChildElementByName(keyInfo,
                                                            SoapConstants.SECURITY_URIS_ARRAY,
                                                            SoapConstants.SECURITYTOKENREFERENCE_EL_NAME);
        if (str == null) {
            X509Certificate embedded = decodeEmbeddedCert(keyInfo);
            if (embedded != null) {
                SignerInfo found = securityTokenResolver.lookupPrivateKeyByCert(embedded);
                if (found != null)
                    return found;

                logger.fine("Ignoring embedded certificate which was unrecognized");
                // Fallthrough and look for other keyinfo.
            }
        }

        Element ki = DomUtils.findOnlyOneChildElementByName(str,
                                                           SoapConstants.SECURITY_URIS_ARRAY,
                                                           SoapConstants.KEYIDENTIFIER_EL_NAME);
        if (ki == null) {
            Element reference = DomUtils.findOnlyOneChildElementByName(str,
                                                              SoapConstants.SECURITY_URIS_ARRAY,
                                                              SoapConstants.REFERENCE_EL_NAME);
            if (reference == null || certResolver == null) {
                // no reference or keyidentifier
                throw new UnsupportedKeyInfoFormatException("KeyInfo's SecurityTokenReference includes no KeyIdentifier element");
            } else {
                String uriAttr = reference.getAttribute("URI");
                if (uriAttr == null || uriAttr.length() < 1) {
                    throw new UnsupportedKeyInfoFormatException("KeyInfo contains a reference but the URI attribute cannot be obtained");
                }
                if (uriAttr.charAt(0) == '#') {
                    uriAttr = uriAttr.substring(1);
                }
                if (certResolver.resolve(uriAttr)==null) {
                    throw new InvalidDocumentFormatException("Invalid security token reference '"+uriAttr+"' in KeyInfo");
                }
                X509Certificate referencedCert = certResolver.resolve(uriAttr);
                SignerInfo found = securityTokenResolver.lookupPrivateKeyByCert(referencedCert);

                if (found != null) {
                    logger.fine("The Key recipient cert is recognized");
                    return found;
                } else {
                    String msg = "This KeyInfo declares a specific cert, " +
                            "but our certificate does not match.";
                    logger.warning(msg);
                    return null;
                }
            }
        } else {
            String valueType = ki.getAttribute("ValueType");
            String keyIdentifierValue = DomUtils.getTextValue(ki);
            if (valueType == null || valueType.length() <= 0) {
                logger.fine("The KeyId Value Type is not specified. We will therefore assume it is a Subject Key Identifier.");
                valueType = SoapConstants.VALUETYPE_SKI;
            }
            if (valueType.endsWith( SoapConstants.VALUETYPE_SKI_SUFFIX)) {
                SignerInfo found = securityTokenResolver.lookupPrivateKeyBySki(keyIdentifierValue);
                if (found != null) {
                    logger.fine("the Key SKI is recognized. This key is for us for sure!");
                    return found;
                } else {
                    String msg = "This KeyInfo declares a specific SKI, " +
                            "but our certificate's SKI does not match.";
                    logger.fine(msg);
                    return null;
                }
            } else if (valueType.endsWith( SoapConstants.VALUETYPE_X509_SUFFIX)) {
                // It seems to be a complete certificate
                byte[] keyIdValueBytes;
                keyIdValueBytes = HexUtils.decodeBase64(keyIdentifierValue, true);
                if (keyIdValueBytes == null || keyIdValueBytes.length < 1) throw new InvalidDocumentFormatException("KeyIdentifier was empty");
                X509Certificate referencedCert = CertUtils.decodeCert(keyIdValueBytes);
                SignerInfo found = securityTokenResolver.lookupPrivateKeyByCert(referencedCert);

                if (found != null) {
                    logger.fine("The Key recipient cert is recognized");
                    return found;

                } else {
                    String msg = "This KeyInfo declares a specific cert, " +
                            "but our certificate does not match.";
                    logger.warning(msg);
                    return null;
                }
            } else if (valueType.endsWith( SoapConstants.VALUETYPE_X509_THUMB_SHA1_SUFFIX))
            {
                SignerInfo found = securityTokenResolver.lookupPrivateKeyByX509Thumbprint(keyIdentifierValue);
                if (found != null) {
                    logger.fine("The cert SHA1 thumbprint was recognized.  The cert is ours for sure.");
                    return found;
                } else {
                    String msg = "This KeyInfo declares a specific cert SHA1 thumbprint, " +
                            "but our certificate's thumbprint does not match.";
                    logger.fine(msg);
                    return null;
                }
            } else if (valueType.endsWith( SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1_SUFFIX)) {
                // TODO - to support this, need to be able to look up a (long-)previously-processed EncryptedKey by its hash
                throw new UnsupportedKeyInfoFormatException("The EncryptedKey's KeyInfo uses an unsupported " +
                        "ValueType: " + valueType);
            } else
                throw new UnsupportedKeyInfoFormatException("The EncryptedKey's KeyInfo uses an unsupported " +
                        "ValueType: " + valueType);
        }
    }

    public static class UnsupportedKeyInfoFormatException extends InvalidDocumentFormatException {
        public UnsupportedKeyInfoFormatException(String message) { super(message); }
        public UnsupportedKeyInfoFormatException() {}
        public UnsupportedKeyInfoFormatException(Throwable cause) { super(cause); }
        public UnsupportedKeyInfoFormatException(String message, Throwable cause) { super(message, cause); }
    }
}
