/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import com.l7tech.util.DomUtils;
import com.l7tech.util.NamespaceFactory;
import org.w3c.dom.Element;

/**
 * Populates a ds:KeyInfo with &lt;SecurityTokenReference&gt;&lt;Reference URI="#{@link #uri}" ValueType="{@link #valueType}"&gt;&lt;/&gt;&lt;/&gt;
 */
public class UriReferenceKeyInfoDetails extends KeyInfoDetails {
    private final String uri;         // Reference URI attribute.
    private final String valueType;

    /**
     * Prepare to create a new KeyInfo element using a reference URI.
     *
     * @param uri       The Reference target URI, including leading hash mark, or null if using a keyid value instead.
     * @param valueType The ValueType of the uriOrKeyId parameter.  Must not be null.
     * @throws IllegalArgumentException if both uri and value are null.
     */
    public UriReferenceKeyInfoDetails(String uri, String valueType) {
        this.valueType = valueType;
        this.uri = uri;
    }

    @Override
    public Element populateExistingKeyInfoElement(NamespaceFactory nsf, Element keyInfo) {
        final Element str = createStr(nsf, keyInfo);

        final Element refEl = DomUtils.createAndAppendElementNS(str, "Reference", nsf.getWsseNs(), "wsse");
        refEl.setAttribute("URI", uri);
        refEl.setAttribute("ValueType", valueType);

        return keyInfo;
    }
}
