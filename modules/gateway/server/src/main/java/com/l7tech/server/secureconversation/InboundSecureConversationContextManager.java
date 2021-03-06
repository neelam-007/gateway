package com.l7tech.server.secureconversation;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.security.xml.SecureConversationKeyDeriver;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class InboundSecureConversationContextManager extends SecureConversationContextManager<String> implements SecurityContextFinder {

    //- PUBLIC

    public InboundSecureConversationContextManager( final Config config,
                                                    final StoredSecureConversationSessionManager storedSecureConversationSessionManager ) {
        super(logger, config, true);
        this.storedSecureConversationSessionManager = storedSecureConversationSessionManager;
    }

    /**
     * For use by the WssProcessor on the ssg.
     */
    @Override
    public SecurityContext getSecurityContext( final String securityContextIdentifier ) {
        return getSession(securityContextIdentifier);
    }

     /**
     * Creates a new session and saves it
     * @param sessionOwner The user for the session (required)
     * @param namespace The WS-SecureConversation namespace in use (may be null)
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser( final User sessionOwner,
                                                           final String namespace) throws SessionCreationException {
        return createContextForUser(sessionOwner, namespace, getDefaultSessionDuration(), null, -1);
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionOwner The user for the session (required)
     * @param namespace The WS-SecureConversation namespace in use (may be null)
     * @param sessionDuration: its unit is milliseconds.  It must be greater than 0.
     * @param requestClientEntropy The request client entropy (may be null)
     * @param requestKeySize The request key size in bits (values of 0 or less ignored)
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser( final User sessionOwner,
                                                           final String namespace,
                                                           final long sessionDuration,
                                                           final byte[] requestClientEntropy,
                                                           final int requestKeySize ) throws SessionCreationException {
        // make up a new session identifier
        final String sessionIdentifier = "http://www.layer7tech.com/uuid/" + randomUuid();
        if (sessionDuration <= 0L) {
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
            null
        );
        saveSession(session.getIdentifier(), session);
        return session;
    }

    //- PROTECTED

    @Override
    protected void storeSession( final String sessionKey, final SecureConversationSession session ) throws SaveException {
        storedSecureConversationSessionManager.save( toStored( session ) );
    }

    @Override
    protected SecureConversationSession loadSession( final String sessionKey ) throws FindException {
        final StoredSecureConversationSession session =
                storedSecureConversationSessionManager.findInboundSessionByIdentifier( sessionKey );
        return session==null ?
                null :
                fromStored(
                        session,
                        storedSecureConversationSessionManager.decryptSessionKey( session.getEncryptedKey() ) );
    }

    @Override
    protected void deleteSession( final String sessionKey ) throws DeleteException {
        storedSecureConversationSessionManager.deleteInboundSessionByIdentifer( sessionKey );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( InboundSecureConversationContextManager.class.getName() );

    private final StoredSecureConversationSessionManager storedSecureConversationSessionManager;

    private StoredSecureConversationSession toStored( final SecureConversationSession session ) {
        return new StoredSecureConversationSession(
                session.getIdentifier(),
                session.getCreation(),
                session.getExpiration(),
                session.getSCNamespace(),
                storedSecureConversationSessionManager.encryptSessionKey( session.getSecretKey() ),
                session.getUsedBy().getProviderId(),
                session.getUsedBy().getId(),
                session.getUsedBy().getLogin() );
    }

    private int calculateKeySize( final int requestedKeySize,
                                  final String namespace ) {
        final int size;
        final int defaultSize = getDefaultKeySize( namespace );

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
}
