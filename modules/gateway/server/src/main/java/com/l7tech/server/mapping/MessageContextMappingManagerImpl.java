package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */
public class MessageContextMappingManagerImpl implements MessageContextMappingManager {

    //- PUBLIC

    public  MessageContextMappingManagerImpl( final MessageContextMappingKeyManager mappingKeyManager,
                                              final MessageContextMappingValueManager mappingValueManager) {
        this.mappingKeyManager = mappingKeyManager;
        this.mappingValueManager = mappingValueManager;
    }

    public Goid saveMessageContextMappingKeys( final MessageContextMappingKeys mappingKeysEntity ) throws SaveException, FindException, UpdateException {
        MessageContextMappingKeys anEntity = mappingKeyManager.getMessageContextMappingKeys(mappingKeysEntity);
        if (anEntity == null) {
            mappingKeysEntity.setCreateTime(System.currentTimeMillis());
            return mappingKeyManager.save(mappingKeysEntity);
        } else {
            return anEntity.getGoid();
        }
    }

    public Goid saveMessageContextMappingValues( final MessageContextMappingValues mappingValuesEntity ) throws SaveException, FindException {
        MessageContextMappingValues anEntity = mappingValueManager.getMessageContextMappingValues(mappingValuesEntity);
        if (anEntity == null) {
            mappingValuesEntity.setCreateTime(System.currentTimeMillis());
            return mappingValueManager.save(mappingValuesEntity);
        } else {
            return anEntity.getGoid();
        }
    }

    //- PRIVATE

    private final MessageContextMappingKeyManager mappingKeyManager;
    private final MessageContextMappingValueManager mappingValueManager;
    
}
