/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml.processor;

import com.l7tech.security.token.ParsedElement;

/**
 * @author mike
 */
public interface WssTimestamp extends ParsedElement {
    WssTimestampDate getCreated();
    WssTimestampDate getExpires();
}
