/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml.processor;

import com.l7tech.security.token.ParsedElement;
import org.w3c.dom.Element;

/**
 * Implementation of ParsedElement.  Single-threaded only.
 */
class ParsedElementImpl implements ParsedElement {
    protected Element element;

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
