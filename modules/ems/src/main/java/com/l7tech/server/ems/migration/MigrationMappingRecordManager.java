package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeaderRef;

/**
 *
 */
public interface MigrationMappingRecordManager extends EntityManager<MigrationMappingRecord, EntityHeader> {

    /**
     * Lookup a mapping record by source cluster/entity and destination cluster.
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param targetClusterId  The identifier for the target cluster.
     * @return The mapping or null
     * @throws FindException If an error occurs during find
     */
    MigrationMappingRecord findByMapping( String sourceClusterId, EntityHeaderRef sourceEntityHeader, String targetClusterId ) throws FindException;

    /**
     * Lookup an EntityHeader by source cluster/entity and destination cluster.
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param targetClusterId  The identifier for the target cluster.
     * @return The mapping or null
     * @throws FindException If an error occurs during find
     */
    EntityHeader findEntityHeaderForMapping( String sourceClusterId, EntityHeaderRef sourceEntityHeader, String targetClusterId ) throws FindException;

    /**
     * Save a mapping for the given source / destination.
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param destinationClusterId  The identifier for the destination cluster.
     * @param destinationEntityHeader The destination entity.
     * @return the mapping OID
     * @throws SaveException If an error occurs when persisting
     */
    long persistMapping( String sourceClusterId, EntityHeader sourceEntityHeader, String destinationClusterId, EntityHeader destinationEntityHeader ) throws SaveException;
}