package com.l7tech.objectmodel;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.NamedEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity finder which can retrieve entities by Security Zone.
 */
public interface SecurityZoneEntityFinder {
    /**
     * @param type            the EntityType to retrieve.
     * @param securityZoneOid the oid of the SecurityZone that the entities must be in.
     * @return a collection of entities of the given EntityType which are in a SecurityZone identified by the given oid.
     * @throws FindException if a db error occurs or the given EntityType is not supported.
     */
    public Collection<Entity> findByEntityTypeAndSecurityZoneOid(@NotNull final EntityType type, final long securityZoneOid) throws FindException;
}
