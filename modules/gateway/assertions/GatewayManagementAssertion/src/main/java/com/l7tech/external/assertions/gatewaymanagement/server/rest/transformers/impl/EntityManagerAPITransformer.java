package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.SecurityZoneableObject;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.GoidUpgradeMapper;

import javax.inject.Inject;

/**
 * Base transformer class that used entity managers
 */
public abstract class EntityManagerAPITransformer<M, E extends Entity> implements EntityAPITransformer<M,E> {
    @Inject
    ServiceManager serviceManager;
    @Inject
    SecurityZoneManager securityZoneManager;

    protected void doSecurityZoneToMO(M resource, final E entity) {
        if (resource instanceof SecurityZoneableObject && entity instanceof ZoneableEntity) {

            if (((ZoneableEntity)entity).getSecurityZone() != null) {
                ((SecurityZoneableObject)resource).setSecurityZoneId(((ZoneableEntity)entity).getSecurityZone().getId());
                ((SecurityZoneableObject)resource).setSecurityZone(((ZoneableEntity)entity).getSecurityZone().getName());
            }
        }
    }

    protected void doSecurityZoneFromMO(M resource, final E entity, final boolean strict)
            throws ResourceFactory.InvalidResourceException {
        if (resource instanceof SecurityZoneableObject && entity instanceof ZoneableEntity) {

            if (((SecurityZoneableObject)resource).getSecurityZoneId() != null && !((SecurityZoneableObject)resource).getSecurityZoneId().isEmpty()) {
                final Goid securityZoneId;
                try {
                    securityZoneId = GoidUpgradeMapper.mapId(EntityType.SECURITY_ZONE, ((SecurityZoneableObject)resource).getSecurityZoneId());
                } catch (IllegalArgumentException nfe) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                            "invalid or unknown security zone reference");
                }
                SecurityZone zone = null;
                try {
                    zone = securityZoneManager.findByPrimaryKey(securityZoneId);
                } catch (FindException e) {
                    if (strict)
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "invalid or unknown security zone reference");
                }
                if (strict) {
                    if (zone == null)
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "invalid or unknown security zone reference");
                    if (!zone.permitsEntityType(EntityType.findTypeByEntity(entity.getClass())))
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "entity type not permitted for referenced security zone");
                } else if (zone == null) {
                    zone = new SecurityZone();
                    zone.setGoid(securityZoneId);
                    zone.setName(((SecurityZoneableObject)resource).getSecurityZone());
                }
                ((ZoneableEntity)entity).setSecurityZone(zone);
            }
        }
    }
}
