package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 13, 2008
 */
public interface MessageContextMappingValueManager extends EntityManager<MessageContextMappingValues, EntityHeader> {
    MessageContextMappingValues getMessageContextMappingValues(long oid) throws FindException;
    MessageContextMappingValues getMessageContextMappingValues(MessageContextMappingValues values) throws FindException;
}
