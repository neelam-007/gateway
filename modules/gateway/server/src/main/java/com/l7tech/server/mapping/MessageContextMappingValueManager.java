package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 13, 2008
 */
public interface MessageContextMappingValueManager extends GoidEntityManager<MessageContextMappingValues, EntityHeader> {
    MessageContextMappingValues getMessageContextMappingValues(long oid) throws FindException;
    MessageContextMappingValues getMessageContextMappingValues(MessageContextMappingValues values) throws FindException;
}
