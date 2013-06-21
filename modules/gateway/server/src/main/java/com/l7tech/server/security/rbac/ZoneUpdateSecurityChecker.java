package com.l7tech.server.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for SecurityZone-related RBAC checks.
 */
public interface ZoneUpdateSecurityChecker {
    /**
     * Check that a bulk zone update is permitted for all of the given entities.
     *
     * @param user            the User performing the update.
     * @param securityZoneOid the oid of the SecurityZone to set on the given entities or null to remove the entities from their current SecurityZone.
     * @param entityType      the EntityType of the entities to update. Must be a zoneable EntityType.
     * @param entityOids      the oids of the entities to update.
     * @throws FindException            if a db error occurs when trying to retrieve any of the entities identified by oid.
     * @throws com.l7tech.gateway.common.security.rbac.PermissionDeniedException
     *                                  if the bulk update is not allowed for at least one of the entities.
     * @throws IllegalArgumentException if any of the entities identified by oid do not identify existing entities or the EntityType is not security zoneable.
     */
    void checkBulkUpdatePermitted(@NotNull final User user, @Nullable final Long securityZoneOid, @NotNull final EntityType entityType, @NotNull final Collection<Long> entityOids) throws FindException;

    void checkBulkUpdatePermitted(@NotNull final User user, @Nullable final Long securityZoneOid, @NotNull final Map<EntityType, Collection<Long>> entityOids) throws FindException;
}
