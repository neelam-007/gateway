package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */
public interface MessageContextMappingManager {

    /**
     * Retrieve the keys of the current message context mapping from the database.
     * @return the keys of the current message context mapping.
     * @throws FindException
     */
    MessageContextMappingKeys getMessageContextMappingKeys(long oid) throws FindException;
    MessageContextMappingValues getMessageContextMappingValues(long oid) throws FindException;

    long saveMessageContextMappingKeys(MessageContextMappingKeys mappingKeysEntity) throws SaveException, FindException, UpdateException;
    long saveMessageContextMappingValues(MessageContextMappingValues mappingValuesEntity) throws SaveException;
}
