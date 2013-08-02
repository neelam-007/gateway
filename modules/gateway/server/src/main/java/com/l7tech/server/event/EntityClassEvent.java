package com.l7tech.server.event;

import com.l7tech.objectmodel.Entity;
import org.springframework.context.ApplicationEvent;

/**
 * Superclass for application events that pertain to a specific class of entities.
 * <p/>
 * The intended general interpretation of events of this type is "something has just happened that relates to this type
 * of entity."  One example is entity invalidation (detection that an entity has been created/updated/deleted,
 * possibly on another cluster node).
 */
public abstract class EntityClassEvent extends ApplicationEvent {
    public EntityClassEvent(Object source) {
        super(source);
    }

    /**
     * Get the class of entity implicated in this event.
     *
     * @return The Entity sub-class
     */
    public abstract Class<? extends Entity> getEntityClass();
}
