package com.l7tech.server.secureconversation;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.common.security.xml.processor.SecurityContext;
import com.l7tech.common.security.xml.processor.SecurityContextFinder;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
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
     * Retrieve a session previously recorded.
     */
    public SecureConversationSession getSession(String identifier) {
        checkExpiriedSessions();
        SecureConversationSession output = null;
        Sync readlock = rwlock.readLock();
        try {
            readlock.acquire();
            output = (SecureConversationSession)sessions.get(identifier);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Read lock interrupted", e);
        } finally {
            readlock.release();
        }
        return output;
    }

    /**
     * For use by the token service. Records and remembers a session for the duration specified.
     */
    public void saveSession(SecureConversationSession newSession) throws DuplicateSessionException {
        Sync writelock = rwlock.writeLock();
        try {
            writelock.acquire();
            // Two sessions with same id is not allowed. ever (even if one is expired)
            SecureConversationSession alreadyExistingOne =
                    (SecureConversationSession)sessions.get(newSession.getIdentifier());
            if (alreadyExistingOne != null) {
                throw new DuplicateSessionException("Session already exists with id " + newSession.getIdentifier());
            }
            sessions.put(newSession.getIdentifier(), newSession);
            logger.finest("Saved SC context " + newSession.getIdentifier());
        } catch (InterruptedException e) {
            String msg = "Write lock interrupted. Session might not have been saved";
            logger.log(Level.WARNING, msg, e);
            throw new RuntimeException(e);
        } finally {
            writelock.release();
        }
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

    private void checkExpiriedSessions() {
        long now = System.currentTimeMillis();
        if ((now - lastExpirationCheck) > SESSION_CHECK_INTERVAL) {
            Sync writelock = rwlock.writeLock();
            try {
                writelock.acquire();
                // check expired sessions
                logger.finest("Checking for expired sessions");
                Collection sessionsCol = sessions.values();
                for (Iterator iterator = sessionsCol.iterator(); iterator.hasNext();) {
                    SecureConversationSession sess = (SecureConversationSession)iterator.next();
                    if (sess.getExpiration() <= now) {
                        // delete the session
                        logger.info("Deleting secure conversation session because expired: " + sess.getIdentifier());
                        sessions.remove(sess.getIdentifier());
                        // reset the traversor
                        sessionsCol = sessions.values();
                        iterator = sessionsCol.iterator();
                    }
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "this check was interrupted", e);
            } finally {
                writelock.release();
            }
            lastExpirationCheck = now;
        }
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
    public SecurityContext getSecurityContext(String securityContextIdentifier) {
        return getSession(securityContextIdentifier);
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

    private final Logger logger = Logger.getLogger(SecureConversationContextManager.class.getName());
    private static final long DEFAULT_SESSION_DURATION = 1000*60*60*2; // 2 hrs
    private static final long SESSION_CHECK_INTERVAL = 1000*60*5; // check every 5 minutes
    private static final Random random = new SecureRandom();
    private long lastExpirationCheck = System.currentTimeMillis();
    private final ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();
}

