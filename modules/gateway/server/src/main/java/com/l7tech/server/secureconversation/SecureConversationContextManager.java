package com.l7tech.server.secureconversation;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.xml.SecureConversationKeyDeriver;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side manager that manages the inbound/outbound Secure Conversation sessions.
 */
public abstract class SecureConversationContextManager<KT> {

    public static final String LOG_SECRET_VALUES = "com.l7tech.security.wstrust.debug.logSecretValues";

    public SecureConversationContextManager( final Logger logger,
                                             final Config config,
                                             final boolean isInbound ) {
        this.isInbound = isInbound;
        this.logger = logger;
        this.config = validated(config);

        String maxSessionsPropertyName = isInbound? "wss.secureConversation.maxSessions" : "outbound.secureConversation.maxSessions";
        sessions = new LRUMap( config.getIntProperty( maxSessionsPropertyName, DEFAULT_MAX_SESSIONS )){
            @Override
            protected boolean removeLRU( final LinkEntry entry ) {
                final SecureConversationSession session = (SecureConversationSession) entry.getValue();
                logger.fine("Deleting least recently used SecureConversation context: " + session.getIdentifier());
                return true;
            }
        };
    }

    /**
     * Retrieve a session previously recorded.
     *
     * @param sessionKey the key of a session to be retrieved
     * @return a secure conversation session that matches the given key or null
     */
    public final SecureConversationSession getSession( final KT sessionKey ) {
        // Cleanup if necessary
        checkExpiredSessions();

        // Get session
        SecureConversationSession output;
        synchronized( sessions ) {
            output = (SecureConversationSession) sessions.get(sessionKey);
        }

        // Check if expired
        if ( output != null && output.getExpiration() <= System.currentTimeMillis() ) {
            output = null;

            synchronized( sessions ) {
                sessions.remove(sessionKey);
            }
        }
        return output;
    }

    /**
     * Cancel the session for the given key.
     *
     * @param sessionKey The key for the session to cancel
     * @return True if cancelled, false if the session was not found.
     */
    public final boolean cancelSession( final KT sessionKey ) {
        final boolean cancelled;

        synchronized (sessions) {
            cancelled = sessions.remove(sessionKey) != null;
        }

        return cancelled;
    }

    /**
     *  Records and remembers an inbound/outbound session for the duration specified.
     *
     * @param sessionKey: For Inbound Session, it is a session identifier.  For Outbound Session, it is User ID + Service URL.
     * @param newSession: The session is to be saved.
     * @throws DuplicateSessionException thrown when attempting to save an existing session.
     */
    protected void saveSession( final KT sessionKey ,
                                final SecureConversationSession newSession ) throws SessionCreationException {
        validateSessionKey( sessionKey, null );
        synchronized( sessions ) {
            // Two sessions with same id is not allowed. ever (even if one is expired)
            SecureConversationSession alreadyExistingOne = (SecureConversationSession) sessions.get(sessionKey);
            if (alreadyExistingOne != null) {
                throw new DuplicateSessionException("Session already exists with id " + newSession.getIdentifier());
            }
            sessions.put(sessionKey, newSession);
            logger.fine("Saved SecureConversation context " + newSession.getIdentifier());
        }
    }

    /**
     * Override if the session key needs validation.
     *
     * @param sessionKey The key to validate
     */
    protected void validateSessionKey( KT sessionKey, User user ) throws SessionCreationException {
    }

