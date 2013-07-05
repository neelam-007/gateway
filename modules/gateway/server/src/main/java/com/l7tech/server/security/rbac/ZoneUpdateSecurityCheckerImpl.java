package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class ZoneUpdateSecurityCheckerImpl implements ZoneUpdateSecurityChecker {
    @Inject
    private EntityFinder entityFinder;
    @Inject
    private RbacServices rbacServices;

    @Override
    public void checkBulkUpdatePermitted(@NotNull final User user, @Nullable final Long securityZoneOid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws FindException {
        Validate.isTrue(entityType.isSecurityZoneable(), "Entity type is not security zoneable: " + entityType);
        SecurityZone zoneToSet = null;
        if (securityZoneOid != null) {
            // don't need to check if user can look up this zone as we are just using it to update the entity
            zoneToSet = entityFinder.find(SecurityZone.class, securityZoneOid);
            Validate.notNull(zoneToSet, "No security zone with oid " + securityZoneOid + " exists.");
        }

        for (final Serializable id : entityIds) {
            final Entity entity = entityFinder.find(entityType.getEntityClass(), id);
            if (entity instanceof ZoneableEntity) {
                final ZoneableEntity zoneable = (ZoneableEntity) entity;
                checkPermittedForEntity(user, entity);
                zoneable.setSecurityZone(zoneToSet);
                checkPermittedForEntity(user, entity);
            } else {
                throw new IllegalArgumentException("Entity with id " + id + " does not exist or is not Security Zoneable");
            }
        }
    }

    @Override
    public void checkBulkUpdatePermitted(@NotNull User user, @Nullable Long securityZoneOid, @NotNull Map<EntityType, Collection<Serializable>> entityIds) throws FindException {
        for (final Map.Entry<EntityType, Collection<Serializable>> entry : entityIds.entrySet()) {
            checkBulkUpdatePermitted(user, securityZoneOid, entry.getKey(), entry.getValue());
        }
    }

    private void checkPermittedForEntity(final User user, final Entity entity) throws FindException {
        if (!rbacServices.isPermittedForEntity(user, entity, OperationType.UPDATE, null)) {
            throw new PermissionDeniedException(OperationType.UPDATE, entity, null);
        }
    }
}
