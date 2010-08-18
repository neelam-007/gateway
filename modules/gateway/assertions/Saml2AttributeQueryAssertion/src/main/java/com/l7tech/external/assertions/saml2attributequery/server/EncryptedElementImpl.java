/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id: EncryptedElementImpl.java 11006 2005-06-02 00:26:12Z emil $
 */

package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.security.token.EncryptedElement;
import org.w3c.dom.Element;

/**
 * @author emil
 */
class EncryptedElementImpl implements EncryptedElement {
    private final Element element;
    private final String algorithm;

    public EncryptedElementImpl(Element element, String algorithm) {
        this.element = element;
        this.algorithm = algorithm;
    }

    public Element asElement() {
        return element;
    }

    /**
     * Returns the xml encryption algorithm such as
     * http://www.w3.org/2001/04/xmlenc#tripledes-cbc
     * that this element was encrypted with.
     */
    public String getAlgorithm() {
        return algorithm;
    }
}