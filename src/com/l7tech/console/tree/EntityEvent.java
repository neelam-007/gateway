package com.l7tech.console.tree;

import java.util.EventObject;

/**
 *  Event describing a change in Entities.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class EntityEvent extends EventObject {
    /** Create a new event.
     * @param o origin object
     */
    public EntityEvent(Object o) {
        super(o);
    }

    /** Get the entity that is a part of the change occurred.
     * @return the entity
     */
    public final Object getEntity() {
        return getSource();
    }
}
