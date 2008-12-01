package com.l7tech.server.ems;

import com.l7tech.server.EntityFinder;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 *
 */
public class EsmEntityFinderImpl implements EntityFinder {

    @Override
    public EntityHeaderSet<EntityHeader> findAll( final Class<? extends Entity> entityClass ) throws FindException {
        return new EntityHeaderSet<EntityHeader>();
    }

    @Override
    public Entity find( final EntityHeader header ) throws FindException {
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
}
