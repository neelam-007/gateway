package com.l7tech.server.security.kerberos;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.util.SyspropUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Server-side manager that manages Kerberos sessions.
 */
public class KerberosSessionContextManager implements SecurityContextFinder {

    //- PUBLIC

    /**
     * For use by the WssProcessor on the ssg.
     */
    @Override
    public SecurityContext getSecurityContext( final String identifier ) {
        return getSession( identifier );
    }

    /**
     * Retrieve an existing session.
     *
     * @return the session or null if not found / expired.
     */
    public KerberosSession getSession( final String identifier ) {
        // Cleanup if necessary
        checkExpiredSessions();

        // Get session
        KerberosSession output = null;
        lock.readLock().lock();
        try {
            output = sessions.get(identifier);
        } finally {
            lock.readLock().unlock();
        }

        // Check if expired
        if ( output != null && output.getExpiration() <= System.currentTimeMillis() ) {
            output = null;

            lock.writeLock().lock();
            try {
                sessions.remove(identifier);
            }
            finally {
                lock.writeLock().unlock();
            }
        }

        return output;
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionId the session identifier, you should use your own "namespace"
     * @param expiryTime the expiry in milliseconds
     * @param credentials the users credentials
     * @param sharedKey the key for the session
     * @return the newly created session
     */
    public KerberosSession createSession( final String sessionId,
                                          final long expiryTime,
                                          final LoginCredentials credentials,
                                          final byte[] sharedKey ) throws DuplicateSessionException {
        long expires = expiryTime > 0 ? expiryTime : System.currentTimeMillis() + DEFAULT_SESSION_DURATION;
        expires = Math.min( expires, MAXIMUM_SESSION_DURATION );
        KerberosSession session = new KerberosSession(
                sessionId,
                sharedKey,
                System.currentTimeMillis(),
                expires,
                credentials
        );
        saveSession(session);
        return session;
    }

    public static class DuplicateSessionException extends Exception {
        public DuplicateSessionException(String message) {
            super(message);
        }

        public DuplicateSessionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    //- PRIVATE

    private Map<String,KerberosSession> sessions = new HashMap<String,KerberosSession>();

    private static final Logger logger = Logger.getLogger( KerberosSessionContextManager.class.getName());
    private static final long DEFAULT_SESSION_DURATION = SyspropUtil.getLong("com.l7tech.server.security.kerberos.sessionDefaultDuration", 1000*60*60*2); // 2 hrs
    private static final long MAXIMUM_SESSION_DURATION = SyspropUtil.getLong("com.l7tech.server.security.kerberos.sessionMaximumDuration", 1000*60*60*10); // 10 hrs
    private static final long SESSION_CHECK_INTERVAL = 1000*60*5; // check every 5 minutes

    private long lastExpirationCheck = System.currentTimeMillis();
    private final ReadWriteLock lock = new ReentrantReadWriteLock(false);

    private void saveSession( final KerberosSession session ) throws DuplicateSessionException {
        lock.writeLock().lock();
        try {
            // Two sessions with same id is not allowed. ever (even if one is expired)
            KerberosSession alreadyExistingOne = sessions.get(session.getIdentifier());
            if (alreadyExistingOne != null) {
                throw new DuplicateSessionException("Session already exists with id " + session.getIdentifier());
            }
            sessions.put(session.getIdentifier(), session);
            logger.fine("Saved kerberos session context " + session.getIdentifier());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void checkExpiredSessions() {
        long now = System.currentTimeMillis();
        long last;
        lock.readLock().lock();
        try {
            last = lastExpirationCheck;
        } finally {
            lock.readLock().unlock();
        }

        if ((now - last) > SESSION_CHECK_INTERVAL) {
            lock.writeLock().lock();
            try {
                // check expired sessions
                logger.finest("Checking for expired sessions");
                Collection sessionsCol = sessions.values();
                for (Iterator iterator = sessionsCol.iterator(); iterator.hasNext();) {
                    KerberosSession session = (KerberosSession)iterator.next();
                    if (session.getExpiration() <= now) {
                        // delete the session
                        logger.fine("Deleting session due to expiry: " + session.getIdentifier());
                        iterator.remove();
                    }
                }

                lastExpirationCheck = now;
            }
            finally {
                lock.writeLock().unlock();
            }
        }
    }

}