package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 13, 2008
 */
public interface MessageContextMappingKeyManager extends GoidEntityManager<MessageContextMappingKeys, EntityHeader> {
    MessageContextMappingKeys getMessageContextMappingKeys(long oid) throws FindException;
    MessageContextMappingKeys getMessageContextMappingKeys(MessageContextMappingKeys keys) throws FindException;
}
