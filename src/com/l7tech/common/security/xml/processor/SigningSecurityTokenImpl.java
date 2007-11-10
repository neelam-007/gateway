/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.token.SigningSecurityToken;
import com.l7tech.common.security.token.SignedPart;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds some common implementation for SigningSecurityTokens.
 */
abstract class SigningSecurityTokenImpl extends ParsedElementImpl implements SigningSecurityToken {
    private List<SignedElement> signedElements = new ArrayList();
    private List<SignedPart> signedParts = new ArrayList();
    protected boolean possessionProved = false;

    public SigningSecurityTokenImpl(Element element) {
        super(element);
    }

    /**
     * Use this contructor, and override makeElement(), to support lazy construction of the Element.
     */
    protected SigningSecurityTokenImpl() {
    }

    public SignedElement[] getSignedElements() {
        return signedElements.toArray(new SignedElement[signedElements.size()]);
    }

    public void addSignedElement(SignedElement signedElement) {
        if (signedElement == null) throw new NullPointerException();
        signedElements.add(signedElement);
    }

    public SignedPart[] getSignedParts() {
        return signedParts.toArray(new SignedPart[signedParts.size()]);
    }

    public void addSignedPart(SignedPart signedPart) {
        if (signedPart == null) throw new NullPointerException();
        signedParts.add(signedPart);
    }

    public void onPossessionProved() {
        possessionProved = true;
    }

    public boolean isPossessionProved() {
        return possessionProved;
    }
}
