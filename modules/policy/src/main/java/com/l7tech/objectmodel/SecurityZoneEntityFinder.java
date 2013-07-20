package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity finder which can retrieve entities by Security Zone.
 */
public interface SecurityZoneEntityFinder {
    /**
     * Retrieves a collection of ZoneableEntityHeader by type and security zone goid.
     *
     * @param type            the EntityType to retrieve.
     * @param securityZoneGoid the goid of the SecurityZone that the entities must be in.
     * @return a collection of EntityHeader of the given EntityType which are in a SecurityZone identified by the given goid.
     * @throws FindException            if a db error occurs.
     * @throws IllegalArgumentException if the given EntityType is not security zoneable.
     */
    public Collection<EntityHeader> findByEntityTypeAndSecurityZoneGoid(@NotNull final EntityType type, final Goid securityZoneGoid) throws FindException;
}
