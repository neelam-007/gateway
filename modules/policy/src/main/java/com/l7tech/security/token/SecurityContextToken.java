package com.l7tech.security.token;

import com.l7tech.security.xml.processor.SecurityContext;

/**
 * Represents a WS-SecureConversation SecurityContextToken.
 */
public interface SecurityContextToken extends SigningSecurityToken {
    SecurityContext getSecurityContext();
    String getContextIdentifier();
    String getWsscNamespace();
}
