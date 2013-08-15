package com.l7tech.server.secureconversation;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class StoredSecureConversationSessionManagerImpl extends HibernateEntityManager<StoredSecureConversationSession, EntityHeader> implements StoredSecureConversationSessionManager {

    //- PUBLIC

    @Override
    public StoredSecureConversationSession findInboundSessionByIdentifier( final String identifier ) throws FindException {
        return findBySessionKey(
                StoredSecureConversationSession.generateSessionKey( identifier ),
                inboundMatcher( identifier ));
    }

    @Override
    public StoredSecureConversationSession findOutboundSessionByUserAndService( final Goid providerId,
                                                                                final String userId,
                                                                                final String serviceUrl ) throws FindException {
        return findBySessionKey(
                StoredSecureConversationSession.generateSessionKey( providerId, userId, serviceUrl ),
                outboundMatcher( providerId, userId, serviceUrl ));
    }

    @Override
    public void deleteInboundSessionByIdentifer( final String identifier ) throws DeleteException {
        deleteIfMatches(
                StoredSecureConversationSession.generateSessionKey( identifier ),
                inboundMatcher( identifier ));
    }

    @Override
    public void deleteOutboundSessionByUserAndService( final Goid providerId, final String userId, final String serviceUrl ) throws DeleteException {
        deleteIfMatches(
                StoredSecureConversationSession.generateSessionKey( providerId, userId, serviceUrl ),
                outboundMatcher( providerId, userId, serviceUrl ));
    }

    @Override
    public void deleteStale( final long expiryTime ) throws DeleteException {
        getHibernateTemplate().bulkUpdate( "delete StoredSecureConversationSession s where s.expires < ?", expiryTime );
    }

    @Override
    public void deleteAll() throws DeleteException {
        getHibernateTemplate().bulkUpdate( "delete StoredSecureConversationSession" );
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    @Override
    public String encryptSessionKey( final byte[] key ) {
        return clusterEncryptionManager.encryptPassword( HexUtils.encodeBase64(key).toCharArray() );
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    @Override
    public byte[] decryptSessionKey( final String encryptedKey ) throws FindException {
        try {
            return HexUtils.decodeBase64( new String(clusterEncryptionManager.decryptPassword( encryptedKey )) );
        } catch ( ParseException e ) {
            throw new FindException( "Unable to decrypt session key", e );
        }
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    @Override
    public Class<StoredSecureConversationSession> getImpClass() {
        return StoredSecureConversationSession.class;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final StoredSecureConversationSession entity ) {
        return Collections.singleton( sessionKeyMap( entity.getSessionKey() ) );
    }

    //- PRIVATE

    @Inject
    @Named("dbPasswordEncryption")
    private MasterPasswordManager clusterEncryptionManager;

    private Map<String,Object> sessionKeyMap( final String sessionKey ) {
        return Collections.<String,Object>singletonMap( "sessionKey", sessionKey );
    }

    private StoredSecureConversationSession findBySessionKey( final String sessionKey,
                                                              final Functions.Unary<Boolean,StoredSecureConversationSession> matcher ) throws FindException {
        StoredSecureConversationSession session = findUnique( sessionKeyMap(sessionKey) );

        if ( !matcher.call( session ) ) {
            session = null; // this is not the session we are looking for
        }

        return session;
    }

    private void deleteIfMatches( final String sessionKey,
                                  final Functions.Unary<Boolean,StoredSecureConversationSession> matcher ) throws DeleteException {
        try {
            final StoredSecureConversationSession session = findBySessionKey( sessionKey, matcher );
            if ( session != null ) {
                delete( session );
            }
        } catch ( FindException e ) {
            throw new DeleteException( "Error finding session for deletion", e );
        }
    }

    private Functions.Unary<Boolean,StoredSecureConversationSession> inboundMatcher( final String identifier ) {
        return new Functions.Unary<Boolean, StoredSecureConversationSession>() {
            @Override
            public Boolean call( final StoredSecureConversationSession session ) {
                return session != null &&
                        session.isInbound() &&
                        session.getIdentifier().equals( identifier );
            }
        };
    }

    private Functions.Unary<Boolean,StoredSecureConversationSession> outboundMatcher( final Goid providerId,
                                                                                      final String userId,
                                                                                      final String serviceUrl ) {
        return new Functions.Unary<Boolean, StoredSecureConversationSession>() {
            @Override
            public Boolean call( final StoredSecureConversationSession session ) {
                return session != null &&
                        !session.isInbound() &&
                        session.getProviderId().equals(providerId) &&
                        session.getUserId().equals( userId ) &&
                        session.getServiceUrl().equals( serviceUrl );
            }
        };
    }
}
