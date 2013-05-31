package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Interface for managing the Security Zone for zoneable entities.
 */
public interface SecurityZoneEntityManager extends SecurityZoneEntityFinder {
    /**
     * Sets the given SecurityZone on a collection of entities.
     *
     * @param securityZoneOid the oid of the SecurityZone to set on the entities or null to remove the entities from their current SecurityZone.
     * @param entityType      the EntityType of the entities. Must be a zoneable EntityType.
     * @param entityOids      a collection of object ids that identify the entities to update.
     * @throws UpdateException if a db error occurs or any of the object ids provided do not identify existing entities.
     */
    public void setSecurityZoneForEntities(@Nullable final Long securityZoneOid, @NotNull final EntityType entityType, @NotNull final Collection<Long> entityOids) throws UpdateException;
}
