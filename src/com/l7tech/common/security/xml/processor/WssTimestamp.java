/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

/**
 * @author mike
 */
public interface WssTimestamp extends ParsedElement {
    WssTimestampDate getCreated();
    WssTimestampDate getExpires();
    boolean isSigned();
    SecurityToken getSigningSecurityToken();
}