    /**
     * Creates a new inbound/outbound session and saves it.
     *
     * @param sessionKey the key for the new session
     * @param sessionId the external ("public") session identifier, you should use your own "namespace"
     * @param expiryTime the expiry in milliseconds
     * @param sessionOwner the user
     * @param credentials the users credentials
     * @param sharedKey the key for the session
     * @return the newly created session
     * @throws DuplicateSessionException thrown when attempting to create a new session, which exists already.
     */
    public SecureConversationSession createContextForUser(KT sessionKey, String sessionId, long expiryTime, User sessionOwner, LoginCredentials credentials, byte[] sharedKey) throws SessionCreationException {
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
        saveSession(sessionKey, session);
        return session;
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionOwner The user for the session (required)
     * @param sessionKey The key for the session (must not be null)
     * @param credentials The credentials used to authenticate
     * @param namespace The WS-SecureConversation namespace in use (may be null)
     * @param sessionIdentifier The external session identifier
     * @param creationTime: The time of the session created.  Its unit is milliseconds.  It must be greater than 0.
     * @param expirationTime: The time of the session expired.  Its unit is milliseconds.  It must be greater than 0.
     * @param requestSharedSecret: The full key (may be null)
     * @param requestClientEntropy The client entropy (may be null)
     * @param requestServerEntropy The server entropy (may be null)
     * @param keySizeBits The key size in bits (0 for not specified)
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(final User sessionOwner,
                                                          final KT sessionKey,
                                                          final LoginCredentials credentials,
                                                          final String namespace,
                                                          final String sessionIdentifier,
                                                          final long creationTime,
                                                          final long expirationTime,
                                                          final byte[] requestSharedSecret,
                                                          final byte[] requestClientEntropy,
                                                          final byte[] requestServerEntropy,
                                                          final int keySizeBits ) throws SessionCreationException {
        final byte[] sharedSecret;
        final byte[] clientEntropy;
        final byte[] serverEntropy;

        if ( sessionOwner == null ) throw new SessionCreationException( "session owner is required" );
        if ( sessionKey == null ) throw new SessionCreationException( "session key is required" );
        validateSessionKey( sessionKey, sessionOwner );

        if (requestSharedSecret != null) {
            if (requestSharedSecret.length >= MIN_SHARED_SECRET_BYTES && requestSharedSecret.length <= MAX_SHARED_SECRET_BYTES) {
                sharedSecret = requestSharedSecret;
                clientEntropy = null;
                serverEntropy = null;
            } else {
                throw new SessionCreationException("Unable to create a session: the shared secret length is not in the valid range from " +
                    MIN_SHARED_SECRET_BYTES + " bytes to " + MAX_SHARED_SECRET_BYTES + " bytes.");
            }
        } else if (requestClientEntropy != null) {
            if (requestClientEntropy.length >= MIN_CLIENT_ENTROPY_BYTES && requestClientEntropy.length <= MAX_CLIENT_ENTROPY_BYTES) {
                if (requestServerEntropy != null) {
                    if (requestServerEntropy.length >= MIN_SERVER_ENTROPY_BYTES && requestServerEntropy.length <= MAX_SERVER_ENTROPY_BYTES) {
                        final int keySize = keySizeBits == 0 ? getDefaultKeySize( namespace ) : (keySizeBits+7) / 8;
                        if ( keySize < MIN_KEY_SIZE || keySize > MAX_KEY_SIZE  ) {
                            throw new SessionCreationException("Unable to create a session: the key size is not in the valid range from " +
                                MIN_KEY_SIZE + " bytes to " + MAX_KEY_SIZE + " bytes.");
                        }
                        try {
                            sharedSecret = deriveSharedKey( sessionIdentifier, requestClientEntropy, requestServerEntropy, keySize );
                        } catch ( InvalidKeyException e ) {
                            throw new SessionCreationException( "Error creating shared key: " + ExceptionUtils.getMessage(e), e );
                        } catch ( NoSuchAlgorithmException e ) {
                            throw new SessionCreationException( "Error creating shared key: " + ExceptionUtils.getMessage(e), e );
                        }
                        clientEntropy = requestClientEntropy;
                        serverEntropy = requestServerEntropy;
                    } else {
                        throw new SessionCreationException("Unable to create a session: the server entropy length is not in the valid range from " +
                            MIN_SERVER_ENTROPY_BYTES + " bytes to " + MAX_SERVER_ENTROPY_BYTES + " bytes.");
                    }
                } else {
                    sharedSecret = requestClientEntropy;
                    clientEntropy = null;
                    serverEntropy = null;
                }
            } else {
                throw new SessionCreationException("Unable to create a session: the client entropy length is not in the valid range from " +
                    MIN_CLIENT_ENTROPY_BYTES + " bytes to " + MAX_CLIENT_ENTROPY_BYTES + " bytes.");
            }
        } else {
            throw new SessionCreationException("Unable to create a session: there are no shared secret and client entropy to create a session key");
        }

        final SecureConversationSession session = new SecureConversationSession(
            namespace,
            sessionIdentifier,
            clientEntropy,
            serverEntropy,
            sharedSecret,
            creationTime,
            expirationTime,
            sessionOwner,
            credentials
        );

        saveSession(sessionKey, session);

        return session;
    }

    public final long getDefaultSessionDuration() {
        String defaultSessionDurationPropName = isInbound? "wss.secureConversation.defaultSessionDuration" : "outbound.secureConversation.defaultSessionDuration";
        return config.getTimeUnitProperty( defaultSessionDurationPropName, DEFAULT_SESSION_DURATION );
    }

    protected final void checkExpiredSessions() {
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

    protected final byte[] generateNewSecret(int length) {
        // return some random secret
        final byte[] output = new byte[length];
        random.nextBytes(output);
        return output;
    }

    protected final String randomUuid() {
        // return some random encoded string
        final byte[] output = new byte[20];
        random.nextBytes(output);
        return HexUtils.hexDump(output);
    }

    protected final int getDefaultKeySize( final String namespace ) {
        // allow default size to be overridden per namespace (by index) if desired
        final int nsIndex = namespace==null? -1 : Arrays.asList( SoapConstants.WSSC_NAMESPACE_ARRAY ).indexOf( namespace );
        return nsIndex < 0 ?
                SyspropUtil.getInteger( PROP_DEFAULT_KEY_SIZE, 32) :
                SyspropUtil.getInteger( PROP_DEFAULT_KEY_SIZE + "." + nsIndex, SyspropUtil.getInteger( PROP_DEFAULT_KEY_SIZE, 32) );
    }


    private Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger );

        String defaultSessionDurationPropName = isInbound? "wss.secureConversation.defaultSessionDuration" : "outbound.secureConversation.defaultSessionDuration";
        vc.setMinimumValue( defaultSessionDurationPropName, MIN_SESSION_DURATION );
        vc.setMaximumValue( defaultSessionDurationPropName, MAX_SESSION_DURATION );

        String maxSessionsPropName = isInbound? "wss.secureConversation.maxSessions" : "outbound.secureConversation.maxSessions";
        vc.setMinimumValue( maxSessionsPropName, MIN_SESSIONS );
        vc.setMaximumValue( maxSessionsPropName, MAX_SESSIONS );

        return vc;
    }

