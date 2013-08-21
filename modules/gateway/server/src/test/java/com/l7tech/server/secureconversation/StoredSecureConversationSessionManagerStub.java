package com.l7tech.server.secureconversation;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;

/**
 * Stub for StoredSecureConversationSessionManager
 */
public class StoredSecureConversationSessionManagerStub extends EntityManagerStub<StoredSecureConversationSession,EntityHeader> implements StoredSecureConversationSessionManager {

    @Override
    public StoredSecureConversationSession findInboundSessionByIdentifier( final String identifier ) {
        return null;
    }

    @Override
    public StoredSecureConversationSession findOutboundSessionByUserAndService( final Goid providerId, final String userId, final String serviceUrl ) {
        return null;
    }

    @Override
    public void deleteInboundSessionByIdentifer( final String identifier ) {
    }

    @Override
    public void deleteOutboundSessionByUserAndService( final Goid providerId, final String userId, final String serviceUrl ) {
    }

    @Override
    public void deleteStale( final long expiryTime ) {
    }

    @Override
    public void deleteAll() {
    }

    @Override
    public String encryptSessionKey( final byte[] key ) {
        return "";
    }

    @Override
    public byte[] decryptSessionKey( final String encryptedKey ) throws FindException {
        throw new FindException("Not implemented");
    }
}
