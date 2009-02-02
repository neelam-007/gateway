package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.*;

/**
 *
 */
public interface MigrationMappingRecordManager extends EntityManager<MigrationMappingRecord, ExternalEntityHeader> {

    /**
     * Lookup a mapping record by source cluster/entity and destination cluster.
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param targetClusterId  The identifier for the target cluster.
     * @return The mapping or null
     * @throws FindException If an error occurs during find
     */
    MigrationMappingRecord findByMapping( String sourceClusterId, ExternalEntityHeader sourceEntityHeader, String targetClusterId ) throws FindException;

    /**
     * Lookup an EntityHeader by source cluster/entity and destination cluster.
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param targetClusterId  The identifier for the target cluster.
     * @return The mapping or null
     * @throws FindException If an error occurs during find
     */
    ExternalEntityHeader findEntityHeaderForMapping( String sourceClusterId, ExternalEntityHeader sourceEntityHeader, String targetClusterId ) throws FindException;

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
    long persistMapping( String sourceClusterId, ExternalEntityHeader sourceEntityHeader, String destinationClusterId, ExternalEntityHeader destinationEntityHeader, boolean sameEntity ) throws SaveException;
}