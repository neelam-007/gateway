package com.l7tech.server.secureconversation;

import com.l7tech.objectmodel.*;

/**
 * Entity manager for stored WS-SecureConversation sessions.
 */
public interface StoredSecureConversationSessionManager extends GoidEntityManager<StoredSecureConversationSession, EntityHeader> {

    /**
     * Find an inbound session by identifier.
     *
     * @param identifier The session identifier.
     * @return The session or null.
     * @throws FindException If an error occurs
     */
    StoredSecureConversationSession findInboundSessionByIdentifier( String identifier ) throws FindException;

    /**
     * Find an outbound session by user and service.
     *
     * @param providerId The provider identifier for the session user.
     * @param userId The user identifier for the session user.
     * @param serviceUrl The service URL for the session.
     * @return The session or null.
     * @throws FindException If an error occurs
     */
    StoredSecureConversationSession findOutboundSessionByUserAndService( Goid providerId,
                                                                         String userId,
                                                                         String serviceUrl ) throws FindException;

    /**
     * Delete an inbound session by identifier.
     *
     * @param identifier The session identifier.
     * @throws DeleteException If an error occurs
     */
    void deleteInboundSessionByIdentifer( String identifier ) throws DeleteException;

    /**
     * Delete an outbound session by user and service.
     *
     * @param providerId The provider identifier for the session user.
     * @param userId The user identifier for the session user.
     * @param serviceUrl The service URL for the session.
     * @throws DeleteException If an error occurs
     */
    void deleteOutboundSessionByUserAndService( Goid providerId,
                                                String userId,
                                                String serviceUrl ) throws DeleteException;

    /**
     * Deletes stale sessions.
     *
     * @throws DeleteException If an error occurs
     */
    void deleteStale( long expiryTime ) throws DeleteException;

    /**
     * Delete all stored sessions.
     *
     * @throws DeleteException If an error occurs
     */
    void deleteAll() throws DeleteException;

    /**
     * Encrypt the given session key.
     *
     * @param key The session key to encrypt
     * @return The encrypted session key (never null)
     */
    String encryptSessionKey( byte[] key );

    /**
     * Decrypt the given encrypted session key.
     *
     * @param encryptedKey The key to encrypt.
     * @return The session key (never null)
     * @throws FindException If an error occurs
     */
    byte[] decryptSessionKey( String encryptedKey ) throws FindException;
}
