package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity finder which can retrieve entities by Security Zone.
 */
public interface SecurityZoneEntityFinder {
    /**
     * Retrieves a collection of entities by type and security zone oid.
     *
     * @param type            the EntityType to retrieve.
     * @param securityZoneOid the oid of the SecurityZone that the entities must be in.
     * @return a collection of entities of the given EntityType which are in a SecurityZone identified by the given oid.
     * @throws FindException            if a db error occurs.
     * @throws IllegalArgumentException if the given EntityType is not security zoneable.
     */
    public Collection<Entity> findByEntityTypeAndSecurityZoneOid(@NotNull final EntityType type, final long securityZoneOid) throws FindException;

    /**
     * Retrieves a collection of entities by class and security zone oid.
     *
     * @param clazz           the class of entity to retrieve.
     * @param securityZoneOid the oid of the SecurityZone that the entities must be in.
     * @param <ET>            the entity class.
     * @return a collection of entities of the given class which are in a SecurityZone identified by the given oid.
     * @throws FindException            if a db error occurs.
     * @throws IllegalArgumentException if the given class is not security zoneable.
     */
    public <ET extends Entity> Collection<ET> findByClassAndSecurityZoneOid(@NotNull final Class<ET> clazz, final long securityZoneOid) throws FindException;
}
