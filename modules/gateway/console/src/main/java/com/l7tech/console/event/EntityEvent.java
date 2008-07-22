package com.l7tech.console.event;

import com.l7tech.objectmodel.EntityHeader;

import java.util.EventObject;

/**
 *  Event describing a change in Entities.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EntityEvent extends EventObject {
    private EntityHeader header;

    /** Create a new event.
     * @param o origin object
     */
    public EntityEvent(Object o, EntityHeader h) {
        super(o);
        header = h;
    }

    /** Get the entity that is a part of the change occurred.
     * @return the entity
     */
    public final Object getEntity() {
        return header;
    }
}
