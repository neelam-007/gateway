/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml.processor;

import com.l7tech.security.token.EncryptedElement;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.SignedPart;
import com.l7tech.security.xml.SecurityActor;
import org.w3c.dom.Element;


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

    public String getLastSignatureValue() {
        return delegate.getLastSignatureValue();
    }

    public String getLastSignatureConfirmation()
    {
        return delegate.getLastSignatureConfirmation();
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
