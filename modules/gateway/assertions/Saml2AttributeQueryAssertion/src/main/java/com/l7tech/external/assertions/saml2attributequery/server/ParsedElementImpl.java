/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id: ParsedElementImpl.java 15168 2006-12-16 01:34:37Z mike $
 */

package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.security.token.ParsedElement;
import org.w3c.dom.Element;

/**
 * Implementation of ParsedElement.  Single-threaded only.
 */
class ParsedElementImpl implements ParsedElement {
    private Element element;

    public ParsedElementImpl(Element element) {
        this.element = element;
    }

    /**
     * Use this constructor, and override makeElement, to support lazily generating the element.
     */
    protected ParsedElementImpl() {
    }

    public Element asElement() {
        Element e = getElement();
        if (e != null) return e;
        e = makeElement();
        setElement(e);
        return e;
    }

    protected Element getElement() {
        return element;
    }

    protected void setElement(Element element) {
        this.element = element;
    }

    // Override this to populate element lazily.
    protected Element makeElement() {
        throw new UnsupportedOperationException("Unable to create an element");
    }

}