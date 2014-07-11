package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntityAdmin;
import com.l7tech.external.assertions.xmppassertion.XMPPConstants;
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

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 07/03/12
 * Time: 1:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class ManageXMPPConnectionEntitiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ManageXMPPConnectionEntitiesDialog.class.getName());

    private JPanel mainPanel;
    private JTable xmppConnectionsTable;
    private SimpleTableModel<XMPPConnectionEntity> xmppConnectionsTableModel;
    TableRowSorter<SimpleTableModel<XMPPConnectionEntity>> rowSorter;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;

    public ManageXMPPConnectionEntitiesDialog(Frame parent) {
        super(parent, "Manage XMPP Connections", true);
        initializeComponents();
        loadXMPPConnectionsConfigurations();
    }

    private void initializeComponents() {
        xmppConnectionsTableModel = buildXMPPConnectionsTableModel();
        xmppConnectionsTable.setModel(xmppConnectionsTableModel);
        xmppConnectionsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Utilities.setRowSorter(xmppConnectionsTable, xmppConnectionsTableModel, new int[]{1}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<XMPPConnectionEntity>>) xmppConnectionsTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);

        xmppConnectionsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableComponents();
            }
        });

        xmppConnectionsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    rowSelected();
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final XMPPConnectionEntity config = new XMPPConnectionEntity();
                final XMPPConnectionEntityDialog dialog = new XMPPConnectionEntityDialog(ManageXMPPConnectionEntitiesDialog.this, xmppConnectionsTableModel.getRows(), config);
                Utilities.centerOnParentWindow(dialog);

                DialogDisplayer.display(dialog, new Runnable() {
                    public void run() {
                        if(dialog.isConfirmed()) {
                            try {
                                dialog.updateModel(config);
                                getEntityManager().save(config);
                            } catch(SaveException ex) {
                                JOptionPane.showMessageDialog(ManageXMPPConnectionEntitiesDialog.this, "Failed to add the new XMPP connection.", "Error", JOptionPane.ERROR_MESSAGE);
                            } catch(UpdateException ex) {
                                JOptionPane.showMessageDialog(ManageXMPPConnectionEntitiesDialog.this, "Failed to add the new XMPP connection.", "Error", JOptionPane.ERROR_MESSAGE);
                            }

                            loadXMPPConnectionsConfigurations();
                        }
                    }
                });
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rowSelected();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] indices = xmppConnectionsTable.getSelectedRows();
                XMPPConnectionEntity[] configsToDelete = new XMPPConnectionEntity[indices.length];
                for(int i = 0;i < indices.length;i++) {
                    configsToDelete[i] = xmppConnectionsTableModel.getRowObject(rowSorter.convertRowIndexToModel(indices[i]));
                }

                // @bug 12901: Confirm the removal of selected XMPP configs.
                // Display a panel confirming the removal of the (indicies.length) configs...
                String dialogMessage = "Are you sure you wish to remove the" + (indices.length > 1 ? " " + indices.length + " " : " ") + "selected XMPP connection" + (indices.length > 1 ? "s" : "" ) + "?";
                Object[] options = { "OK", "Cancel" }; // Needed to make CANCEL default.
                if (JOptionPane.showOptionDialog(mainPanel, dialogMessage, "Confirm Connection Removal", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]) == 0) {

                    for(XMPPConnectionEntity configToDelete : configsToDelete) {
                        xmppConnectionsTableModel.removeRow(configToDelete);
                    }

                    for(XMPPConnectionEntity entity : configsToDelete) {
                        try {
                            getEntityManager().delete(entity);
                        } catch(DeleteException ex) {
                            JOptionPane.showMessageDialog(ManageXMPPConnectionEntitiesDialog.this, "Failed to delete the XMPP connection (" + entity.getName() + ").", "Error", JOptionPane.ERROR_MESSAGE);
                        } catch(FindException ex) {
                            JOptionPane.showMessageDialog(ManageXMPPConnectionEntitiesDialog.this, "Failed to delete the XMPP connection (" + entity.getName() + ").", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    loadXMPPConnectionsConfigurations();
                }


            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setContentPane(mainPanel);
        pack();

        Utilities.setEscKeyStrokeDisposes(this);

        enableDisableComponents();
    }

    private void rowSelected() {
        final XMPPConnectionEntity config = xmppConnectionsTableModel.getRowObject(rowSorter.convertRowIndexToModel(xmppConnectionsTable.getSelectedRow()));
        final XMPPConnectionEntityDialog dialog = new XMPPConnectionEntityDialog(ManageXMPPConnectionEntitiesDialog.this, xmppConnectionsTableModel.getRows(), config);
        Utilities.centerOnParentWindow(dialog);

        DialogDisplayer.display(dialog, new Runnable() {
            public void run() {
                if(dialog.isConfirmed()) {
                    try {
                        dialog.updateModel(config);
                        getEntityManager().save(config);
                    } catch(SaveException ex) {
                        JOptionPane.showMessageDialog(ManageXMPPConnectionEntitiesDialog.this, "Failed to update the XMPP connection.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch(UpdateException ex) {
                        JOptionPane.showMessageDialog(ManageXMPPConnectionEntitiesDialog.this, "Failed to add the XMPP connection.", "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    loadXMPPConnectionsConfigurations();
                }
            }
        });
    }

    private void enableDisableComponents() {
        final int[] selectedRows = xmppConnectionsTable.getSelectedRows();
        editButton.setEnabled(selectedRows.length == 1);
        removeButton.setEnabled(selectedRows.length > 0);
    }

    private SimpleTableModel<XMPPConnectionEntity> buildXMPPConnectionsTableModel() {
        return TableUtil.configureTable(
                xmppConnectionsTable,
                TableUtil.column("Enabled", 40, 80, 120, new Functions.Unary<String, XMPPConnectionEntity>() {
                    @Override
                    public String call(final XMPPConnectionEntity config) {
                        if (config.isInbound()) {
                            return (config.isEnabled() ? "Yes" : "No");
                        } else {
                            return "Yes";
                        }
                    }
                }, String.class),
                TableUtil.column("Name", 40, 200, 100000, new Functions.Unary<String, XMPPConnectionEntity>() {
                    @Override
                    public String call(final XMPPConnectionEntity config) {
                        return config.getName();
                    }
                }, String.class),
                TableUtil.column("Direction", 40, 80, 180, new Functions.Unary<String, XMPPConnectionEntity>() {
                    @Override
                    public String call(XMPPConnectionEntity config) {
                        return config.isInbound() ? "In" : "Out";
                    }
                }, String.class)
        );
    }

    private void loadXMPPConnectionsConfigurations() {
        try {
            Collection<XMPPConnectionEntity> rowCollection = getEntityManager().findAll();
            java.util.List<XMPPConnectionEntity> rowList = new ArrayList<XMPPConnectionEntity>(rowCollection);
            xmppConnectionsTableModel.setRows(rowList);
        } catch (FindException e) {
            logger.log(Level.WARNING, XMPPConstants.TABLE_LOAD_ERROR);
            error(XMPPConstants.TABLE_LOAD_ERROR + ExceptionUtils.getMessage(e));
        }
    }

    private void error(String s) {
        DialogDisplayer.showMessageDialog(this, s, null);
    }

    private static XMPPConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(XMPPConnectionEntityAdmin.class, null);
    }
}
