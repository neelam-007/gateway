package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity finder which can retrieve entities by Security Zone.
 */
public interface SecurityZoneEntityFinder {
    /**
     * Retrieves a collection of ZoneableEntityHeader by type and security zone oid.
     *
     * @param type            the EntityType to retrieve.
     * @param securityZoneOid the oid of the SecurityZone that the entities must be in.
     * @return a collection of EntityHeader of the given EntityType which are in a SecurityZone identified by the given oid.
     * @throws FindException            if a db error occurs.
     * @throws IllegalArgumentException if the given EntityType is not security zoneable.
     */
    public Collection<EntityHeader> findByEntityTypeAndSecurityZoneOid(@NotNull final EntityType type, final long securityZoneOid) throws FindException;
}
