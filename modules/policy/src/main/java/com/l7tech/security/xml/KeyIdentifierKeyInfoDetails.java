/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import org.w3c.dom.Element;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;

/**
 * Creates and populates a KeyInfo/SecurityTokenReference/KeyIdentifier element with a provided {@link #valueType} and
 * {@link #value}.
 *
 * @author alex
 */
public class KeyIdentifierKeyInfoDetails extends KeyInfoDetails {
    private final String valueType;
    private final boolean isBase64;
    private final String value;       // KeyIdentifier child text node.

    /**
     * @param value       The text to go under the KeyIdentifier element, or null if using a reference uri instead.
     * @param valueType
     * @param isBase64
     */
    public KeyIdentifierKeyInfoDetails(String value, String valueType, boolean isBase64) {
        this.value = value;
        this.valueType = valueType;
        this.isBase64 = isBase64;
    }


    @Override
    public Element populateExistingKeyInfoElement(NamespaceFactory nsf, Element keyInfo) {
        Element str = createStr(nsf, keyInfo);

        // Using a KeyIdentifier value
        // Create <KeyInfo><SecurityTokenReference><KeyIdentifier ValueType="ValueTypeURI">b64blah==</></></>
        Element keyId = DomUtils.createAndAppendElementNS(str, "KeyIdentifier", nsf.getWsseNs(), "wsse");
        keyId.setAttribute("ValueType", valueType);
        if (isBase64)
            keyId.setAttribute("EncodingType", nsf.getEncodingType( SoapConstants.ENCODINGTYPE_BASE64BINARY, str));
        keyId.appendChild(DomUtils.createTextNode(str, value));

        return keyInfo;
    }

    public boolean isX509ValueReference() {
        return SoapConstants.VALUETYPE_SKI.equals(valueType) ||
               SoapConstants.VALUETYPE_X509_THUMB_SHA1.equals(valueType);
    }
}
