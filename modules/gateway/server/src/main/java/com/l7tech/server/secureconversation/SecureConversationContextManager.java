package com.l7tech.server.secureconversation;

import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
public class SecureConversationContextManager implements SecurityContextFinder {

    public static SecureConversationContextManager getInstance() {
        return SingletonHolder.singleton;
    }

    /**
     * For use by the WssProcessor on the ssg.
     */
    public SecurityContext getSecurityContext(String securityContextIdentifier) {
        return getSession(securityContextIdentifier);
    }

    /**
     * Retrieve a session previously recorded.
     */
    public SecureConversationSession getSession(String identifier) {
        checkExpiriedSessions();
        SecureConversationSession output = null;
        rwlock.readLock().lock();
        try {
            output = (SecureConversationSession)sessions.get(identifier);
        } finally {
            rwlock.readLock().unlock();
        }
        return output;
    }

    /**
     * For use by the token service. Records and remembers a session for the duration specified.
     */
    public void saveSession(SecureConversationSession newSession) throws DuplicateSessionException {
        rwlock.writeLock().lock();
        try {
            // Two sessions with same id is not allowed. ever (even if one is expired)
            SecureConversationSession alreadyExistingOne =
                    (SecureConversationSession) sessions.get(newSession.getIdentifier());
            if (alreadyExistingOne != null) {
                throw new DuplicateSessionException("Session already exists with id " + newSession.getIdentifier());
            }
            sessions.put(newSession.getIdentifier(), newSession);
            logger.finest("Saved SC context " + newSession.getIdentifier());
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Creates a new session and saves it
     * @param sessionOwner
     * @param credentials
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(User sessionOwner, LoginCredentials credentials, String namespace) throws DuplicateSessionException {
        byte[] tmp = null;
        if (namespace != null && namespace.equals( SoapConstants.WSSC_NAMESPACE2)) {
            tmp = generateNewSecret(32);
        } else {
            tmp = generateNewSecret(16);
        }
        final byte[] sharedSecret = tmp;
        String newSessionIdentifier = "http://www.layer7tech.com/uuid/" + randomuuid();
        // make up a new session identifier and shared secret (using some random generator)
        SecureConversationSession session = new SecureConversationSession();
        session.setCreation(System.currentTimeMillis());
        session.setExpiration(System.currentTimeMillis() + DEFAULT_SESSION_DURATION);
        session.setIdentifier(newSessionIdentifier);
        session.setCredentials(credentials);
        session.setSecConvNamespaceUsed(namespace);
        session.setSharedSecret(sharedSecret);
        session.setUsedBy(sessionOwner);
        saveSession(session);
        return session;
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionId the session identifier, you should use your own "namespace"
     * @param expiryTime the expiry in millis
     * @param sessionOwner the user
     * @param credentials the users creds
     * @param sharedKey the key for the session
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(String sessionId, long expiryTime, User sessionOwner, LoginCredentials credentials, byte[] sharedKey) throws DuplicateSessionException {
        long expires = expiryTime > 0 ? expiryTime : System.currentTimeMillis() + DEFAULT_SESSION_DURATION;
        SecureConversationSession session = new SecureConversationSession();
        session.setCreation(System.currentTimeMillis());
        session.setExpiration(expires);
        session.setIdentifier(sessionId);
        session.setCredentials(credentials);
        session.setSharedSecret(sharedKey);
        session.setUsedBy(sessionOwner);
        saveSession(session);
        return session;
    }

    private void checkExpiriedSessions() {
        long now = System.currentTimeMillis();
        long last;
        rwlock.readLock().lock();
        try {
            last = lastExpirationCheck;
        } finally {
            rwlock.readLock().unlock();
        }

        if ((now - last) > SESSION_CHECK_INTERVAL) {
            rwlock.writeLock().lock();
            try {
                // check expired sessions
                logger.finest("Checking for expired sessions");
                Collection sessionsCol = sessions.values();
                for (Iterator iterator = sessionsCol.iterator(); iterator.hasNext();) {
                    SecureConversationSession sess = (SecureConversationSession)iterator.next();
                    if (sess.getExpiration() <= now) {
                        // delete the session
                        logger.info("Deleting secure conversation session because expired: " + sess.getIdentifier());
                        iterator.remove();
                    }
                }

                lastExpirationCheck = now;
            }
            finally {
                rwlock.writeLock().unlock();
            }
        }
    }

    private byte[] generateNewSecret(int length) {
        // return some random secret
        byte[] output = new byte[length];
        random.nextBytes(output);
        return output;
    }

    private String randomuuid() {
        // return some random encoded string
        byte[] output = new byte[20];
        random.nextBytes(output);
        return HexUtils.hexDump(output);
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

    private static final Logger logger = Logger.getLogger(SecureConversationContextManager.class.getName());
    private static final long DEFAULT_SESSION_DURATION = 1000*60*60*2; // 2 hrs
    private static final long SESSION_CHECK_INTERVAL = 1000*60*5; // check every 5 minutes
    private static final Random random = new SecureRandom();
    private long lastExpirationCheck = System.currentTimeMillis();
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock(false);
}

