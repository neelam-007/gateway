package com.l7tech.server.secureconversation;

import com.l7tech.common.security.xml.WssProcessor;

import javax.crypto.SecretKey;
import java.util.HashMap;

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

    public static SecureConversationContextManager getInstance() {
        return SingletonHolder.singleton;
    }

    /**
     * Retrieve a session previously recorded.
     */
    public synchronized SecureConversationSession getSession(String identifier) {
        return (SecureConversationSession)sessions.get(identifier);
    }

    /**
     * For use by the token service. Records and remembers a session for the duration specified.
     */
    public synchronized void saveSession(SecureConversationSession newSession) {
        // Two sessions with same id is not allowed. ever (even if one is expired)
        SecureConversationSession alreadyExistingOne = getSession(newSession.getIdentifier());
        if (alreadyExistingOne != null) {
            // todo some special exception
        }
        sessions.put(newSession.getIdentifier(), newSession);
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

    private SecureConversationContextManager() {
        // maybe in the future we use some distributed cache?
    }

    /**
     * keys: identifier (string)
     * values: SecureConversationSession objects
     */
    private HashMap sessions = new HashMap();

    private static class SingletonHolder {
        private static SecureConversationContextManager singleton = new SecureConversationContextManager();
    }
}
