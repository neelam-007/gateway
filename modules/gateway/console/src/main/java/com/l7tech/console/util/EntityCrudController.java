package com.l7tech.console.util;

import com.l7tech.console.action.DeleteEntityNodeAction;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides wiring to connect a table showing available entities with button actions to create new ones,
 * remove, or edit them.
 */
public class EntityCrudController<ET> {
    private static final Logger logger = Logger.getLogger(EntityCrudController.class.getName());
    private static final String DELETE_CONFIRMATION_FORMAT = "Are you sure you want to remove the {0} {1}? {2}";
    private JTable entityTable;
    private SimpleTableModel<ET> entityTableModel;
    private EntityCreator<ET> entityCreator;
    private EntityEditor<ET> entityEditor;
    private EntitySaver<ET> entitySaver;
    private EntityDeleter<ET> entityDeleter;

    public EntityCrudController() {
    }

    public JTable getEntityTable() {
        return entityTable;
    }

    public void setEntityTable(JTable entityTable) {
        this.entityTable = entityTable;
    }

    public SimpleTableModel<ET> getEntityTableModel() {
        return entityTableModel;
    }

    public void setEntityTableModel(SimpleTableModel<ET> entityTableModel) {
        this.entityTableModel = entityTableModel;
    }

    public EntityCreator<ET> getEntityCreator() {
        return entityCreator;
    }

    public void setEntityCreator(EntityCreator<ET> entityCreator) {
        this.entityCreator = entityCreator;
    }

    public EntityEditor<ET> getEntityEditor() {
        return entityEditor;
    }

    public void setEntityEditor(EntityEditor<ET> entityEditor) {
        this.entityEditor = entityEditor;
    }

    public EntitySaver<ET> getEntitySaver() {
        return entitySaver;
    }

    public void setEntitySaver(EntitySaver<ET> entitySaver) {
        this.entitySaver = entitySaver;
    }

    public EntityDeleter<ET> getEntityDeleter() {
        return entityDeleter;
    }

    public void setEntityDeleter(EntityDeleter<ET> entityDeleter) {
        this.entityDeleter = entityDeleter;
    }

    //
    // Actions
    //

    public ActionListener createMoveUpAction() {
        return TableUtil.createMoveUpAction(entityTable, entityTableModel);
    }

    public ActionListener createMoveDownAction() {
        return TableUtil.createMoveDownAction(entityTable, entityTableModel);
    }

    public ActionListener createCreateAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (entityCreator != null) {
                    ET entity = entityCreator.createNewEntity();
                    if (entity != null && entityEditor != null) {
                        entityEditor.displayEditDialog(entity, new Functions.UnaryVoid<ET>() {
                            @Override
                            public void call(ET editedEntity) {
                                if (editedEntity != null) {
                                    if (null != (editedEntity = doSave(editedEntity)))
                                        entityTableModel.addRow(editedEntity);
                                }
                            }
                        });
                    } else if (entity != null) {
                        if (null != (entity = doSave(entity)))
                            entityTableModel.addRow(entity);
                    }
                }
            }
        };
    }

    /**
     * Creates an ActionListener which opens an edit dialog for the selected row.
     *
     * @param callback optional Runnable to execute after a successful edit.
     * @return an ActionListener which opens an edit dialog for the selected row.
     */
    public ActionListener createEditAction(@Nullable final Runnable callback) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (entityEditor != null) {
                    final int rowIndex = entityTable.getSelectedRow();
                    final int modelIndex = entityTable.convertRowIndexToModel(rowIndex);
                    final ET entity = entityTableModel.getRowObject(modelIndex);
                    if (entity != null) {
                        entityEditor.displayEditDialog(entity, new Functions.UnaryVoid<ET>() {
                            @Override
                            public void call(ET editedEntity) {
                                if (editedEntity != null) {
                                    if (null != (editedEntity = doSave(editedEntity))) {
                                        entityTableModel.setRowObject(rowIndex, editedEntity);
                                        if (callback != null) {
                                            callback.run();
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        };
    }

    /**
     * @return a bare bones delete ActionListener which just deletes the entity on actionPerformed.
     */
    public ActionListener createDeleteAction() {
        return createDeleteAction(null, null, null);
    }

    /**
     * Creates a delete action with a confirmation prompt if the entity is a NamedEntity.
     * <p/>
     * If either/both entityType or parent are null, the delete action will delete without confirmation.
     *
     * @param entityType        the EntityType of the entity to delete which is used for the confirmation prompt.
     * @param parent            the Component which is a parent to the input dialog.
     * @param additionalMessage an optional additional message to display to the user when confirming the deletion under the usual "are you sure" message.
     * @return a delete ActionListener which prompts the user to confirm before deleting the entity on actionPerformed.
     */
    public ActionListener createDeleteAction(@Nullable final EntityType entityType, @Nullable final Component parent, @Nullable final String additionalMessage) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (entityDeleter != null) {
                    final int rowIndex = entityTable.getSelectedRow();
                    final int modelIndex = entityTable.convertRowIndexToModel(rowIndex);
                    final ET entity = entityTableModel.getRowObject(modelIndex);
                    if (entity != null) {
                        if (entity instanceof NamedEntity && entityType != null && parent != null) {
                            confirmDelete((NamedEntity) entity, entityType, parent, additionalMessage);
                        } else {
                            delete(entity);
                        }
                    }
                }
            }
        };
    }

    private void confirmDelete(final NamedEntity namedEntity, final EntityType entityType, final Component parent, final String additionalMessage) {
        final String additionalWarning = additionalMessage == null ? StringUtils.EMPTY : additionalMessage;
        final String msg = MessageFormat.format(DELETE_CONFIRMATION_FORMAT, entityType.getName().toLowerCase(), namedEntity.getName(), additionalWarning);
        DialogDisplayer.showOptionDialog(
                parent,
                WordUtils.wrap(msg, DeleteEntityNodeAction.LINE_CHAR_LIMIT, null, true),
                "Remove " + entityType.getName(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{"Remove " + entityType.getName(), "Cancel"},
                null,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == 0) {
                            delete((ET) namedEntity);
                        }
                    }
                });
    }

    private void delete(final ET entity) {
        try {
            entityDeleter.deleteEntity(entity);
            entityTableModel.removeRow(entity);
        } catch (final DeleteException e) {
            final String mess = "Unable to delete: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, mess, e);
            DialogDisplayer.showMessageDialog(entityTable, mess, null);
        }
    }

    //
    // Protected
    //

    protected ET doSave(ET entity) {
        if (entitySaver != null) {
            try {
                return entitySaver.saveEntity(entity);
            } catch (SaveException e) {
                final String mess = "Unable to save: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, mess, e);
                DialogDisplayer.showMessageDialog(entityTable, mess, null);
                return null;
            }
        }
        return null;
    }
}
