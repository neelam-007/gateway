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
     * @param valueMapping True for searching value mappings, false for searching entity mappings.
     * @return The mapping or null
     * @throws FindException If an error occurs during find
     */
    MigrationMappingRecord findByMapping( String sourceClusterId, ExternalEntityHeader sourceEntityHeader, String targetClusterId, boolean valueMapping ) throws FindException;

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
     * <p>This will do nothing if the source or destination cluster identifiers are invalid (will return -1 in this case)</p>
     *
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param destinationClusterId  The identifier for the destination cluster.
     * @param destinationEntityHeader The destination entity.
     * @param sameEntity True if this mapping is for an item with the same identity on source and destination (a copy)
     * @return the mapping OID or -1 if none was persisted
     * @throws SaveException If an error occurs when persisting
     */
    Goid persistMapping(String sourceClusterId, ExternalEntityHeader sourceEntityHeader, String destinationClusterId, ExternalEntityHeader destinationEntityHeader, boolean sameEntity) throws SaveException;

    /**
     * Save a value mapping for the given source / destination.
     *
     * <p>This will do nothing if the source or destination cluster identifiers are invalid (will return -1 in this case)</p>
     *
     *
     * @param sourceClusterId The identifier for the source cluster.
     * @param sourceEntityHeader The source entity.
     * @param destinationClusterId  The identifier for the destination cluster.
     * @param value The destination value.
     * @return the mapping OID or -1 if none was persisted
     * @throws SaveException If an error occurs when persisting
     */
    Goid persistMapping(String sourceClusterId, ExternalEntityHeader sourceEntityHeader, String destinationClusterId, String value) throws SaveException;
}