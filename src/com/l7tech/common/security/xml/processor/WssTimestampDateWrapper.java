/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import org.w3c.dom.Element;

import java.util.Date;

/**
 * @author mike
 */
public class WssTimestampDateWrapper implements WssTimestampDate {
    private final WssTimestampDate delegate;
    private final Date dateOverride;

    public WssTimestampDateWrapper(WssTimestampDate delegate) {
        this.delegate = delegate;
        this.dateOverride = null;
    }

    public WssTimestampDateWrapper(WssTimestampDate delegate, Date dateOverride) {
        this.delegate = delegate;
        this.dateOverride = dateOverride;
    }

    public Date asDate() {
        return dateOverride != null ? dateOverride : delegate.asDate();
    }

    public Element asElement() {
        return delegate.asElement();
    }
}
