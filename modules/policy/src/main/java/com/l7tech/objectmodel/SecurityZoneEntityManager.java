package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Interface for managing the Security Zone for zoneable entities.
 */
public interface SecurityZoneEntityManager extends SecurityZoneEntityFinder {
    /**
     * Sets the given SecurityZone on a collection of entities.
     *
     * @param securityZoneGoid the goid of the SecurityZone to set on the entities or null to remove the entities from their current SecurityZone.
     * @param entityType      the EntityType of the entities. Must be a zoneable EntityType.
     * @param entityIds       a collection of object ids that identify the entities to update.
     * @throws UpdateException if a db error occurs or any of the object ids provided do not identify existing entities.
     */
    public void setSecurityZoneForEntities(@Nullable final Goid securityZoneGoid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws UpdateException;

    /**
     * Sets the given SecurityZone on a map of entities.
     *
     * @param securityZoneGoid the goid of the SecurityZone to set on the entities or null to remove the entities from their current SecurityZone.
     * @param entityIds       a map where key = entity type of the entities to update and value = collection of ids which represent the entities to update.
     * @throws UpdateException if a db error occurs or any of the object ids provided do not identify existing entities.
     */
    public void setSecurityZoneForEntities(@Nullable final Goid securityZoneGoid, @NotNull Map<EntityType, Collection<Serializable>> entityIds) throws UpdateException;
}
