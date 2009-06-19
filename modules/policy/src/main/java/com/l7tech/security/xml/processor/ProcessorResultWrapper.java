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

    @Override
    public SignedElement[] getElementsThatWereSigned() {
        return delegate.getElementsThatWereSigned();
    }

    @Override
    public SignedPart[] getPartsThatWereSigned() {
        return delegate.getPartsThatWereSigned();
    }

    @Override
    public EncryptedElement[] getElementsThatWereEncrypted() {
        return delegate.getElementsThatWereEncrypted();
    }

    @Override
    public XmlSecurityToken[] getXmlSecurityTokens() {
        return delegate.getXmlSecurityTokens();
    }

    @Override
    public WssTimestamp getTimestamp() {
        return delegate.getTimestamp();
    }

    @Override
    public String getSecurityNS() {
        return delegate.getSecurityNS();
    }

    @Override
    public String getWSUNS() {
        return delegate.getWSUNS();
    }

    @Override
    public SecurityActor getProcessedActor() {
        return delegate.getProcessedActor();
    }

    @Override
    public String getProcessedActorUri() {
        return delegate.getProcessedActorUri();
    }

    @Override
    public List<String> getValidatedSignatureValues() {
        return delegate.getValidatedSignatureValues();
    }

    @Override
    public SignatureConfirmation getSignatureConfirmation() {
        return delegate.getSignatureConfirmation();
    }

    @Override
    public String getLastKeyEncryptionAlgorithm()
    {
        return delegate.getLastKeyEncryptionAlgorithm();
    }

    @Override
    public boolean isWsse11Seen() {
        return delegate.isWsse11Seen();
    }

    @Override
    public boolean isDerivedKeySeen() {
        return delegate.isDerivedKeySeen();
    }

    /**
     * @param element the element to find the signing tokens for
     * @return the array if tokens that signed the element or empty array if none
     */
    @Override
    public SigningSecurityToken[] getSigningTokens(Element element) {
        return delegate.getSigningTokens(element);
    }
}
