package com.l7tech.console.util;

import com.l7tech.objectmodel.DeleteException;
import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by users of EntityCrudController in order to delete entities from persistent store.
 */
public interface EntityDeleter<ET> {
    /**
     * Delete the specified entity from persistent store.  A normal return means the deletion was successful.
     *
     * @param entity the entity to delete.  Required.
     * @throws DeleteException if deletion fails
     */
    void deleteEntity(@NotNull final ET entity) throws DeleteException;
}
