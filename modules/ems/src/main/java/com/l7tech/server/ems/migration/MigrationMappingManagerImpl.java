package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@Transactional(rollbackFor=Throwable.class)
public class MigrationMappingManagerImpl extends HibernateEntityManager<MigrationMapping, EntityHeader> implements MigrationMappingManager {

    //- PUBLIC

    public MigrationMappingManagerImpl( final SsgClusterManager ssgClusterManager ) {
        this.ssgClusterManager = ssgClusterManager;
    }

    @Override
    public long saveMapping( final String sourceClusterGuid,
                             final EntityHeader sourceEntityHeader,
                             final String targetClusterGuid,
                             final EntityHeader targetEntityHeader) throws SaveException {
        MigrationMapping mapping = new MigrationMapping();
        try {
            mapping.setSourceCluster( ssgClusterManager.findByGuid( sourceClusterGuid ) );
            mapping.setTargetCluster( ssgClusterManager.findByGuid( targetClusterGuid ) );
        } catch ( FindException fe ) {
            throw new SaveException( "Error finding cluster by GUID when saving mapping.", fe );            
        }

        MigrationMappedEntity sourceEntity = new MigrationMappedEntity();
        sourceEntity.setEntityType( sourceEntityHeader.getType() );
        sourceEntity.setEntityId( sourceEntityHeader.getStrId() );
        if ( sourceEntityHeader instanceof IdentityHeader) {
            sourceEntity.setEntityProviderId( Long.toString(((IdentityHeader)sourceEntityHeader).getProviderOid()) );
        }
        sourceEntity.setEntityName( sourceEntityHeader.getName() );
        sourceEntity.setEntityDescription( sourceEntityHeader.getDescription() );
        sourceEntity.setEntityVersion(1); //TODO add version to entity header

        MigrationMappedEntity targetEntity = new MigrationMappedEntity();
        targetEntity.setEntityType( targetEntityHeader.getType() );
        targetEntity.setEntityId( targetEntityHeader.getStrId() );
        if ( targetEntityHeader instanceof IdentityHeader ) {
            targetEntity.setEntityProviderId( Long.toString(((IdentityHeader)targetEntityHeader).getProviderOid()) );
        }
        targetEntity.setEntityName( targetEntityHeader.getName() );
        targetEntity.setEntityDescription( targetEntityHeader.getDescription() );
        targetEntity.setEntityVersion(1); //TODO add version to entity header

        mapping.setSource( sourceEntity );
        mapping.setTarget( targetEntity );

        return super.save( mapping );
    }

    @Override
    public Class<MigrationMapping> getImpClass() {
        return MigrationMapping.class;
    }

    @Override
    public Class<MigrationMapping> getInterfaceClass() {
        return MigrationMapping.class;
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
}