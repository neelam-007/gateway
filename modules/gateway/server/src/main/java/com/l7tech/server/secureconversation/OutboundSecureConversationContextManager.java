package com.l7tech.server.secureconversation;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.xml.SecureConversationKeyDeriver;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The outbound session map uses the concatenation of User ID and Service URL (rather than Session Identifier) as a lookup key.
 *
 * @author ghuang
 */
public class OutboundSecureConversationContextManager extends SecureConversationContextManager<OutboundSecureConversationContextManager.OutboundSessionKey> {

    //- PUBLIC

    public static final String LOG_SECRET_VALUES = "com.l7tech.security.wstrust.debug.logSecretValues";

    public OutboundSecureConversationContextManager( final Config config, final InboundSecureConversationContextManager inboundSessionManager ) {
        super(logger, config, false);
        this.inboundSessionManager = inboundSessionManager;
    }

    /**
     * The key for an outbound session.
     *
     * <p>An outbound session is for a particular user and service (or group of services).</p>
     */
    public static final class OutboundSessionKey implements Serializable {
        private final long providerId;
        private final String userId;
        private final String serviceUrl;

        /**
         * Create a new session key for the given user and "url".
         *
         * @param user The user for the session.
         * @param serviceUrl The URL for the session.
         */
        public OutboundSessionKey( final User user,
                                   final String serviceUrl ) {
            if ( user == null ) throw new IllegalArgumentException("user is required");
            if ( serviceUrl == null ) throw new IllegalArgumentException("service url is required");
            this.providerId = user.getProviderId();
            this.userId = user.getId();
            this.serviceUrl = serviceUrl;
        }

        /**
         * Recreate a session key from its string representation.
         *
         * @param stringIdentifier The session string.
         * @return The session key
         * @throws IllegalArgumentException if the given identifier is invalid
         * @see #toStringIdentifier()
         */
        public static OutboundSessionKey fromStringIdentifier( final String stringIdentifier ) {
            final ObjectInputStream in;
            try {
                in = new ObjectInputStream( new ByteArrayInputStream( HexUtils.decodeBase64( stringIdentifier )) );
                final Object read = in.readObject();
                if ( read instanceof OutboundSessionKey ) {
                    return (OutboundSessionKey) read;
                } else {
                    throw new IllegalArgumentException( "Invalid identifier" );
                }
            } catch ( IOException e ) {
                throw new IllegalArgumentException( "Invalid identifier", e );
            } catch ( ClassNotFoundException e ) {
                throw new IllegalArgumentException( "Invalid identifier", e );
            }
        }

        /**
         * Convert the session key to a string representation.
         *
         * @return This session key as a string
         * @see #fromStringIdentifier(String)
         */
        public String toStringIdentifier() {
            String identifier;

            PoolByteArrayOutputStream bos = new PoolByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream( new NonCloseableOutputStream(bos) );
                oos.writeObject( this );
                oos.close();
                identifier = HexUtils.encodeBase64(bos.toByteArray(), true);
            } catch ( IOException e ) {
                throw new IllegalStateException( e );
            } finally {
                bos.close();
            }

            return identifier;
        }

