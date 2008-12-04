package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.SaveException;

/**
 *
 */
public interface MigrationMappingRecordManager extends EntityManager<MigrationMappingRecord, EntityHeader> {

    /**
     * Save a mapping for the given source / destination.
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param destinationClusterId  The identifier for the destination cluster.
     * @param destinationEntityHeader The destination entity.
     * @return the mapping OID
     */
    long saveMapping( String sourceClusterId, EntityHeader sourceEntityHeader, String destinationClusterId, EntityHeader destinationEntityHeader ) throws SaveException;
}