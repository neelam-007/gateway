package com.l7tech.server.mapping;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */
public class MessageContextMappingManagerImpl implements MessageContextMappingManager {
    private MessageContextMappingKeyManager mappingKeyManager;
    private MessageContextMappingValueManager mappingValueManager;


    public  MessageContextMappingManagerImpl(MessageContextMappingKeyManager mappingKeyManager,
                                             MessageContextMappingValueManager mappingValueManager) {
        this.mappingKeyManager = mappingKeyManager;
        this.mappingValueManager = mappingValueManager;
    }

    public MessageContextMappingKeys getMessageContextMappingKeys(long oid) throws FindException {
        return mappingKeyManager.getMessageContextMappingKeys(oid);
    }

    public MessageContextMappingValues getMessageContextMappingValues(long oid) throws FindException {
        return mappingValueManager.getMessageContextMappingValues(oid);
    }

    public long saveMessageContextMappingKeys(MessageContextMappingKeys mappingKeysEntity) throws SaveException, FindException, UpdateException {
        String newGuid = mappingKeysEntity.generateGuid();
        MessageContextMappingKeys anEntity = mappingKeyManager.getMessageContextMappingKeys(newGuid);
        if (anEntity == null) {
            return mappingKeyManager.save(mappingKeysEntity);
        } else {
            anEntity.setCreateTime(mappingKeysEntity.getCreateTime());
            mappingKeyManager.update(anEntity);
            return anEntity.getOid();
        }
    }

    public long saveMessageContextMappingValues(MessageContextMappingValues mappingValuesEntity) throws SaveException {
        return mappingValueManager.save(mappingValuesEntity);
    }
}
