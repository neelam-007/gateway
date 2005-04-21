/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.ParsedElement;
import com.l7tech.common.security.token.SecurityToken;

/**
 * @author mike
 */
public interface WssTimestamp extends ParsedElement {
    WssTimestampDate getCreated();
    WssTimestampDate getExpires();
    boolean isSigned();
    SecurityToken[] getSigningSecurityTokens();
}
