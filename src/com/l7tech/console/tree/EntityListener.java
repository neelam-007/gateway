package com.l7tech.console.tree;

import java.util.EventListener;

/** Listener to changes with Entities.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public interface EntityListener extends EventListener {

    /** Fired when an new entity is added.
     * @param ev event describing the action
     */
    public void entityAdded(EntityEvent ev);

    /**
     * Fired when an set of children is updated.
     * @param ev event describing the action
     */
    public void entityUpdated(EntityEvent ev);


    /** Fired when an entity is removed.
     * @param ev event describing the action
     */
    public void entityRemoved(EntityEvent ev);
}
