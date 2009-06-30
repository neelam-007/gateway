/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityToken;

/**
 * Provided by SecurityContextFinder TO the WssProcessor.  Result of looking up a session. WssProcessor will
 * never create an instance of this.
 */
public interface SecurityContext {

    /**
     * Get the shared secret for the session.
     *
     * @return The shared secret.
     */
    byte[] getSharedSecret();

    /**
     * Get the security token used to create the context (if any)
     *
     * @return The token, or null.
     */
    SecurityToken getSecurityToken();
}
