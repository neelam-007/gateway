package com.l7tech.server.secureconversation;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Config;
import com.l7tech.util.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * The outbound session map uses the concatenation of User ID and Service URL (rather than Session Identifier) as a lookup key.
 *
 * @author ghuang
 */
public class OutboundSecureConversationContextManager extends SecureConversationContextManager<OutboundSecureConversationContextManager.OutboundSessionKey> {

    //- PUBLIC

    public OutboundSecureConversationContextManager( final Config config,
                                                     final StoredSecureConversationSessionManager storedSecureConversationSessionManager ) {
        super(logger, config, false);
        this.storedSecureConversationSessionManager = storedSecureConversationSessionManager;
    }

    /**
     * Create a session key for the given user and url.
     *
     * @param user The user for the session
     * @param serviceUrl The service for the session
     * @return The session key to use
     */
    public static OutboundSessionKey newSessionKey( final User user,
                                                    final String serviceUrl ) {
        return new OutboundSessionKey( user, serviceUrl );
    }

    /**
     * The key for an outbound session.
     *
     * <p>An outbound session is for a particular user and service (or group of services).</p>
     */
    public static final class OutboundSessionKey implements Serializable {
        private final Goid providerId;
        private final String userId;
        private final String serviceUrl;

        /**
         * Create a new session key for the given user and "url".
         *
         * @param user The user for the session.
         * @param serviceUrl The URL for the session.
         */
        private OutboundSessionKey( final User user,
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

            if ( providerId != null ? !providerId.equals( that.providerId ) : that.providerId != null ) return false;
            if ( !serviceUrl.equals( that.serviceUrl ) ) return false;
            if ( userId != null ? !userId.equals( that.userId ) : that.userId != null ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (providerId != null ? providerId.hashCode() : 0);
            result = 31 * result + (userId != null ? userId.hashCode() : 0);
            result = 31 * result + serviceUrl.hashCode();
            return result;
        }
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionOwner The user for the session (required)
     * @param sessionKey The key for the session (required)
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
    public SecureConversationSession createContextForUser( final User sessionOwner,
                                                           final OutboundSessionKey sessionKey,
                                                           final String namespace,
                                                           final String sessionIdentifier,
                                                           final long creationTime,
                                                           final long expirationTime,
                                                           final byte[] requestSharedSecret,
                                                           final byte[] requestClientEntropy,
                                                           final byte[] requestServerEntropy,
                                                           final int keySizeBits ) throws SessionCreationException {
        if ( sessionOwner == null ) throw new SessionCreationException( "session owner is required" );
        if ( sessionKey == null ) throw new SessionCreationException( "session key is required" );

        // Check if there exists an outbound session with the same session key.
        // If found, then overwrite that session by canceling that session first and then creating a new session.
        // If not found, then just create a new session.
        final SecureConversationSession existingSession = getSession(sessionKey);
        if (existingSession != null) {
            cancelSession(sessionKey);
            logger.warning("Secure Conversation Session with the session identifier '" + existingSession.getIdentifier() + "' has been overwritten.");
        }

        return super.createContextForUser( 
                sessionOwner,
                sessionKey,
                namespace,
                sessionIdentifier,
                creationTime,
                expirationTime,
                requestSharedSecret,
                requestClientEntropy,
                requestServerEntropy,
                keySizeBits,
                null );
    }


    // - PROTECTED

    @Override
    protected void validateSessionKey( final OutboundSessionKey sessionKey,
                                       final User user ) throws SessionCreationException {
        if ( !sessionKey.isValid() ) {
            throw new SessionCreationException( "Unable to create session for user (not a persistent identity)" );
        }

        if ( user != null &&
             ( !user.getProviderId().equals(sessionKey.providerId) || !sessionKey.userId.equals( user.getId() )) ) {
            throw new SessionCreationException( "Unable to create session for user (invalid user for session)" );
        }
    }

    @Override
    protected void storeSession( final OutboundSessionKey sessionKey, final SecureConversationSession session ) throws SaveException {
        storedSecureConversationSessionManager.save( toStored( sessionKey, session ) );
    }

    @Override
    protected SecureConversationSession loadSession( final OutboundSessionKey sessionKey ) throws FindException {
        final StoredSecureConversationSession session =
                storedSecureConversationSessionManager.findOutboundSessionByUserAndService(
                        sessionKey.providerId,
                        sessionKey.userId,
                        sessionKey.serviceUrl );
        return session==null ?
                null :
                fromStored(
                        session,
                        storedSecureConversationSessionManager.decryptSessionKey( session.getEncryptedKey() ) );
    }

    @Override
    protected void deleteSession( final OutboundSessionKey sessionKey ) throws DeleteException {
        storedSecureConversationSessionManager.deleteOutboundSessionByUserAndService(
                sessionKey.providerId,
                sessionKey.userId,
                sessionKey.serviceUrl );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( OutboundSecureConversationContextManager.class.getName() );

    private final StoredSecureConversationSessionManager storedSecureConversationSessionManager;

    private StoredSecureConversationSession toStored( final OutboundSessionKey sessionKey,
                                                      final SecureConversationSession session ) throws SaveException {
        try {
            return new StoredSecureConversationSession(
                    sessionKey.serviceUrl,
                    session.getUsedBy().getProviderId(),
                    session.getUsedBy().getId(),
                    session.getUsedBy().getLogin(),
                    session.getIdentifier(),
                    session.getCreation(),
                    session.getExpiration(),
                    session.getSCNamespace(),
                    session.getElement()!=null ? XmlUtil.nodeToString( session.getElement() ) : null,
                    storedSecureConversationSessionManager.encryptSessionKey( session.getSecretKey() ) );
        } catch ( IOException e ) {
            throw new SaveException( "Unable to serialize session for storage: " + ExceptionUtils.getMessage( e ), e );
        }
    }
}