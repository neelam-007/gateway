package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Transactional(rollbackFor=Throwable.class)
public class MigrationMappingRecordManagerImpl extends HibernateEntityManager<MigrationMappingRecord, ExternalEntityHeader> implements MigrationMappingRecordManager {

    //- PUBLIC

    public MigrationMappingRecordManagerImpl( final SsgClusterManager ssgClusterManager ) {
        this.ssgClusterManager = ssgClusterManager;
    }

    @Override
    public MigrationMappingRecord findByMapping( final String sourceClusterGuid,
                                                 final ExternalEntityHeader sourceEntityHeader,
                                                 final String targetClusterGuid,
                                                 final boolean valueMapping) throws FindException {
        SsgCluster sourceCluster;
        SsgCluster targetCluster;
        try {
            sourceCluster = ssgClusterManager.findByGuid( sourceClusterGuid );
            targetCluster = ssgClusterManager.findByGuid( targetClusterGuid );
        } catch ( FindException fe ) {
            throw new FindException( "Error finding cluster by GUID when finding mapping.", fe );            
        }

        if ( sourceCluster == null ) {
            throw new FindException( "Could not find cluster for GUID '"+sourceClusterGuid+"'." );
        }
        if ( targetCluster == null ) {
            throw new FindException( "Could not find cluster for GUID '"+targetClusterGuid+"'." );
        }

        MigrationMappingRecord result;
        if (valueMapping) {
            result = findByMapping(sourceCluster, sourceEntityHeader, targetCluster, null);
            if (result == null) { // try reverse lookup
                result = MigrationMappingRecord.reverse(findByMapping(targetCluster, sourceEntityHeader, sourceCluster, null));
            }
        } else {
            result = findByMapping(sourceCluster, sourceEntityHeader, targetCluster, null, false);
            if (result == null) { // try reverse lookup
                result = MigrationMappingRecord.reverse(findByMapping( targetCluster, null, sourceCluster, sourceEntityHeader, true));
            }
        }
        return result;
    }

    @Override
    public ExternalEntityHeader findEntityHeaderForMapping( final String sourceClusterId,
                                                    final ExternalEntityHeader sourceEntityHeader,
                                                    final String targetClusterId ) throws FindException {
        ExternalEntityHeader entityHeader = null;

        MigrationMappingRecord record = findByMapping( sourceClusterId, sourceEntityHeader, targetClusterId, false );
        if ( record != null && record.getTarget().getEntityType() != null ) {
            entityHeader = MigrationMappedEntity.asEntityHeader(record.getTarget());
        }

        return entityHeader;
    }

    @Override
    public Goid persistMapping(final String sourceClusterGuid,
                               final ExternalEntityHeader sourceEntityHeader,
                               final String targetClusterGuid,
                               final String targetValue) throws SaveException {
        MigrationMappingRecord mapping = new MigrationMappingRecord();
        try {
            mapping.setSourceCluster( ssgClusterManager.findByGuid( sourceClusterGuid ) );
            mapping.setTargetCluster( ssgClusterManager.findByGuid( targetClusterGuid ) );
        } catch ( FindException fe ) {
            throw new SaveException( "Error finding cluster by GUID when saving mapping.", fe );
        }

        Goid goid = PersistentEntity.DEFAULT_GOID;
        if ( mapping.getSourceCluster() != null &&
             mapping.getTargetCluster() != null ) {
            try {
                MigrationMappingRecord existingMapping =
                        findByMapping( mapping.getSourceCluster(), sourceEntityHeader,
                                       mapping.getTargetCluster(), targetValue );
                if ( existingMapping == null ) {
                    mapping.setTimestamp( System.currentTimeMillis() );

                    MigrationMappedEntity sourceEntity = new MigrationMappedEntity();
                    // use mappingKey as id for value-mappings
                    sourceEntity.setExternalId(sourceEntityHeader.getMappingKey());
                    sourceEntity.setEntityType( sourceEntityHeader.getType() );
                    sourceEntity.setEntityId( sourceEntityHeader.getStrId() );
                    sourceEntity.setEntityName( sourceEntityHeader.getName() );
                    sourceEntity.setEntityDescription( sourceEntityHeader.getDescription() );
                    sourceEntity.setEntityVersion( sourceEntityHeader.getVersion() );

                    MigrationMappedEntity targetEntity = new MigrationMappedEntity();
                    targetEntity.setEntityValue( targetValue );

                    mapping.setSource( sourceEntity );
                    mapping.setTarget( targetEntity );

                    goid = super.save( mapping );
                } else {
                    goid = existingMapping.getGoid();
                }
            } catch ( FindException fe ) {
                throw new SaveException( "Error checking for existing mapping saving.", fe );
            }
        }

        return goid;
    }

