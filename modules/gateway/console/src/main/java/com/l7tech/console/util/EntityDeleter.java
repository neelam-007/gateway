package com.l7tech.console.util;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.util.Functions;
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

    /**
     * Display a confirmation dialog for deletion of the entity. If no confirmation dialog is required, implementations can just call the afterDeleteListener immediately.
     *
     * @param entity              the entity to delete.
     * @param afterDeleteListener a callback to invoke when the deletion is confirmed.
     *                            Implementors must invoke this with either the entity to delete if deletion was confirmed or null if the deletion was cancelled.
     */
    void displayDeleteDialog(@NotNull final ET entity, @NotNull final Functions.UnaryVoid<ET> afterDeleteListener);
}
