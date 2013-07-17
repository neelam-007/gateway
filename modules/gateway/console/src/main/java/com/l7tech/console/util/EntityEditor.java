package com.l7tech.console.util;

import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.Functions;

/**
 * Interface implemented by users of EntityCrudController to open a properties dialog to view or edit an entity.
 */
public interface EntityEditor<ET> {
    /**
     * Edit the specified entity.  Afterward, invoke afterEditListener with the edited entity, or null if the edit was cancelled.
     * <p/>
     * Implementors should take care not to edit the original copy of the entity in this method since the eventual
     * save may still fail.
     *
     * @param entity the existing or newly-created entity to edit.  Required.
     * @param afterEditListener a callback to invoke when the edit is complete.  Implementors must invoke this
     *                          when the edit is complete with either the edited entity or null if the edit was cancelled.
     *
     */
    void displayEditDialog(ET entity, Functions.UnaryVoidThrows<ET, SaveException> afterEditListener);
}
