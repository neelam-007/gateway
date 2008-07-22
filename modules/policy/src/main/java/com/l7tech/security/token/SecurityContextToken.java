/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.token;

import com.l7tech.security.xml.processor.SecurityContext;

/**
 * Represents a WS-SecureConversation SecurityContextToken.
 */
public interface SecurityContextToken extends SigningSecurityToken {
    SecurityContext getSecurityContext();
    String getContextIdentifier();
    boolean isPossessionProved();
}
