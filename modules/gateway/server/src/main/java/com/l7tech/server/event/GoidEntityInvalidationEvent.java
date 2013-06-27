package com.l7tech.server.event;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import org.springframework.context.ApplicationEvent;

/**
 * Event raised when a database change is detected.
 *
 * <p>This event is raised periodically when changes in db state are detected.</p>
 *
 * This is the goid version of EntityInvalidationEvent
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class GoidEntityInvalidationEvent extends ApplicationEvent {
    public static final char DELETE = 'D';
    public static final char UPDATE = 'U';
    public static final char CREATE = 'C';

    //- PUBLIC

    /**
     * Create an EntityInvalidationEvent
     *
     * @param source      The source of invalidation (not usually of interest)
     * @param entityClass The class of entity being invalidated (this will be the interface if any)
     * @param entityGoids   The ids of the invalidated entities
     * @param entityOps   The operations that were detected against the entities whose OIDs are in entityIds; 'C' = created, 'U' = updated, 'D' = deleted
     */
    public GoidEntityInvalidationEvent(final Object source,
                                       final Class<? extends Entity> entityClass,
                                       final Goid[] entityGoids,
                                       final char[] entityOps)
    {
        super(source);
        if(entityClass==null) throw new IllegalArgumentException("entityClass must not be null");
        if(!GoidEntity.class.isAssignableFrom(entityClass)) throw new IllegalArgumentException("GoidEntity must be assignable from entityClass");
        if(entityGoids==null) throw new IllegalArgumentException("entityIds must not be null");
        if (entityOps.length != entityGoids.length) throw new IllegalArgumentException("entityIds must have the same length as entityOps");

        this.entityClass = entityClass;
        Goid[] myEntityIds = new Goid[entityGoids.length];
        System.arraycopy(entityGoids,0,myEntityIds,0,entityGoids.length);
        this.entityIds = myEntityIds;

        char[] myEntityOps = new char[entityOps.length];
        System.arraycopy(entityOps,0,myEntityOps,0,entityOps.length);
        this.entityOperations = myEntityOps;
    }

    /**
     * Get the class of entity being invalidated.
     *
     * @return The Entity sub-class
     */
    public Class<? extends Entity> getEntityClass() {
        return entityClass;
    }

    /**
     * Get the list of entities that are invalid.
     *
     * @return the ids (not null)
     */
    public Goid[] getEntityIds() {
        Goid[] entityIds = new Goid[this.entityIds.length];
        System.arraycopy(this.entityIds,0,entityIds,0,this.entityIds.length);
        return entityIds;
    }

    /**
     * Get the list of codes for the operations detected against the entities whose OIDs are in {@link #getEntityIds}.
     * @return the operation codes (not null)
     */
    public char[] getEntityOperations() {
        char[] entityOps = new char[this.entityOperations.length];
        System.arraycopy(this.entityOperations,0,entityOps,0,this.entityOperations.length);
        return entityOps;
    }

    //- PRIVATE

    private final Class<? extends Entity> entityClass;
    private final Goid[] entityIds;
    private final char[] entityOperations;
}
