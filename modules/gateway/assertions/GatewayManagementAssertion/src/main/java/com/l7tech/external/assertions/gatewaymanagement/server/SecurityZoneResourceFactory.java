package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@ResourceFactory.ResourceType(type=SecurityZoneMO.class)
public class SecurityZoneResourceFactory extends EntityManagerResourceFactory<SecurityZoneMO, SecurityZone, EntityHeader> {

    //- PUBLIC

    public SecurityZoneResourceFactory(final RbacServices rbacServices,
                                       final SecurityFilter securityFilter,
                                       final PlatformTransactionManager transactionManager,
                                       final SecurityZoneManager securityZoneManager) {
        super(false, true, false, rbacServices, securityFilter, transactionManager, securityZoneManager);
        this.securityZoneManager = securityZoneManager;
    }

    //- PROTECTED

    @Override
    protected SecurityZoneMO asResource(SecurityZone securityZone) {
        SecurityZoneMO zoneResource = ManagedObjectFactory.createSecurityZone();

        zoneResource.setId( securityZone.getId() );
        zoneResource.setVersion( securityZone.getVersion() );
        zoneResource.setName( securityZone.getName() );
        zoneResource.setDescription( securityZone.getDescription() );

        EntityType[] permittedTypesEnums = new EntityType[securityZone.getPermittedEntityTypes().size()];
        permittedTypesEnums = securityZone.getPermittedEntityTypes().toArray(permittedTypesEnums);
        List<String> permittedEntityTypes  = new ArrayList<String>();
        for (EntityType en : permittedTypesEnums) {

            permittedEntityTypes.add( en.toString() );

        }
        zoneResource.setPermittedEntityTypes( permittedEntityTypes );

        return zoneResource;
    }

    @Override
    protected SecurityZone fromResource(Object resource) throws InvalidResourceException {

        if ( !(resource instanceof SecurityZoneMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected security zone");

        final SecurityZoneMO zoneResource = (SecurityZoneMO) resource;

        final SecurityZone securityZoneEntity;
        securityZoneEntity = new SecurityZone();
        securityZoneEntity.setName(zoneResource.getName());
        securityZoneEntity.setDescription( zoneResource.getDescription() );

        if (zoneResource.getPermittedEntityTypes() != null && zoneResource.getPermittedEntityTypes().size() > 0) {
            List<EntityType> entityList = new ArrayList<EntityType>();
            for (String en : zoneResource.getPermittedEntityTypes()) {
                // Possible exception due to invalid input
                final EntityType et;
                try {
                    et = EntityType.valueOf( en );
                } catch (IllegalArgumentException iae) {
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid security zoneable entity " + en);
                }

                if (et.isSecurityZoneable() || EntityType.ANY.equals( et ))
                    entityList.add( et );
                else
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid security zoneable entity " + en );
            }
            securityZoneEntity.getPermittedEntityTypes().addAll(entityList);
        } else {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "empty permitted entity types" );
        }

        return securityZoneEntity;
    }

    @Override
    protected void updateEntity(SecurityZone oldEntity, SecurityZone newEntity) throws InvalidResourceException {
        oldEntity.setName( newEntity.getName() );
        oldEntity.setDescription( newEntity.getDescription() );
        oldEntity.setPermittedEntityTypes( newEntity.getPermittedEntityTypes() );
    }

    //- PRIVATE

    private SecurityZoneManager securityZoneManager;
}
