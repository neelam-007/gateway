/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import com.l7tech.common.security.xml.processor.SecurityContext;

/**
 * Represents a WS-SecureConversation SecurityContextToken.
 */
public interface SecurityContextToken extends SecurityToken {
    SecurityContext getSecurityContext();
    String getContextIdentifier();
    boolean isPossessionProved();
}
