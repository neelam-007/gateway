/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.SecurityActor;
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

    public EncryptedElement[] getElementsThatWereEncrypted() {
        return delegate.getElementsThatWereEncrypted();
    }

    public SecurityToken[] getSecurityTokens() {
        return delegate.getSecurityTokens();
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

    /**
     * @param element the element to find the signing tokens for
     * @return the array if tokens that signed the element or empty array if none
     */
    public SigningSecurityToken[] getSigningTokens(Element element) {
        return delegate.getSigningTokens(element);
    }
}
