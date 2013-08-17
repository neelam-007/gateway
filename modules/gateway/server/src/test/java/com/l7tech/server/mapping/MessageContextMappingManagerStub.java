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
 * @Date: Sep 1, 2008
 */
public class MessageContextMappingManagerStub implements MessageContextMappingManager {
    public MessageContextMappingKeys getMessageContextMappingKeys(long oid) throws FindException {
        return null;
    }

    public MessageContextMappingValues getMessageContextMappingValues(long oid) throws FindException {
        return null;
    }

    public Goid saveMessageContextMappingKeys(MessageContextMappingKeys mappingKeysEntity) throws SaveException, FindException, UpdateException {
        return new Goid(0, 0);
    }

    public Goid saveMessageContextMappingValues(MessageContextMappingValues mappingValuesEntity) throws SaveException {
        return new Goid(0, 0);
    }
}
