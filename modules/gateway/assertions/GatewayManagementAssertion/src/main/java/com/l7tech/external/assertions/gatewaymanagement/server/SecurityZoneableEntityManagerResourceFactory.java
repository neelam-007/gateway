package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.SecurityZoneableObject;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Abstract base class for Security Zone aware entity manager resource factory.
 */
public abstract class SecurityZoneableEntityManagerResourceFactory<R extends SecurityZoneableObject, E extends PersistentEntity, EH extends EntityHeader> extends EntityManagerResourceFactory<R, E, EH> {

    //- PROTECTED

    protected void doSecurityZoneAsResource(R resource, final E entity) {
        if (entity instanceof ZoneableEntity) {
            ZoneableEntity zoneable = (ZoneableEntity) entity;

            if (zoneable.getSecurityZone() != null) {
                resource.setSecurityZoneId( zoneable.getSecurityZone().getId() );
                resource.setSecurityZone( zoneable.getSecurityZone().getName() );
            }
        }
    }

    protected void doSecurityZoneFromResource(R resource, final E entity) throws InvalidResourceException {
        if (entity instanceof ZoneableEntity) {

            if (resource.getSecurityZoneId() != null && !resource.getSecurityZoneId().isEmpty()) {
                final Goid securityZoneId;
                try {
                    securityZoneId = Goid.parseGoid( resource.getSecurityZoneId() );
                } catch( IllegalArgumentException nfe ) {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
                }
                try {

                    SecurityZone zone = securityZoneManager.findByPrimaryKey( securityZoneId );
                    if ( zone == null )
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
                    if ( !zone.permitsEntityType(EntityType.findTypeByEntity(entity.getClass())) )
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "entity type not permitted for referenced security zone");

                    ((ZoneableEntity) entity).setSecurityZone( zone );

                } catch (FindException e) {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
                }
            }
        }
    }

    //- PACKAGE

    SecurityZoneableEntityManagerResourceFactory(final boolean readOnly,
                                                           final boolean allowNameSelection,
                                                           final RbacServices rbacServices,
                                                           final SecurityFilter securityFilter,
                                                           final PlatformTransactionManager transactionManager,
                                                           final EntityManager<E, EH> manager,
                                                           final SecurityZoneManager securityZoneManager) {
        this(readOnly, allowNameSelection, false, rbacServices, securityFilter, transactionManager, manager, securityZoneManager);
    }

    SecurityZoneableEntityManagerResourceFactory(final boolean readOnly,
                                                           final boolean allowNameSelection,
                                                           final boolean allowGuidSelection,
                                                           final RbacServices rbacServices,
                                                           final SecurityFilter securityFilter,
                                                           final PlatformTransactionManager transactionManager,
                                                           final EntityManager<E, EH> manager,
                                                           final SecurityZoneManager securityZoneManager) {
        super(readOnly, allowNameSelection, allowGuidSelection, rbacServices, securityFilter, transactionManager, manager);
        this.securityZoneManager = securityZoneManager;
    }

    //- PRIVATE

    protected SecurityZoneManager securityZoneManager;
}
