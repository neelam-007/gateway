/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.ParsedElement;
import org.w3c.dom.Element;

/**
 * @author mike
 */
class ParsedElementImpl implements ParsedElement {
    private final Element element;

    public ParsedElementImpl(Element element) {
        this.element = element;
    }

    public Element asElement() {
        return element;
    }
}
