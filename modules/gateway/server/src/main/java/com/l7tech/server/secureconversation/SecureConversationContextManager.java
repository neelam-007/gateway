package com.l7tech.server.secureconversation;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.xml.SecureConversationKeyDeriver;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ValidatedConfig;
import org.apache.commons.collections.map.LRUMap;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
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
        return output;
    }

    /**
     * Cancel the session for the given identifier.
     *
     * @param identifier The identifier for the session to cancel
     * @return True if cancelled, false if the session was not found.
     */
    public boolean cancelSession( final String identifier ) {
        final boolean cancelled;

        synchronized (sessions) {
            cancelled = sessions.remove(identifier) != null;
        }
        
        return cancelled;
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
     * @param sessionOwner The user for the session (required)
     * @param credentials The credentials used to authenticate (required)
     * @param namespace The WS-SecureConversation namespace in use (may be null)
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(User sessionOwner, LoginCredentials credentials, String namespace) throws SessionCreationException {
        return createContextForUser(sessionOwner, credentials, namespace, getDefaultSessionDuration(), null, -1);
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionOwner The user for the session (required)
     * @param credentials The credentials used to authenticate
     * @param namespace The WS-SecureConversation namespace in use (may be null)
     * @param sessionDuration: its unit is milliseconds.  It must be greater than 0.
     * @param requestClientEntropy The request client entropy (may be null)
     * @param requestKeySize The request key size in bits (values of 0 or less ignored)
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser( final User sessionOwner,
                                                           final LoginCredentials credentials,
                                                           final String namespace,
                                                           final long sessionDuration,
                                                           final byte[] requestClientEntropy,
                                                           final int requestKeySize ) throws SessionCreationException {
        // make up a new session identifier
        final String sessionIdentifier = "http://www.layer7tech.com/uuid/" + randomUuid();
        if (sessionDuration <= 0) {
            throw new SessionCreationException("Session duration must be greater than zero.");
        }
        // generate the session key and server entropy (if required)
        final int keySizeInBytes = calculateKeySize( requestKeySize, namespace );
        final byte[] clientEntropy;
        final byte[] serverEntropy;
        final byte[] sharedSecret;
        if ( requestClientEntropy != null && requestClientEntropy.length >= MIN_CLIENT_ENTROPY_BYTES && requestClientEntropy.length <= MAX_CLIENT_ENTROPY_BYTES ) {
            clientEntropy = requestClientEntropy;
            serverEntropy = generateNewSecret( keySizeInBytes );
            try {
                sharedSecret = SecureConversationKeyDeriver.pSHA1( clientEntropy, serverEntropy, keySizeInBytes );
            } catch ( NoSuchAlgorithmException e ) {
                throw new SessionCreationException( "Unable to generate session key: " + ExceptionUtils.getMessage( e ), e);
            } catch ( InvalidKeyException e ) {
                throw new SessionCreationException( "Unable to generate session key: " + ExceptionUtils.getMessage( e ), e);
            }
        } else {
            clientEntropy = null;
            serverEntropy = null;
            sharedSecret = generateNewSecret( keySizeInBytes );
        }

        final long time = System.currentTimeMillis();
        final SecureConversationSession session = new SecureConversationSession(
            namespace,
            sessionIdentifier,
            clientEntropy,
            serverEntropy,
            sharedSecret,
            time,
            time  + sessionDuration,
            sessionOwner,
            credentials
        );
        saveSession(session);
        return session;
    }

    private int calculateKeySize( final int requestedKeySize,
                                  final String namespace ) {
        final int size;

        // allow default size to be overridden per namespace (by index) if desired
        final int nsIndex = namespace==null? -1 : Arrays.asList( SoapConstants.WSSC_NAMESPACE_ARRAY ).indexOf( namespace );
        final int defaultSize = nsIndex < 0 ?
                SyspropUtil.getInteger( PROP_DEFAULT_KEY_SIZE, 32) :
                SyspropUtil.getInteger( PROP_DEFAULT_KEY_SIZE + "." + nsIndex, SyspropUtil.getInteger( PROP_DEFAULT_KEY_SIZE, 32) );

        if ( requestedKeySize > 0 ) {
            // convert to size in bytes rounding up
            final int requestedKeySizeBytes = requestedKeySize / 8 + ( requestedKeySize % 8 > 0 ? 1 : 0 );
            if ( requestedKeySizeBytes < MIN_KEY_SIZE ) {
                size = MIN_KEY_SIZE;
            } else if ( requestedKeySizeBytes > MAX_KEY_SIZE ) {
                size = MAX_KEY_SIZE;
            } else {
                size = requestedKeySizeBytes;
            }
        } else {
            size = defaultSize;
        }

        return size;
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
    private static final int MIN_CLIENT_ENTROPY_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.clientEntropyMinBytes", 8 );
    private static final int MAX_CLIENT_ENTROPY_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.clientEntropyMaxBytes", 1024 );
    private static final int MIN_KEY_SIZE = SyspropUtil.getInteger("com.l7tech.security.wssc.minLength", 16);
    private static final int MAX_KEY_SIZE = SyspropUtil.getInteger("com.l7tech.security.wssc.maxLength", 512);
    private static final String PROP_DEFAULT_KEY_SIZE = "com.l7tech.security.secureconversation.defaultSecretLengthInBytes";
    private static final Random random = new SecureRandom();

    /**
     * keys: identifier (string)
     * values: SecureConversationSession objects
     */
    private final LRUMap sessions;
    private final Config config;
    private long lastExpirationCheck = System.currentTimeMillis();
}