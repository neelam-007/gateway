package com.l7tech.server.ems;

import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;

/**
 *
 */
public class EsmEntityFinderImpl implements EntityFinder {

    @Override
    public EntityHeaderSet<EntityHeader> findAll( final Class<? extends Entity> entityClass ) throws FindException {
        return new EntityHeaderSet<EntityHeader>();
    }

    @Override
    public Entity find( @NotNull final EntityHeader header ) throws FindException {
        return null;
    }

    @Override
    public <ET extends Entity> ET find( final Class<ET> clazz,  final Serializable pk ) throws FindException {
        return null;
    }

    @Override
    public EntityHeader findHeader( final EntityType etype,  final Serializable pk ) throws FindException {
        return null;
    }

    @Override
    public Collection<Entity> findByEntityTypeAndSecurityZoneOid(@NotNull EntityType type, long securityZoneOid) throws FindException {
        throw new NotImplementedException("method findByEntityTypeAndSecurityZoneOid is not implemented");
    }

    @Override
    public <ET extends Entity> Collection<ET> findByClassAndSecurityZoneOid(@NotNull Class<ET> clazz, long securityZoneOid) throws FindException {
        throw new NotImplementedException("method findByClassAndSecurityZoneOid is not implemented");
    }
}
