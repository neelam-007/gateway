package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

/**
 *
 */
@Transactional(rollbackFor=Throwable.class)
public class MigrationMappingRecordManagerImpl extends HibernateEntityManager<MigrationMappingRecord, EntityHeader> implements MigrationMappingRecordManager {

    //- PUBLIC

    public MigrationMappingRecordManagerImpl( final SsgClusterManager ssgClusterManager ) {
        this.ssgClusterManager = ssgClusterManager;
    }

    @Override
    public MigrationMappingRecord findByMapping( final String sourceClusterGuid,
                                                 final EntityHeaderRef sourceEntityHeader,
                                                 final String targetClusterGuid ) throws FindException {
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

        return findByMapping( sourceCluster, sourceEntityHeader, targetCluster, null );
    }

    @Override
    public EntityHeader findEntityHeaderForMapping( final String sourceClusterId,
                                                    final EntityHeaderRef sourceEntityHeader,
                                                    final String targetClusterId ) throws FindException {
        EntityHeader entityHeader = null;

        MigrationMappingRecord record = findByMapping( sourceClusterId, sourceEntityHeader, targetClusterId );
        if ( record != null ) {
            entityHeader = asEntityHeader( record.getTarget() );            
        }

        return entityHeader;
    }

    @Override
    public long persistMapping( final String sourceClusterGuid,
                                final EntityHeader sourceEntityHeader,
                                final String targetClusterGuid,
                                final EntityHeader targetEntityHeader ) throws SaveException {
        MigrationMappingRecord mapping = new MigrationMappingRecord();
        try {
            mapping.setSourceCluster( ssgClusterManager.findByGuid( sourceClusterGuid ) );
            mapping.setTargetCluster( ssgClusterManager.findByGuid( targetClusterGuid ) );
        } catch ( FindException fe ) {
            throw new SaveException( "Error finding cluster by GUID when saving mapping.", fe );            
        }

        long oid;
        try {
            MigrationMappingRecord exisingMapping =
                    findByMapping( mapping.getSourceCluster(), sourceEntityHeader,
                                   mapping.getTargetCluster(), targetEntityHeader );
            if ( exisingMapping == null ) {
                mapping.setTimestamp( System.currentTimeMillis() );

                MigrationMappedEntity sourceEntity = new MigrationMappedEntity();
                sourceEntity.setEntityType( sourceEntityHeader.getType() );
                sourceEntity.setEntityId( sourceEntityHeader.getStrId() );
                if ( sourceEntityHeader instanceof IdentityHeader) {
                    sourceEntity.setEntityProviderId( ((IdentityHeader)sourceEntityHeader).getProviderOid() );
                }
                sourceEntity.setEntityName( sourceEntityHeader.getName() );
                sourceEntity.setEntityDescription( sourceEntityHeader.getDescription() );
                sourceEntity.setEntityVersion( sourceEntityHeader.getVersion() );

                MigrationMappedEntity targetEntity = new MigrationMappedEntity();
                targetEntity.setEntityType( targetEntityHeader.getType() );
                targetEntity.setEntityId( targetEntityHeader.getStrId() );
                if ( targetEntityHeader instanceof IdentityHeader ) {
                    targetEntity.setEntityProviderId( ((IdentityHeader)targetEntityHeader).getProviderOid() );
                }
                targetEntity.setEntityName( targetEntityHeader.getName() );
                targetEntity.setEntityDescription( targetEntityHeader.getDescription() );
                targetEntity.setEntityVersion( targetEntityHeader.getVersion() );

                mapping.setSource( sourceEntity );
                mapping.setTarget( targetEntity );

                oid = super.save( mapping );
            } else {
                oid = exisingMapping.getOid();
            }
        } catch ( FindException fe ) {
            throw new SaveException( "Error checking for existing mapping saving.", fe );
        }
        
        return oid;
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

    @SuppressWarnings({"deprecation"})
    private EntityHeader asEntityHeader( final MigrationMappedEntity entity ) {
        EntityHeader header;
        if ( entity.getEntityProviderId() != null ) {
            IdentityHeader identityHeader = new IdentityHeader();
            identityHeader.setProviderOid( entity.getEntityProviderId() );
            header = identityHeader;
        } else {
            header = new EntityHeader();
        }

        header.setType( entity.getEntityType() );
        header.setStrId( entity.getEntityId() );
        header.setName( entity.getEntityName() );
        header.setDescription( entity.getEntityDescription() );
        header.setVersion( entity.getEntityVersion() );

        return header;
    }

    private MigrationMappingRecord findByMapping( final SsgCluster sourceCluster,
                                                  final EntityHeaderRef sourceEntityHeader,
                                                  final SsgCluster targetCluster,
                                                  final EntityHeaderRef targetEntityHeader ) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "sourceCluster", sourceCluster );
        map.put( "targetCluster", targetCluster );

        if ( sourceEntityHeader != null  ) {
            map.put("source.entityType", sourceEntityHeader.getType());
            if ( sourceEntityHeader instanceof IdentityHeader ) {
                map.put("source.entityProviderId", ((IdentityHeader)sourceEntityHeader).getProviderOid());
            }
            map.put("source.entityId", sourceEntityHeader.getStrId());
        }

        if ( targetEntityHeader != null  ) {
            map.put("target.entityType", targetEntityHeader.getType());
            if ( targetEntityHeader instanceof IdentityHeader ) {
                map.put("target.entityProviderId", ((IdentityHeader)targetEntityHeader).getProviderOid());
            }
            map.put("target.entityId", targetEntityHeader.getStrId());
        }

        List<MigrationMappingRecord> result = findMatching(Arrays.asList(map));

        MigrationMappingRecord mostRecentMatch = null;
        for ( MigrationMappingRecord record : result ) {
            if ( mostRecentMatch==null ) {
                mostRecentMatch = record;
            } else if ( mostRecentMatch.getTimestamp() < record.getTimestamp() ) {
                mostRecentMatch = record;
            }
        }

        return mostRecentMatch;
    }


}