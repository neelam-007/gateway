package com.l7tech.objectmodel;

/**
 * Listeners for entity invalidation may implement this interface.
 *
 * @author steve
 */
public interface EntityInvalidationListener {

    /**
     * Notification of entity invalidation.
     *
     * @param entityHeader Descriptor for the invalidated entity
     */
    void notifyDelete(EntityHeader entityHeader);

    /**
     * Notification of entity invalidation.
     *
     * @param entityHeader Descriptor for the invalidated entity
     */
    void notifyUpdate(EntityHeader entityHeader);

}
