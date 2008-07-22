/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.SoapConstants;

/**
 * Holds details about a KeyInfo.  Can be used to create a new KeyInfo element.  For parsing an existing
 * KeyInfo XML element, including resolving embedded cert references, see {@link KeyInfoElement}.
 */
public class KeyInfoDetails {
    // ----  One and only one of the following two values will always be non-null:
    final String uri;         // Reference URI attribute.
    final String value;       // KeyIdentifier child text node.
    // ----

    final String valueType;   // ValueType URI.  Must not be null or empty.  Must be a valid ValueType.
    final boolean isBase64;   // If true, will include EncodingType=Base64 in any generated KeyIdentifier.

    /**
     * Prepare to create a new KeyInfo element using either a reference URI or a KeyName.
     *
     * @param uri         The Reference target URI, including leading hash mark, or null if using a keyid value instead.
     * @param value       The text to go under the KeyIdentifier element, or null if using a reference uri instead.
     * @param valueType   The ValueType of the uriOrKeyId parameter.  Must not be null.
     * @param isBase64    If true, an EncodingType of Base64Binary will be included if a KeyIdentifier is generated.
     * @throws IllegalArgumentException if both uri and value are null.
     */
    private KeyInfoDetails(String uri, String value, String valueType, boolean isBase64) {
        if (uri == null && value == null) throw new IllegalArgumentException("Either uri or value must be provided");
        if (valueType == null) throw new IllegalArgumentException("valueType must be provided");
        this.uri = uri;
        this.value = value;
        this.valueType = valueType;
        this.isBase64 = isBase64;
    }

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
        if (!uri.startsWith("#")) uri = "#" + uri;
        return new KeyInfoDetails(uri, null, valueType, false);
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
        return new KeyInfoDetails(uri, null, valueType, false);
    }

    /**
     * Prepare to create a new KeyInfo element using a binary key identifier.
     *
     * @param identifier   the binary key identifier.  Must not be null.
     * @param valueType    the ValueType URI.  Must not be null.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeKeyId(byte[] identifier, String valueType) {
        return new KeyInfoDetails(null, HexUtils.encodeBase64(identifier, true), valueType, true);
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
        return new KeyInfoDetails(null, identifier, valueType, isBase64);
    }


    /**
     * Prepare to create a new KeyInfo element using an EncryptedKeySHA1.
     *
     * @param encryptedKeySha1  the EncryptedKeySHA1 to use.
     * @return a new KeyInfoDetails instance, ready to create the requested KeyInfo element.  Never null.
     */
    public static KeyInfoDetails makeEncryptedKeySha1Ref(String encryptedKeySha1) {
        return new KeyInfoDetails(null, encryptedKeySha1, SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1, false);
    }

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
        if (uri == null && value == null) throw new IllegalStateException(); // can't happen

        final Element keyInfo;

        // Check for an existing CipherData element.  If there is one, the KeyInfo needs to come before it.
        Element cipherData = DomUtils.findFirstChildElementByName(parent, SoapConstants.XMLENC_NS, "CipherData");
        if (cipherData == null)
            keyInfo = DomUtils.createAndAppendElementNS(parent, "KeyInfo", SoapConstants.DIGSIG_URI, "dsig");
        else
            keyInfo = DomUtils.createAndInsertBeforeElementNS(cipherData, "KeyInfo", SoapConstants.DIGSIG_URI, "dsig");

        return populateExistingKeyInfoElement(nsf, keyInfo);
    }

    /**
     * Add our information to an existing presumably-empty KeyInfo element.
     *
     * @param nsf     the NamespaceFactory to use when choosing namespaces.  Must not be null.
     * @param keyInfo the existing KeyInfo to which we will add our information.  Must not be null.
     * @return the newly-added-to element.  Never null.
     */
    public Element populateExistingKeyInfoElement(NamespaceFactory nsf, Element keyInfo) {
        Element str = DomUtils.createAndAppendElementNS(keyInfo,
                                                       SoapConstants.SECURITYTOKENREFERENCE_EL_NAME,
                                                       nsf.getWsseNs(), "wsse");

        populateExistingSecurityTokenReferenceElement(nsf, str);
        return keyInfo;
    }

    public Element populateExistingSecurityTokenReferenceElement(NamespaceFactory nsf, Element str) {
        if (value != null) {
            // Using a KeyIdentifier value
            // Create <KeyInfo><SecurityTokenReference><KeyIdentifier ValueType="ValueTypeURI">b64blah==</></></>
            Element keyId = DomUtils.createAndAppendElementNS(str, "KeyIdentifier",
                                                             nsf.getWsseNs(), "wsse");
            keyId.setAttribute("ValueType", valueType);
            if (isBase64)
                keyId.setAttribute("EncodingType", nsf.getEncodingType( SoapConstants.ENCODINGTYPE_BASE64BINARY, str));
            keyId.appendChild(DomUtils.createTextNode(str, value));

            return str;
        }

        // Using a URI reference
        // Create <KeyInfo><SecurityTokenReference><Reference URI="#Blah" ValueType="ValueTypeURI"></></></>
        Element refEl = DomUtils.createAndAppendElementNS(str, "Reference", nsf.getWsseNs(), "wsse");
        refEl.setAttribute("URI", uri);
        refEl.setAttribute("ValueType", valueType);
        return str;
    }
}
