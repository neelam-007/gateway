package com.l7tech.server.secureconversation;

import com.l7tech.common.security.xml.WssProcessor;

import javax.crypto.SecretKey;

/**
 * Server-side manager that manages the SecureConversation sessions.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationContextManager implements WssProcessor.SecurityContextFinder {

    /**
     * Retrieve a session previously recorded.
     */
    public SecureConversationSession getSession(String identifier) {
        // todo
        return null;
    }

    /**
     * For use by the token service. Records and remembers a session for the duration specified.
     */
    public void saveSession(SecureConversationSession newSession) {
        // todo
    }

    /**
     * For use by the WssProcessor on the ssg.
     */
    public WssProcessor.SecurityContext getSecurityContext(String securityContextIdentifier) {
        final SecureConversationSession session = getSession(securityContextIdentifier);
        if (session == null) return null;
        return new WssProcessor.SecurityContext() {
            public SecretKey getSecretKey() {
                return session.getSharedSecret();
            }
        };
    }
}