    @Override
    public Goid persistMapping(final String sourceClusterGuid,
                               final ExternalEntityHeader sourceEntityHeader,
                               final String targetClusterGuid,
                               final ExternalEntityHeader targetEntityHeader,
                               final boolean sameEntity) throws SaveException {
        MigrationMappingRecord mapping = new MigrationMappingRecord();
        try {
            mapping.setSourceCluster( ssgClusterManager.findByGuid( sourceClusterGuid ) );
            mapping.setTargetCluster( ssgClusterManager.findByGuid( targetClusterGuid ) );
        } catch ( FindException fe ) {
            throw new SaveException( "Error finding cluster by GUID when saving mapping.", fe );            
        }

        Goid goid = PersistentEntity.DEFAULT_GOID;
        if ( mapping.getSourceCluster() != null &&
             mapping.getTargetCluster() != null ) {
            try {
                MigrationMappingRecord existingMapping =
                        findByMapping( mapping.getSourceCluster(), sourceEntityHeader,
                                       mapping.getTargetCluster(), targetEntityHeader, false );
                if ( existingMapping == null ) {
                    mapping.setTimestamp( System.currentTimeMillis() );

                    MigrationMappedEntity sourceEntity = new MigrationMappedEntity();
                    sourceEntity.setExternalId(sourceEntityHeader.getExternalId());
                    sourceEntity.setEntityType( sourceEntityHeader.getType() );
                    sourceEntity.setEntityId( sourceEntityHeader.getStrId() );
                    sourceEntity.setEntityName( sourceEntityHeader.getName() );
                    sourceEntity.setEntityDescription( sourceEntityHeader.getDescription() );
                    sourceEntity.setEntityVersion( sourceEntityHeader.getVersion() );

                    MigrationMappedEntity targetEntity = new MigrationMappedEntity();
                    targetEntity.setExternalId(targetEntityHeader.getExternalId());
                    targetEntity.setEntityType( targetEntityHeader.getType() );
                    targetEntity.setEntityId( targetEntityHeader.getStrId() );
                    targetEntity.setEntityName( targetEntityHeader.getName() );
                    targetEntity.setEntityDescription( targetEntityHeader.getDescription() );
                    targetEntity.setEntityVersion( targetEntityHeader.getVersion() );

                    mapping.setSource( sourceEntity );
                    mapping.setTarget( targetEntity );
                    mapping.setSameEntity(sameEntity);

                    goid = super.save( mapping );
                } else {
                    goid = existingMapping.getGoid();
                }
            } catch ( FindException fe ) {
                throw new SaveException( "Error checking for existing mapping saving.", fe );
            }
        }

        return goid;
    }

    @Override
    public Class<MigrationMappingRecord> getImpClass() {
        return MigrationMappingRecord.class;
    }

    @Override
    public Class<MigrationMappingRecord> getInterfaceClass() {
        return MigrationMappingRecord.class;
    }

    @Override
    public String getTableName() {
        return "migration_mapping";
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    //- PRIVATE

    private final SsgClusterManager ssgClusterManager;

    private MigrationMappingRecord findByMapping( final SsgCluster sourceCluster,
                                                  final ExternalEntityHeader sourceEntityHeader,
                                                  final SsgCluster targetCluster,
                                                  final ExternalEntityHeader targetEntityHeader, 
                                                  boolean reverse) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "sourceCluster", sourceCluster );
        map.put( "targetCluster", targetCluster );

        if ( sourceEntityHeader != null  ) {
            map.put("source.entityType", sourceEntityHeader.getType());
            map.put("source.externalId", sourceEntityHeader.getExternalId());
            if (reverse) // source version not included direct mapping lookups
                map.put("source.entityVersion", sourceEntityHeader.getVersion());
        }

        if ( targetEntityHeader != null  ) {
            map.put("target.entityType", targetEntityHeader.getType());
            map.put("target.externalId", targetEntityHeader.getExternalId());
            if (! reverse) // source version not included reverse mapping lookups
                map.put("target.entityVersion", targetEntityHeader.getVersion());
        }

        List<MigrationMappingRecord> result = findMatching(Arrays.asList(map));

        MigrationMappingRecord mostRecentMatch = null;
        for ( MigrationMappingRecord record : result ) {
            if (isValueMapping(record)) continue;
            if ( mostRecentMatch==null ) {
                mostRecentMatch = record;
            } else if ( mostRecentMatch.getTimestamp() < record.getTimestamp() ) {
                mostRecentMatch = record;
            }
        }

        return mostRecentMatch;
    }

    private MigrationMappingRecord findByMapping( final SsgCluster sourceCluster,
                                                  final ExternalEntityHeader sourceEntityHeader,
                                                  final SsgCluster targetCluster,
                                                  final String targetValue ) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "sourceCluster", sourceCluster );
        map.put( "targetCluster", targetCluster );

        if ( sourceEntityHeader != null  ) {
            map.put("source.entityType", sourceEntityHeader.getType());
            // use mappingKey as id for value-mappings
            map.put("source.externalId", sourceEntityHeader.getMappingKey());
        }

        if ( targetValue != null  ) {
            map.put("target.entityValue", targetValue);
        }

        List<MigrationMappingRecord> result = findMatching(Arrays.asList(map));

        MigrationMappingRecord mostRecentMatch = null;
        for ( MigrationMappingRecord record : result ) {
            if (!isValueMapping(record)) continue;
            if ( mostRecentMatch==null ) {
                mostRecentMatch = record;
            } else if ( mostRecentMatch.getTimestamp() < record.getTimestamp() ) {
                mostRecentMatch = record;
            }
        }

        return mostRecentMatch;
    }

    private static boolean isValueMapping(MigrationMappingRecord record) {
        return record.getTarget().getEntityValue() != null;
    }

}