        /**
         * Is this session key valid?
         */
        boolean isValid() {
            return userId != null;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final OutboundSessionKey that = (OutboundSessionKey) o;

            if ( providerId != that.providerId ) return false;
            if ( !serviceUrl.equals( that.serviceUrl ) ) return false;
            if ( userId != null ? !userId.equals( that.userId ) : that.userId != null ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (providerId ^ (providerId >>> 32));
            result = 31 * result + (userId != null ? userId.hashCode() : 0);
            result = 31 * result + serviceUrl.hashCode();
            return result;
        }
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionOwner The user for the session (required)
     * @param serviceUrl The URL of the service that creates a SCT (must not be null)
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
                                                          final String serviceUrl,
                                                          final LoginCredentials credentials,
                                                          final String namespace,
                                                          final String sessionIdentifier,
                                                          final long creationTime,
                                                          final long expirationTime,
                                                          final byte[] requestSharedSecret,
                                                          final byte[] requestClientEntropy,
                                                          final byte[] requestServerEntropy,
                                                          final int keySizeBits,
                                                          final boolean copyToInboundCache ) throws SessionCreationException {
        final byte[] sharedSecret;
        final byte[] clientEntropy;
        final byte[] serverEntropy;

        if ( sessionOwner == null ) throw new SessionCreationException( "session owner is required" );
        if ( serviceUrl == null ) throw new SessionCreationException( "service url is required" );

        final OutboundSessionKey sessionKey = new OutboundSessionKey( sessionOwner, serviceUrl );

        // Check if there exists an outbound session with the same session key.
        // If found, then overwrite that session by canceling that session first and then creating a new session.
        // If not found, then just create a new session.
        final SecureConversationSession existingSession = getSession(sessionKey);
        if (existingSession != null) {
            cancelSession(sessionKey);
            logger.warning("Secure Conversation Session with the session identifier '" + existingSession.getIdentifier() + "' has been overwritten.");
        }

        if (requestSharedSecret != null) {
            if (requestSharedSecret.length >= MIN_SHARED_SECRET_BYTES && requestSharedSecret.length <= MAX_SHARED_SECRET_BYTES) {
                sharedSecret = requestSharedSecret;
                clientEntropy = null;
                serverEntropy = null;
            } else {
                throw new SessionCreationException("Unable to create a session: the shared secret length is not in the valid range from " +
                    MIN_SHARED_SECRET_BYTES + " bytes to " + MAX_SHARED_SECRET_BYTES + "bytes.");
            }
        } else if (requestClientEntropy != null) {
            if (requestClientEntropy.length >= MIN_CLIENT_ENTROPY_BYTES && requestClientEntropy.length <= MAX_CLIENT_ENTROPY_BYTES) {
                if (requestServerEntropy != null) {
                    if (requestServerEntropy.length >= MIN_SERVER_ENTROPY_BYTES && requestServerEntropy.length <= MAX_SERVER_ENTROPY_BYTES) {
                        final int keySize = keySizeBits == 0 ? getDefaultKeySize( namespace ) : (keySizeBits+7) / 8;
                        if ( keySize < MIN_KEY_SIZE || keySize > MAX_KEY_SIZE  ) {
                            throw new SessionCreationException("Unable to create a session: the key size is not in the valid range from " +
                                MIN_KEY_SIZE + " bytes to " + MAX_KEY_SIZE + "bytes.");
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
                            MIN_SERVER_ENTROPY_BYTES + " bytes to " + MAX_SERVER_ENTROPY_BYTES + "bytes.");
                    }
                } else {
                    sharedSecret = requestClientEntropy;
                    clientEntropy = null;
                    serverEntropy = null;
                }
            } else {
                throw new SessionCreationException("Unable to create a session: the client entropy length is not in the valid range from " +
                    MIN_CLIENT_ENTROPY_BYTES + " bytes to " + MAX_CLIENT_ENTROPY_BYTES + "bytes.");
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

        if (copyToInboundCache) {
            session.setCopiedIntoInboundCache(true);
            inboundSessionManager.saveSession(sessionIdentifier, session);
        }

        return session;
    }

    // - PROTECTED

    @Override
    protected void validateSessionKey( final OutboundSessionKey sessionKey ) throws SessionCreationException {
        if ( !sessionKey.isValid() ) {
            throw new SessionCreationException( "Unable to create session for user (not a persistent identity)" );
        }
    }

    @Override
    public boolean cancelSession(OutboundSessionKey sessionKey) {
        boolean cancelled = false;
        final SecureConversationSession session = getSession(sessionKey);

        if (session != null) {
            cancelled = super.cancelSession(sessionKey);
        }

        if (cancelled && session.isCopiedIntoInboundCache()) {
            final String inboundSessionId = session.getIdentifier();
            final SecureConversationSession inboundSession = inboundSessionManager.getSession(inboundSessionId);

            if (inboundSession != null) {
                cancelled = inboundSessionManager.cancelSession(session.getIdentifier());
            }
        }

        return cancelled;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( OutboundSecureConversationContextManager.class.getName() );

    /**
     * Combine information from a parsed RST and RSTR to extract the session identifier and session shared secret.
     *
     * @param externalId The external session identifier. Required.
     * @param clientEntropy The client entropy to use. Required.
     * @param serverEntropy The client entropy to use. Required.
     * @param keySizeBytes The size of the key to generate. Required.
     * @return the shared secret byte array.  Never null.
     * @throws java.security.InvalidKeyException may occur if current crypto policy disallows HMac with long keys
     * @throws java.security.NoSuchAlgorithmException if no HMacSHA1 service available from current security providers
     */
    private byte[] deriveSharedKey( final String externalId,
                                    final byte[] clientEntropy,
                                    final byte[] serverEntropy,
                                    final int keySizeBytes ) throws InvalidKeyException, NoSuchAlgorithmException {
        if (clientEntropy == null) throw new IllegalArgumentException("client entropy is required");
        if (serverEntropy == null) throw new IllegalArgumentException("server entropy is required");

        // Derive the shared secret
        byte[] secret = SecureConversationKeyDeriver.pSHA1(clientEntropy, serverEntropy, keySizeBytes);
        if ( logger.isLoggable(Level.FINEST) && SyspropUtil.getBoolean(LOG_SECRET_VALUES, false))
            logger.log(Level.FINEST, "Shared secret computed, length = {0}; value = {1}", new Object[] {secret.length, HexUtils.encodeBase64(secret)});


        if ( logger.isLoggable(Level.FINER) )
            logger.log(Level.FINER, "SC context created for {0} ==> key length bytes: {1}", new Object[] {externalId, secret.length});

        return secret;
    }

    // PRIVATE

    private final InboundSecureConversationContextManager inboundSessionManager;
}