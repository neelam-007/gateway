/*
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import com.l7tech.util.HexUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
     */
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
}
