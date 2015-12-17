package com.l7tech.security.xml;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.SoapConstants;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Holds details about a KeyInfo.  Can be used to create a new KeyInfo element.  For parsing an existing
 * KeyInfo XML element, including resolving embedded cert references, see {@link KeyInfoElement}.
 */
public abstract class KeyInfoDetails {
    /**
     * Prepare to create a new KeyInfo element using a Reference URI.  A leading hash mark will be added
     * to the URI by this method.
     *
     * @param uri        the Reference URI, not including leading hash mark: one will be added.  Must not be null.
     * @param valueType  the ValueType URI.  Must not be null.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     * @deprecated Use makeUriReferenceRaw instead
     */
    @Deprecated
    public static KeyInfoDetails makeUriReference(String uri, String valueType) {
        // TODO replace all uses of this method with makeUriReferenceRaw, then rename that method to this name
        return new UriReferenceKeyInfoDetails(uri.startsWith("#") ? uri : "#" + uri, valueType);
    }

    /**
     * Prepare to create a new KeyInfo element using a Reference URI.  No leading hash mark will be added
     * to the URI by this method.
     *
     * @param uri        the Reference URI, including a leading hash mark if needed: this method won't add one.  Must not be null.
     * @param valueType  the ValueType URI.  Must not be null.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeUriReferenceRaw(String uri, String valueType) {
        return new UriReferenceKeyInfoDetails(uri, valueType);
    }

    /**
     * Prepare to create a new KeyInfo element using a binary key identifier.
     *
     * @param identifier   the binary key identifier.  Must not be null.
     * @param valueType    the ValueType URI.  Must not be null.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeKeyId(byte[] identifier, String valueType) {
        return new KeyIdentifierKeyInfoDetails(HexUtils.encodeBase64(identifier, true), valueType, true);
    }

    /**
     * Prepare to create a new KeyInfo element using a key identifier string.
     *
     * @param identifier  the key identifier string.  Must not be null.
     * @param isBase64    true if identifier is Base64-encoded.  If so, an EncodingType of base64binary will be included in the generated KeyInfo.
     * @param valueType    the ValueType URI.  Must not be null.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeKeyId(String identifier, boolean isBase64, String valueType) {
        return new KeyIdentifierKeyInfoDetails(identifier, valueType, isBase64);
    }


    /**
     * Prepare to create a new KeyInfo element using an EncryptedKeySHA1.
     *
     * @param encryptedKeySha1  the EncryptedKeySHA1 to use.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeEncryptedKeySha1Ref(String encryptedKeySha1) {
        return new KeyIdentifierKeyInfoDetails(encryptedKeySha1, SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1, false);
    }

    /**
     * Create a free-floating KeyInfo element within the specific DOM but do not add it as a child of
     * any existing nodes within the DOM.
     *
     * @param domFactory  document to use to create the new element.  Required.
     * @param nsf     the NamespaceFactory to use when choosing namespaces.  Must not be null.
     * @param dsigPrefix a namespace prefix for the DIGSIG_URI that caller guarantees will be in-scope
     *                   when the created element is finally inserted into the document.  Caller must
     *                   manually add a namespace declaration for this prefix if one turns out to be required
     *                   when the element is added to the document.
     *                   <p/>
     *                   This prefix may be null, in which case the created KeyInfo element will assume that
     *                   DIGSIG_URI will be the default namespace at the eventual insertion point and will
     *                   create the element with no namespace prefix.
     * @return a newly-created free-floating KeyInfo Element.  Never null.
     */
    public Element createKeyInfoElement(Document domFactory, NamespaceFactory nsf, String dsigPrefix) {
        String elname = dsigPrefix == null ? "KeyInfo" : (dsigPrefix + ":" + "KeyInfo");
        Element keyInfo = domFactory.createElementNS(SoapConstants.DIGSIG_URI, elname);
        return populateExistingKeyInfoElement(nsf, keyInfo);
    }

