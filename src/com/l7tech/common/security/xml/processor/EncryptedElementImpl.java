/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.EncryptedElement;
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
