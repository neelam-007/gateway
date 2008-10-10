/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml.processor;

import com.l7tech.security.token.*;
import com.l7tech.security.xml.SecurityActor;
import org.w3c.dom.Element;

import java.util.List;

/**
 * @author mike
 */
public class ProcessorResultWrapper implements ProcessorResult {
    private ProcessorResult delegate;

    public ProcessorResultWrapper(ProcessorResult delegate) {
        this.delegate = delegate;
    }

    public SignedElement[] getElementsThatWereSigned() {
        return delegate.getElementsThatWereSigned();
    }

    public SignedPart[] getPartsThatWereSigned() {
        return delegate.getPartsThatWereSigned();
    }

    public EncryptedElement[] getElementsThatWereEncrypted() {
        return delegate.getElementsThatWereEncrypted();
    }

    public XmlSecurityToken[] getXmlSecurityTokens() {
        return delegate.getXmlSecurityTokens();
    }

    public WssTimestamp getTimestamp() {
        return delegate.getTimestamp();
    }

    public String getSecurityNS() {
        return delegate.getSecurityNS();
    }

    public String getWSUNS() {
        return delegate.getWSUNS();
    }

    public SecurityActor getProcessedActor() {
        return delegate.getProcessedActor();
    }

    public List<String> getValidatedSignatureValues() {
        return delegate.getValidatedSignatureValues();
    }

    public List<SignatureConfirmation> getSignatureConfirmationValues() {
        return delegate.getSignatureConfirmationValues();
    }

    public String getLastKeyEncryptionAlgorithm()
    {
        return delegate.getLastKeyEncryptionAlgorithm();
    }

    public boolean isWsse11Seen() {
        return delegate.isWsse11Seen();
    }

    public boolean isDerivedKeySeen() {
        return delegate.isDerivedKeySeen();
    }

    /**
     * @param element the element to find the signing tokens for
     * @return the array if tokens that signed the element or empty array if none
     */
    public SigningSecurityToken[] getSigningTokens(Element element) {
        return delegate.getSigningTokens(element);
    }
}