    /**
     * Prepare to create a new KeyInfo element using KeyInfo/[SecurityTokenReference/]X509Data/X509IssuerSerial.
     *
     * @param certificate the certificate from which to extract the Issuer DN and Serial number.
     * @param includeStr <code>true</code> to interpose an otherwise empty wsse:SecurityTokenReference between the
     *                   KeyInfo and the X509Data (required by WS-Security but sort of dumb), or <code>false</code> to
     *                   make the X509Data a direct child of the KeyInfo.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeIssuerSerial(X509Certificate certificate, boolean includeStr) {
        return new IssuerSerialKeyInfoDetails(certificate, includeStr);
    }

    /**
     * Prepare to create a new KeyInfo element using KeyInfo/SecurityTokenReference/KeyIdentifier whose value is a certificate ThumbprintSHA1 in Base-64.
     *
     * @param recipientCertificate the recipient certificate to examine.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     * @throws CertificateEncodingException if the certificate could not be encoded in order to calculate its ThumbprintSHA1.
     */
    public static KeyInfoDetails makeThumbprintSha1(X509Certificate recipientCertificate) throws CertificateEncodingException {
        String thumb = CertUtils.getThumbprintSHA1(recipientCertificate);
        return new KeyIdentifierKeyInfoDetails(thumb, SoapConstants.VALUETYPE_X509_THUMB_SHA1, true);
    }

    /**
     * Prepare to create a new KeyInfo element using KeyInfo/SecurityTokenReference/KeyIdentifier[@valueType="...#X509v3]
     *
     * @param certificate the certificate from which to extract the Issuer DN and Serial number.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeStrKeyIdLiteralX509( X509Certificate certificate ) throws CertificateEncodingException {
        String value = HexUtils.encodeBase64( certificate.getEncoded(), true );
        return new KeyIdentifierKeyInfoDetails( value, SoapConstants.VALUETYPE_X509, true );
    }

    /**
     * Prepare to create a new KeyInfo element using KeyInfo/[SecurityTokenReference/]KeyName.
     *
     * @param certificate the certificate from which to extract the Subject DN.
     * @param includeStr <code>true</code> to interpose an otherwise empty wsse:SecurityTokenReference between the
     *                   KeyInfo and the KeyName (required by BSP), or <code>false</code> to
     *                   make the KeyName a direct child of the KeyInfo.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    @NotNull
    public static KeyInfoDetails makeKeyName( @NotNull X509Certificate certificate, boolean includeStr ) {
        return new KeyNameKeyInfoDetails( certificate, includeStr );
    }

    /**
     * Add our information to an existing presumably-empty KeyInfo element.
     *
     * @param nsf     the NamespaceFactory to use when choosing namespaces.  Must not be null.
     * @param keyInfo the existing KeyInfo to which we will add our information.  Must not be null.
     * @return the newly-added-to element.  Never null.
     */
    public abstract Element populateExistingKeyInfoElement(NamespaceFactory nsf, Element keyInfo);

    /**
     * Add a new KeyInfo element to the specified parent element.  The new KeyInfo will be appended to the end
     * of the parent element unless it already contains a CipherData element, in which case the KeyInfo
     * will be positioned before the CipherData element to comply with the xenc schema.
     *
     * @param nsf     the NamespaceFactory to use when choosing namespaces.  Must not be null.
     * @param parent  the parent to which we'll be adding a new KeyInfo element.  Must not be null.
     * @return the newly-added element.  Never null.
     */
    public Element createAndAppendKeyInfoElement(NamespaceFactory nsf, Node parent) {
        final Element keyInfo;

        // Check for an existing CipherData element.  If there is one, the KeyInfo needs to come before it.
        Element cipherData = DomUtils.findFirstChildElementByName(parent, SoapConstants.XMLENC_NS, "CipherData");
        if (cipherData == null)
            keyInfo = DomUtils.createAndAppendElementNS(parent, "KeyInfo", SoapConstants.DIGSIG_URI, "dsig");
        else
            keyInfo = DomUtils.createAndInsertBeforeElementNS(cipherData, "KeyInfo", SoapConstants.DIGSIG_URI, "dsig");

        return populateExistingKeyInfoElement(nsf, keyInfo);
    }

    protected static Element createStr(NamespaceFactory nsf, Element keyInfo) {
        return DomUtils.createAndAppendElementNS(keyInfo, SoapConstants.SECURITYTOKENREFERENCE_EL_NAME, nsf.getWsseNs(), "wsse");
    }

    /**
     * Return false if the referenced token is present in the message.
     *
     * @return true for a value reference.
     */
    public abstract boolean isX509ValueReference();
}
