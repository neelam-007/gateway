/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.util.ISO8601Date;
import org.w3c.dom.Element;

import java.util.Date;

/**
 * @author mike
 */
public class WssTimestampDateWrapper implements WssTimestampDate {
    private final WssTimestampDate delegate;
    private final Date dateOverride;
    private String dateString;

    public WssTimestampDateWrapper(WssTimestampDate delegate) {
        this.delegate = delegate;
        this.dateOverride = null;
    }

    public WssTimestampDateWrapper(WssTimestampDate delegate, Date dateOverride) {
        this.delegate = delegate;
        this.dateOverride = dateOverride;
    }

    public long asTime() {
        return dateOverride != null ? dateOverride.getTime() : delegate.asTime();
    }

    public String asIsoString() {
        if (dateOverride == null) return delegate.asIsoString();
        synchronized (this) {
            if (dateString == null)
                dateString = ISO8601Date.format(dateOverride);
            return dateString;
        }
    }

    public Element asElement() {
        return delegate.asElement();
    }
}
