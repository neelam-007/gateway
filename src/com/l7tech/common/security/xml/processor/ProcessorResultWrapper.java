/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;



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

    public ParsedElement[] getElementsThatWereEncrypted() {
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
}
