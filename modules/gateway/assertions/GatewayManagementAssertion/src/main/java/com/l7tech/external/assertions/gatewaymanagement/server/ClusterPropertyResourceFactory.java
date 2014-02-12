package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
@ResourceFactory.ResourceType(type=ClusterPropertyMO.class)
public class ClusterPropertyResourceFactory extends EntityManagerResourceFactory<ClusterPropertyMO, ClusterProperty, EntityHeader> {

    //- PUBLIC

    public ClusterPropertyResourceFactory( final RbacServices services,
                                           final SecurityFilter securityFilter,
                                           final PlatformTransactionManager transactionManager,
                                           final ClusterPropertyManager clusterPropertyManager ) {
        super( false, true, services, securityFilter, transactionManager, clusterPropertyManager );
    }

    //- PROTECTED

    @Override
    public ClusterPropertyMO asResource( final ClusterProperty clusterProperty ) {
        ClusterPropertyMO property = ManagedObjectFactory.createClusterProperty();

        property.setName( clusterProperty.getName() );
        property.setValue( clusterProperty.getValue() );

        return property;
    }

    @Override
    protected ClusterProperty fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof ClusterPropertyMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected cluster property");

        final ClusterPropertyMO propertyResource = (ClusterPropertyMO) resource;

        ClusterProperty propertyEntity = new ClusterProperty();
        propertyEntity.setName( asName(propertyResource.getName()) );
        propertyEntity.setValue( propertyResource.getValue() );

        if ( propertyEntity.isHiddenProperty() )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "reserved name '"+propertyResource.getName()+"'");

        return propertyEntity;
    }

    @Override
    protected void updateEntity( final ClusterProperty oldEntity,
                                 final ClusterProperty newEntity ) throws InvalidResourceException {
        oldEntity.setValue( newEntity.getValue() );
    }

    @Override
    protected List<EntityHeader> filterHeaders( final List<EntityHeader> headers ) {
        List<EntityHeader> filtered = new ArrayList<>(headers.size());

        final ClusterProperty dummy = new ClusterProperty();
        for ( final EntityHeader entityHeader : headers ) {
            dummy.setName( entityHeader.getName() );
            if ( !dummy.isHiddenProperty() ) {
                filtered.add( entityHeader );   
            }
        }

        return filtered;
    }

    @Override
    protected ClusterProperty filterEntity( final ClusterProperty entity ) {
        return entity.isHiddenProperty() ? null : entity; 
    }
}
