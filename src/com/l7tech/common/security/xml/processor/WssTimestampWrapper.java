/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.util.DateTranslator;
import org.w3c.dom.Element;

/**
 * @author mike
 */
public class WssTimestampWrapper implements WssTimestamp {
    private final WssTimestamp delegate;
    private final DateTranslator dateTranslator;

    public WssTimestampWrapper(WssTimestamp delegate) {
        this.delegate = delegate;
        this.dateTranslator = null;
    }

    public WssTimestampWrapper(WssTimestamp delegate, DateTranslator dateTranslator) {
        this.delegate = delegate;
        this.dateTranslator = dateTranslator;
    }

    private WssTimestampDate created = null;
    public WssTimestampDate getCreated() {
        if (created != null)
            return created;
        if (dateTranslator != null)
            return created = new WssTimestampDateWrapper(delegate.getCreated(),
                                                         dateTranslator.translate(delegate.getCreated().asDate()));
        return delegate.getCreated();
    }

    private WssTimestampDate expires = null;
    public WssTimestampDate getExpires() {
        if (expires != null)
            return expires;
        if (dateTranslator != null)
            return expires = new WssTimestampDateWrapper(delegate.getExpires(),
                                                         dateTranslator.translate(delegate.getExpires().asDate()));
        return delegate.getExpires();
    }

    public boolean isSigned() {
        return delegate.isSigned();
    }

    public SecurityToken[] getSigningSecurityTokens() {
        return delegate.getSigningSecurityTokens();
    }

    public Element asElement() {
        return delegate.asElement();
    }
}
