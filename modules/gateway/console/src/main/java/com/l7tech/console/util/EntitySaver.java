package com.l7tech.console.util;

import com.l7tech.objectmodel.SaveException;

/**
 * Interface implemented by users of EntityCrudController in order to save entities after they have been created
 * or edited.
 */
public interface EntitySaver<ET> {
    /**
     * Save the specified (new or updated) entity.  A normal return means the save was successful and that,
     * in the case of a new entity, any needed persistent identifiers have been updated in the instance
     * returned from this method.
     * <p/>
     * If this is a new entity, and the entity has a persistent ID of some kind, the implementor of this method
     * is responsible for updating the persistent ID in the entity after it is saved, before returning normally from
     * this method.
     *
     * @param entity the entity to save.  Required.
     * @return the most up-to-date version of the entity, after the save.
     *         A persistent ID has been assigned if applicable.
     * @throws SaveException if the entity cannot be saved.
     */
    ET saveEntity(ET entity) throws SaveException;
}
