package com.l7tech.external.assertions.whichmodule.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.whichmodule.DemoGenericEntity;
import com.l7tech.external.assertions.whichmodule.DemoGenericEntityAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class ManageDemoGenericEntitiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton closeButton;
    private JButton editButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton viewButton;
    private JTable entityTable;

    private SimpleTableModel<DemoGenericEntity> entityTableModel;

    public ManageDemoGenericEntitiesDialog(Window owner) {
        super(owner);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        entityTableModel = TableUtil.configureTable(entityTable,
                column("Name", 25, 200, 99999, propertyTransform(DemoGenericEntity.class, "name")),
                column("Age", 25, 60, 99999, propertyTransform(DemoGenericEntity.class, "age")),
                column("Plays Trombone", 25, 50, 99999, propertyTransform(DemoGenericEntity.class, "playsTrombone")));
        entityTable.setModel(entityTableModel);

        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSelectedEntity(true, null);
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSelectedEntity(false, entitySaver);
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEntity(new DemoGenericEntity(), false, entitySaver);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DemoGenericEntity entity = getSelectedEntity();
                if (entity == null)
                    return;

                DialogDisplayer.showConfirmDialog(removeButton, "Delete demo entity " + entity.getName() + "?", "Confirm Delete Entity",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (JOptionPane.OK_OPTION == option) {
                            try {
                                getEntityManager().delete(entity);
                            } catch (DeleteException e1) {
                                error("Unable to delete entity: " + ExceptionUtils.getMessage(e1));
                            } catch (FindException e1) {
                                error("Unable to delete entity: " + ExceptionUtils.getMessage(e1));
                            }
                            reloadTable();
                        }
                    }
                });
            }
        });

        reloadTable();
    }

    final Functions.UnaryVoid<DemoGenericEntity> entitySaver = new Functions.UnaryVoid<DemoGenericEntity>() {
        @Override
        public void call(DemoGenericEntity entity) {
            try {
                getEntityManager().save(entity);
            } catch (SaveException e1) {
                error("Unable to save entity: " + ExceptionUtils.getMessage(e1));
            } catch (UpdateException e1) {
                error("Unable to save entity: " + ExceptionUtils.getMessage(e1));
            }
            reloadTable();
        }
    };

    private void showSelectedEntity(boolean readOnly, final @Nullable Functions.UnaryVoid<DemoGenericEntity> continuation) {
        DemoGenericEntity entity = getSelectedEntity();
        if (entity == null)
            return;
        showEntity(entity, readOnly, continuation);
    }

    private void showEntity(DemoGenericEntity entity, boolean readOnly, final Functions.UnaryVoid<DemoGenericEntity> continuation) {
        final DemoGenericEntityDialog dlg = new DemoGenericEntityDialog(this, entity);
        dlg.setReadOnly(readOnly);
        dlg.pack();
        Utilities.centerOnParent(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    if (continuation != null)
                        continuation.call(dlg.getEntity());
                }
            }
        });
    }

    private DemoGenericEntity getSelectedEntity() {
        return entityTableModel.getRowObject(entityTable.getSelectedRow());
    }

    private void reloadTable() {
        try {
            Collection<DemoGenericEntity> rowCollection = getEntityManager().findAll();
            List<DemoGenericEntity> rowList = new ArrayList<DemoGenericEntity>(rowCollection);
            entityTableModel.setRows(rowList);
        } catch (FindException e) {
            error("Unable to load table: " + ExceptionUtils.getMessage(e));
        }
    }

    private void error(String s) {
        DialogDisplayer.showMessageDialog(this, s, null);
    }

    private static DemoGenericEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(DemoGenericEntityAdmin.class, null);
    }

    private void onCancel() {
        dispose();
    }
}