    private static final Random random = new SecureRandom();
    private static final long MIN_SESSIONS = 1;
    private static final long MAX_SESSIONS = 1000000;
    private static final int DEFAULT_MAX_SESSIONS = 10000;
    private static final long MIN_SESSION_DURATION = 1000*60; // 1 min
    private static final long MAX_SESSION_DURATION = 1000*60*60*24; // 24 hrs
    private static final long DEFAULT_SESSION_DURATION = 1000*60*60*2; // 2 hrs
    private static final long SESSION_CHECK_INTERVAL = 1000*60*5; // check every 5 minutes

    protected static final int MIN_CLIENT_ENTROPY_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.clientEntropyMinBytes", 8 );
    protected static final int MAX_CLIENT_ENTROPY_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.clientEntropyMaxBytes", 1024 );
    protected static final int MIN_SERVER_ENTROPY_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.serverEntropyMinBytes", 8 );
    protected static final int MAX_SERVER_ENTROPY_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.serverEntropyMaxBytes", 1024 );
    protected static final int MIN_SHARED_SECRET_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.sharedSecretMinBytes", 16 );
    protected static final int MAX_SHARED_SECRET_BYTES = SyspropUtil.getInteger( "com.l7tech.server.secureconversation.sharedSecretMaxBytes", 1024 );
    protected static final int MIN_KEY_SIZE = SyspropUtil.getInteger("com.l7tech.security.wssc.minLength", 16);
    protected static final int MAX_KEY_SIZE = SyspropUtil.getInteger("com.l7tech.security.wssc.maxLength", 512);
    protected static final String PROP_DEFAULT_KEY_SIZE = "com.l7tech.security.secureconversation.defaultSecretLengthInBytes";

    private final Logger logger;
    /**
     * keys: identifier (string)
     * values: SecureConversationSession objects
     */
    protected final LRUMap sessions;
    private final Config config;
    private long lastExpirationCheck = System.currentTimeMillis();
    private boolean isInbound;

    /**
     * Combine information from a parsed RST and RSTR to extract the session identifier and session shared secret.
     *
     * @param externalId The external session identifier. Required.
     * @param clientEntropy The client entropy to use. Required.
     * @param serverEntropy The client entropy to use. Required.
     * @param keySizeBytes The size of the key to generate. Required.
     * @return the shared secret byte array.  Never null.
     * @throws InvalidKeyException may occur if current crypto policy disallows HMac with long keys
     * @throws NoSuchAlgorithmException if no HMacSHA1 service available from current security providers
     */
    private byte[] deriveSharedKey( final String externalId,
                                    final byte[] clientEntropy,
                                    final byte[] serverEntropy,
                                    final int keySizeBytes ) throws InvalidKeyException, NoSuchAlgorithmException {
        if (clientEntropy == null) throw new IllegalArgumentException("client entropy is required");
        if (serverEntropy == null) throw new IllegalArgumentException("server entropy is required");

        // Derive the shared secret
        byte[] secret = SecureConversationKeyDeriver.pSHA1(clientEntropy, serverEntropy, keySizeBytes);
        if ( logger.isLoggable( Level.FINEST) && SyspropUtil.getBoolean( LOG_SECRET_VALUES, false))
            logger.log(Level.FINEST, "Shared secret computed, length = {0}; value = {1}", new Object[] {secret.length, HexUtils.encodeBase64(secret)});


        if ( logger.isLoggable(Level.FINER) )
            logger.log(Level.FINER, "SC context created for {0} ==> key length bytes: {1}", new Object[] {externalId, secret.length});

        return secret;
    }
}