package com.l7tech.server.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Interface for SecurityZone-related RBAC checks.
 */
public interface ZoneUpdateSecurityChecker {
    /**
     * Check that a bulk zone update is permitted for all of the given entities.
     *
     * @param user             the User performing the update.
     * @param securityZoneGoid the goid of the SecurityZone to set on the given entities or null to remove the entities from their current SecurityZone.
     * @param entityType       the EntityType of the entities to update. Must be a zoneable EntityType.
     * @param entityIds        the ids of the entities to update.
     * @throws FindException            if a db error occurs when trying to retrieve any of the entities identified by oid.
     * @throws com.l7tech.gateway.common.security.rbac.PermissionDeniedException
     *                                  if the bulk update is not allowed for at least one of the entities.
     * @throws IllegalArgumentException if any of the entities identified by oid do not identify existing entities or the EntityType is not security zoneable.
     */
    void checkBulkUpdatePermitted(@NotNull final User user, @Nullable final Goid securityZoneGoid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws FindException;

    void checkBulkUpdatePermitted(@NotNull final User user, @Nullable final Goid securityZoneGoid, @NotNull final Map<EntityType, Collection<Serializable>> entityIds) throws FindException;
}
