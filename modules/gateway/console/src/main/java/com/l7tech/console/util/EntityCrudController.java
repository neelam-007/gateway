package com.l7tech.console.util;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides wiring to connect a table showing available entities with button actions to create new ones,
 * remove, or edit them.
 */
public class EntityCrudController<ET> {
    private static final Logger logger = Logger.getLogger(EntityCrudController.class.getName());
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
            public void actionPerformed(final ActionEvent e) {
                if (entityCreator != null) {
                    final ET entity = entityCreator.createNewEntity();
                    if (entity != null && entityEditor != null) {
                        entityEditor.displayEditDialog(entity, new Functions.UnaryVoidThrows<ET, SaveException>() {
                            @Override
                            public void call(final ET editedEntity) throws SaveException {
                                if (editedEntity != null) {
                                    try {
                                        final ET savedEntity = doSave(editedEntity);
                                        if (savedEntity != null) {
                                            entityTableModel.addRow(0, savedEntity);
                                            // select and scroll to created row
                                            final int selectIndex = entityTable.convertRowIndexToView(0);
                                            if (selectIndex >= 0) {
                                                entityTable.setRowSelectionInterval(selectIndex, selectIndex);
                                                entityTable.scrollRectToVisible(new Rectangle(entityTable.getCellRect(selectIndex, 0, true)));
                                            }
                                        }
                                    } catch (final SaveException e) {
                                        if (e.getCause() instanceof PermissionDeniedException && (((PermissionDeniedException) e.getCause()).getOperation() == OperationType.READ)) {
                                            DialogDisplayer.showMessageDialog(entityTable, "You do not have permission to view the saved entity.", "Error", JOptionPane.ERROR_MESSAGE, null);
                                        } else {
                                            throw e;
                                        }
                                    }
                                }
                            }
                        });
                    } else if (entity != null) {
                        logger.log(Level.WARNING, "Entity Editor not configured. Saving entity without user input.");
                        try {
                            final ET savedEntity = doSave(entity);
                            if (savedEntity != null) {
                                entityTableModel.addRow(entity);
                            }
                        } catch (final SaveException ex) {
                            final String mess = "Unable to save: " + ExceptionUtils.getMessage(ex);
                            logger.log(Level.WARNING, mess, e);
                            DialogDisplayer.showMessageDialog(entityTable, mess, null);
                        }
                    }
                }
            }
        };
    }

    public ActionListener createEditAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (entityEditor != null) {
                    final int rowIndex = entityTable.getSelectedRow();
                    if (rowIndex >= 0) {
                        final int modelIndex = entityTable.convertRowIndexToModel(rowIndex);
                        final ET entity = entityTableModel.getRowObject(modelIndex);
                        if (entity != null) {
                            entityEditor.displayEditDialog(entity, new Functions.UnaryVoidThrows<ET, SaveException>() {
                                @Override
                                public void call(final ET editedEntity) throws SaveException {
                                    if (editedEntity != null) {
                                        try {
                                            final ET savedEntity = doSave(editedEntity);
                                            if (savedEntity != null) {
                                                entityTableModel.setRowObject(modelIndex, savedEntity);
                                            }
                                        } catch (final SaveException e) {
                                            if (e.getCause() instanceof PermissionDeniedException && (((PermissionDeniedException) e.getCause()).getOperation() == OperationType.READ)) {
                                                entityTableModel.removeRowAt(modelIndex);
                                                DialogDisplayer.showMessageDialog(entityTable, "You do not have permission to view the saved entity.", "Error", JOptionPane.ERROR_MESSAGE, null);
                                            } else {
                                                throw e;
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }
        };
    }

    public ActionListener createDeleteAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (entityDeleter != null) {
                    final int rowIndex = entityTable.getSelectedRow();
                    final int modelIndex = entityTable.convertRowIndexToModel(rowIndex);
                    final ET entity = entityTableModel.getRowObject(modelIndex);
                    if (entity != null) {
                        entityDeleter.displayDeleteDialog(entity, new Functions.UnaryVoid<ET>() {
                            @Override
                            public void call(final ET entityToDelete) {
                                if (entityToDelete != null) {
                                    try {
                                        entityDeleter.deleteEntity(entityToDelete);
                                        entityTableModel.removeRow(entityToDelete);
                                    } catch (final DeleteException e) {
                                        final String mess = "Unable to delete: " + ExceptionUtils.getMessage(e);
                                        logger.log(Level.WARNING, mess, e);
                                        DialogDisplayer.showMessageDialog(entityTable, mess, null);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        };
    }

    //
    // Protected
    //

    protected ET doSave(ET entity) throws SaveException {
        ET savedEntity = null;
        if (entitySaver != null) {
            savedEntity = entitySaver.saveEntity(entity);
        }
        return savedEntity;
    }
}
