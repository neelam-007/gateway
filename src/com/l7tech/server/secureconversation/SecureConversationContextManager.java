package com.l7tech.server.secureconversation;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;

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
    public synchronized void saveSession(SecureConversationSession newSession) throws DuplicateSessionException {
        // Two sessions with same id is not allowed. ever (even if one is expired)
        SecureConversationSession alreadyExistingOne = getSession(newSession.getIdentifier());
        if (alreadyExistingOne != null) {
            throw new DuplicateSessionException("Session already exists with id " + newSession.getIdentifier());
        }
        sessions.put(newSession.getIdentifier(), newSession);
    }

    /**
     * Creates a new session and saves it
     * @param sessionOwner
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(User sessionOwner) throws DuplicateSessionException {
        final byte[] sharedSecret = generateNewSecret();
        String newSessionIdentifier = "http://www.layer7tech.com/uuid/" + randomuuid();
        // make up a new session identifier and shared secret (using some random generator)
        SecureConversationSession session = new SecureConversationSession();
        session.setCreation(System.currentTimeMillis());
        session.setExpiration(System.currentTimeMillis() + DEFAULT_SESSION_DURATION);
        session.setIdentifier(newSessionIdentifier);
        session.setSharedSecret(new SecretKey() {
            public byte[] getEncoded() {
                return sharedSecret;
            }
            public String getAlgorithm() {
                return "l7 shared secret";
            }
            public String getFormat() {
                return "l7 shared secret";
            }
        });
        session.setUsedBy(sessionOwner);
        saveSession(session);
        return session;
    }

    private byte[] generateNewSecret() {
        // return some random secret
        byte[] output = new byte[16];
        random.nextBytes(output);
        return output;
    }

    private String randomuuid() {
        // return some random encoded string
        byte[] output = new byte[20];
        random.nextBytes(output);
        return HexUtils.hexDump(output);
    }

    /**
     * For use by the WssProcessor on the ssg.
     */
    public WssProcessor.SecurityContext getSecurityContext(String securityContextIdentifier) {
        return getSession(securityContextIdentifier);
    }

    private SecureConversationContextManager() {
        // maybe in the future we use some distributed cache?
        // todo, something that deletes old sessions
    }

    /**
     * keys: identifier (string)
     * values: SecureConversationSession objects
     */
    private HashMap sessions = new HashMap();

    private static class SingletonHolder {
        private static SecureConversationContextManager singleton = new SecureConversationContextManager();
    }

    private final Logger logger = Logger.getLogger(SecureConversationContextManager.class.getName());
    private static final long DEFAULT_SESSION_DURATION = 1000*60*60*2; // 2 hrs?
    private static final Random random = new SecureRandom();
}
