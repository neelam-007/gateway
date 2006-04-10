package com.l7tech.server.event;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;

import org.springframework.context.ApplicationEvent;

import com.l7tech.objectmodel.Entity;

/**
 * Event raised when a database change is detected.
 *
 * <p>This event is raised periodically when changes in db state are detected.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class EntityInvalidationEvent extends ApplicationEvent {

    //- PUBLIC

    /**
     * Create an EntityInvalidationEvent
     *
     * @param source      The source of invalidation (not usually of interest)
     * @param entityClass The class of entity being invalidated (this will be the interface if any)
     * @param entityIds   The ids of the invalidated entities
     */
    public EntityInvalidationEvent(Object source, Class entityClass, long[] entityIds) {
        super(source);
        if(entityClass==null) throw new IllegalArgumentException("entityClass must not be null");
        if(!Entity.class.isAssignableFrom(entityClass)) throw new IllegalArgumentException("entityClass must be an Entity");
        if(entityIds==null) throw new IllegalArgumentException("entityIds must not be null");

        this.entityClass = entityClass;
        this.entityIds = new long[entityIds.length];
        System.arraycopy(entityIds,0,this.entityIds,0,entityIds.length);
    }

    /**
     * Get the class of entity being invalidated.
     *
     * @return The Entity sub-class
     */
    public Class getEntityClass() {
        return entityClass;
    }

    /**
     * Get the list of entities that are invalid.
     *
     * @return the ids (not null)
     */
    public long[] getEntityIds() {
        long[] entityIds = new long[this.entityIds.length];
        System.arraycopy(this.entityIds,0,entityIds,0,this.entityIds.length);
        return entityIds;
    }

    //- PRIVATE

    private final Class entityClass;
    private final long[] entityIds;
}
