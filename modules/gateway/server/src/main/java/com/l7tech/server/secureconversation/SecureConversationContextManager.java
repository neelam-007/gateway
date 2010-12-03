package com.l7tech.server.secureconversation;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.util.Config;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ValidatedConfig;
import org.apache.commons.collections.map.LRUMap;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Iterator;
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
 */
public class SecureConversationContextManager implements SecurityContextFinder {

    public SecureConversationContextManager( final Config config ) {
        this.config = validated(config);
        sessions = new LRUMap( config.getIntProperty( "wss.secureConversation.maxSessions", DEFAULT_MAX_SESSIONS )){
            @Override
            protected boolean removeLRU( final LinkEntry entry ) {
                final SecureConversationSession session = (SecureConversationSession) entry.getValue();
                logger.fine("Deleting least recently used SecureConversation context: " + session.getIdentifier());
                return true;
            }
        };
    }

    /**
     * For use by the WssProcessor on the ssg.
     */
    @Override
    public SecurityContext getSecurityContext(String securityContextIdentifier) {
        return getSession(securityContextIdentifier);
    }

    /**
     * Retrieve a session previously recorded.
     */
    public SecureConversationSession getSession(String identifier) {
        // Cleanup if necessary
        checkExpiredSessions();

        // Get session
        SecureConversationSession output;
        synchronized( sessions ) {
            output = (SecureConversationSession) sessions.get(identifier);
        }

        // Check if expired
        if ( output != null && output.getExpiration() <= System.currentTimeMillis() ) {
            output = null;

            synchronized( sessions ) {
                sessions.remove(identifier);
            }
        }
        // Comment: if output is null, it cannot tell if the session does not exist or is expired.
        return output;
    }

    public boolean isExpiredSession(String identifier) throws NoSuchSessionException {
        synchronized( sessions ) {
            // Get session
            SecureConversationSession session = (SecureConversationSession) sessions.get(identifier);

            // Check if it exits or not
            if (session == null) throw new NoSuchSessionException("The session (identifier = " + identifier + ") does not exist.");

            // Check if it is expired
            return session.getExpiration() <= System.currentTimeMillis();
        }
    }

    public void cancelSession(String identifier) throws NoSuchSessionException {
        // Check session first
        if (isExpiredSession(identifier)) {
            throw new NoSuchSessionException("The session (identifier = " + identifier + ") is expired.");
        }

        synchronized (sessions) {
            sessions.remove(identifier);
        }
    }

    /**
     * For use by the token service. Records and remembers a session for the duration specified.
     */
    public void saveSession(SecureConversationSession newSession) throws DuplicateSessionException {
        synchronized( sessions ) {
            // Two sessions with same id is not allowed. ever (even if one is expired)
            SecureConversationSession alreadyExistingOne = (SecureConversationSession) sessions.get(newSession.getIdentifier());
            if (alreadyExistingOne != null) {
                throw new DuplicateSessionException("Session already exists with id " + newSession.getIdentifier());
            }
            sessions.put(newSession.getIdentifier(), newSession);
            logger.fine("Saved SecureConversation context " + newSession.getIdentifier());
        }
    }

