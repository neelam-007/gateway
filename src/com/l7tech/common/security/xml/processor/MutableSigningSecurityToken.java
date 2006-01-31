/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.token.SigningSecurityToken;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds some common implementation for SigningSecurityTokens.
 */
abstract class MutableSigningSecurityToken extends ParsedElementImpl implements SigningSecurityToken {
    public static final SignedElement[] PROTO = new SignedElement[0];
    private List signedElements = new ArrayList();
    protected boolean possessionProved = false;

    public MutableSigningSecurityToken(Element element) {
        super(element);
    }

    public SignedElement[] getSignedElements() {
        return (SignedElement[])signedElements.toArray(PROTO);
    }

    /** @param signedElement the signed element to record.  Must not be null. */
    void addSignedElement(SignedElement signedElement) {
        if (signedElement == null) throw new NullPointerException();
        signedElements.add(signedElement);
    }

    public void onPossessionProved() {
        possessionProved = true;
    }

    public boolean isPossessionProved() {
        return possessionProved;
    }
}
