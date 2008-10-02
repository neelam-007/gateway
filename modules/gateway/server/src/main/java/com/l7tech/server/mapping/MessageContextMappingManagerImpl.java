package com.l7tech.server.mapping;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.server.identity.IdentityProviderFactory;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */
public class MessageContextMappingManagerImpl implements MessageContextMappingManager {

    //- PUBLIC

    public  MessageContextMappingManagerImpl( final MessageContextMappingKeyManager mappingKeyManager,
                                              final MessageContextMappingValueManager mappingValueManager,
                                              final IdentityProviderFactory identityProviderFactory ) {
        this.mappingKeyManager = mappingKeyManager;
        this.mappingValueManager = mappingValueManager;
        this.identityProviderFactory = identityProviderFactory;
    }

    public MessageContextMappingKeys getMessageContextMappingKeys( final long oid ) throws FindException {
        return mappingKeyManager.getMessageContextMappingKeys(oid);
    }

    public MessageContextMappingValues getMessageContextMappingValues( final long oid ) throws FindException {
        return mappingValueManager.getMessageContextMappingValues(oid);
    }

    public long saveMessageContextMappingKeys( final MessageContextMappingKeys mappingKeysEntity ) throws SaveException, FindException, UpdateException {
        MessageContextMappingKeys anEntity = mappingKeyManager.getMessageContextMappingKeys(mappingKeysEntity);
        if (anEntity == null) {
            mappingKeysEntity.setCreateTime(System.currentTimeMillis());
            return mappingKeyManager.save(mappingKeysEntity);
        } else {
            return anEntity.getOid();
        }
    }

    public long saveMessageContextMappingValues( final MessageContextMappingValues mappingValuesEntity ) throws SaveException, FindException {
        MessageContextMappingValues anEntity = mappingValueManager.getMessageContextMappingValues(mappingValuesEntity);
        if (anEntity == null) {
            mappingValuesEntity.setCreateTime(System.currentTimeMillis());
            if ( mappingValuesEntity.getAuthUserId() != null &&
                 mappingValuesEntity.getAuthUserProviderId() != null &&
                 mappingValuesEntity.getAuthUserDescription() == null ) {
                mappingValuesEntity.setAuthUserDescription( describe( mappingValuesEntity.getAuthUserProviderId(), mappingValuesEntity.getAuthUserId() ) );
            }
            return mappingValueManager.save(mappingValuesEntity);
        } else {
            return anEntity.getOid();
        }
    }

    //- PRIVATE

    private final MessageContextMappingKeyManager mappingKeyManager;
    private final MessageContextMappingValueManager mappingValueManager;
    private final IdentityProviderFactory identityProviderFactory;

    private String describe( final Long providerOid, final String userId ) {
        String description;

        try {
            IdentityProvider provider = identityProviderFactory.getProvider( providerOid );
            User user = provider.getUserManager().findByPrimaryKey( userId );
            description = getUserDesription(user) + " [" + provider.getConfig().getName() + "]";
        } catch ( FindException fe ) {
            description = userId + " [#" + providerOid + "]";
        }

        return description;
    }

    private String getUserDesription( final User user ) {
        String userName = user.getLogin();
        if (userName == null || "".equals(userName)) userName = user.getName();
        if (userName == null || "".equals(userName)) userName = user.getId();
        return userName;
    }
}