    /**
     * Creates a new session and saves it
     * @param sessionOwner
     * @param credentials
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(User sessionOwner, LoginCredentials credentials, String namespace) throws DuplicateSessionException {
        // make up a new session identifier and shared secret (using some random generator)
        String newSessionIdentifier = "http://www.layer7tech.com/uuid/" + randomUuid();
        return createContextForUser(newSessionIdentifier, sessionOwner, credentials, namespace, getDefaultSessionDuration());
    }

    /**
     * Creates a new session and saves it
     * @param sessionIdentifier: either a new session id or an identifier matching to a security context token
     * @param sessionOwner
     * @param credentials
     * @param sessionDuration: its unit is milliseconds.  It must be greater than 0.
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(String sessionIdentifier, User sessionOwner, LoginCredentials credentials, String namespace, long sessionDuration) throws DuplicateSessionException {
        if (sessionDuration <= 0) {
            throw new IllegalArgumentException("Session duration must be greater than zero.");
        }
        final byte[] sharedSecret;
        if (namespace != null && namespace.equals( SoapConstants.WSSC_NAMESPACE2)) {
            sharedSecret = generateNewSecret(32);
        } else {
            sharedSecret = generateNewSecret(SyspropUtil.getInteger("com.l7tech.security.secureconversation.defaultSecretLengthInBytes", 32));
        }

        final long time = System.currentTimeMillis();
        final SecureConversationSession session = new SecureConversationSession(
            namespace,
            sessionIdentifier,
            sharedSecret,
            time,
            time  + sessionDuration,
            sessionOwner,
            credentials
        );
        saveSession(session);
        return session;
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionId the session identifier, you should use your own "namespace"
     * @param expiryTime the expiry in milliseconds
     * @param sessionOwner the user
     * @param credentials the users credentials
     * @param sharedKey the key for the session
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(String sessionId, long expiryTime, User sessionOwner, LoginCredentials credentials, byte[] sharedKey) throws DuplicateSessionException {
        long expires = expiryTime > 0 ? expiryTime : System.currentTimeMillis() + getDefaultSessionDuration();
        SecureConversationSession session = new SecureConversationSession(
                null,
                sessionId,
                sharedKey,
                System.currentTimeMillis(),
                expires,
                sessionOwner,
                credentials
        );
        saveSession(session);
        return session;
    }

    public long getDefaultSessionDuration() {
        return config.getTimeUnitProperty( "wss.secureConversation.defaultSessionDuration", DEFAULT_SESSION_DURATION );
    }

    private void checkExpiredSessions() {
        final long now = System.currentTimeMillis();
        long last;
        synchronized( sessions ) {
            last = lastExpirationCheck;
        }

        if ((now - last) > SESSION_CHECK_INTERVAL) {
            synchronized( sessions ) {
                // check expired sessions
                logger.finest("Checking for expired sessions");
                final Collection sessionsCol = sessions.values();
                for (final Iterator iterator = sessionsCol.iterator(); iterator.hasNext();) {
                    final SecureConversationSession session = (SecureConversationSession)iterator.next();
                    if (session.getExpiration() <= now) {
                        // delete the session
                        logger.fine("Deleting expired SecureConversation context: " + session.getIdentifier());
                        iterator.remove();
                    }
                }

                lastExpirationCheck = now;
            }
        }
    }

    private byte[] generateNewSecret(int length) {
        // return some random secret
        final byte[] output = new byte[length];
        random.nextBytes(output);
        return output;
    }

    private String randomUuid() {
        // return some random encoded string
        final byte[] output = new byte[20];
        random.nextBytes(output);
        return HexUtils.hexDump(output);
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger );

        vc.setMinimumValue( "wss.secureConversation.defaultSessionDuration", MIN_SESSION_DURATION );
        vc.setMaximumValue( "wss.secureConversation.defaultSessionDuration", MAX_SESSION_DURATION );

        vc.setMinimumValue( "wss.secureConversation.maxSessions", MIN_SESSIONS );
        vc.setMaximumValue( "wss.secureConversation.maxSessions", MAX_SESSIONS );

        return vc;
    }

    private static final Logger logger = Logger.getLogger(SecureConversationContextManager.class.getName());
    private static final long MIN_SESSIONS = 1;
    private static final long MAX_SESSIONS = 1000000;
    private static final int DEFAULT_MAX_SESSIONS = 10000;
    private static final long MIN_SESSION_DURATION = 1000*60; // 1 min
    private static final long MAX_SESSION_DURATION = 1000*60*60*24; // 24 hrs
    private static final long DEFAULT_SESSION_DURATION = 1000*60*60*2; // 2 hrs
    private static final long SESSION_CHECK_INTERVAL = 1000*60*5; // check every 5 minutes
    private static final Random random = new SecureRandom();

    /**
     * keys: identifier (string)
     * values: SecureConversationSession objects
     */
    private final LRUMap sessions;
    private final Config config;
    private long lastExpirationCheck = System.currentTimeMillis();
}