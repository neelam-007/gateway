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
public interface MessageContextMappingManager {

    /**
     * Save a set of keys into the database.
     * @param mappingKeysEntity: the mapping keys entity to save.
     * @return the object id after the mapping key entity is successfully saved.
     * @throws SaveException
     * @throws FindException
     * @throws UpdateException
     */
    Goid saveMessageContextMappingKeys(MessageContextMappingKeys mappingKeysEntity) throws SaveException, FindException, UpdateException;

    /**
     * Save a set of values into the database.
     * @param mappingValuesEntity: the mapping values entity to save.
     * @return the object id after the mapping value entity is successfully saved.
     * @throws SaveException
     * @throws FindException
     */
    Goid saveMessageContextMappingValues(MessageContextMappingValues mappingValuesEntity) throws SaveException, FindException;
}
