package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntityAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 29/11/11
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorsDialog extends JDialog {
    private JPanel mainPanel;
    private SimpleTableModel<ExtensibleSocketConnectorEntity> socketConnectorsTableModel;
    TableRowSorter<SimpleTableModel<ExtensibleSocketConnectorEntity>> rowSorter;
    private JTable socketConnectorsTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;

    public ExtensibleSocketConnectorsDialog(Frame owner) {
        super(owner, "Socket Connectors", true);
        initializeComponents();
        loadListenerConfigurations();
    }

    public ExtensibleSocketConnectorsDialog(Dialog owner) {
        super(owner, "Socket Connectors", true);
        initializeComponents();
        loadListenerConfigurations();
    }

    private void initializeComponents() {
        socketConnectorsTableModel = buildListenersTableModel();
        socketConnectorsTable.setModel(socketConnectorsTableModel);
        socketConnectorsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Utilities.setRowSorter(socketConnectorsTable, socketConnectorsTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<ExtensibleSocketConnectorEntity>>) socketConnectorsTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);

        socketConnectorsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableComponents();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ExtensibleSocketConnectorEntity config = new ExtensibleSocketConnectorEntity();
                ExtensibleSocketConnectorDialog dialog = new ExtensibleSocketConnectorDialog(ExtensibleSocketConnectorsDialog.this, config);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    try {
                        getEntityManager().save(config);
                    } catch (SaveException ex) {
                        JOptionPane.showMessageDialog(ExtensibleSocketConnectorsDialog.this, "Failed to add the new listener.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (UpdateException ex) {
                        JOptionPane.showMessageDialog(ExtensibleSocketConnectorsDialog.this, "Failed to add the new listener.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (Throwable th) {
                        String a = "5";
                    }

                    loadListenerConfigurations();
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ExtensibleSocketConnectorEntity config = socketConnectorsTableModel.getRowObject(rowSorter.convertRowIndexToModel(socketConnectorsTable.getSelectedRow()));
                ExtensibleSocketConnectorDialog dialog = new ExtensibleSocketConnectorDialog(ExtensibleSocketConnectorsDialog.this, config);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    try {
                        getEntityManager().save(config);
                    } catch (SaveException ex) {
                        JOptionPane.showMessageDialog(ExtensibleSocketConnectorsDialog.this, "Failed to update the listener.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (UpdateException ex) {
                        JOptionPane.showMessageDialog(ExtensibleSocketConnectorsDialog.this, "Failed to update the listener.", "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    loadListenerConfigurations();
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] indices = socketConnectorsTable.getSelectedRows();
                ExtensibleSocketConnectorEntity[] configsToDelete = new ExtensibleSocketConnectorEntity[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    configsToDelete[i] = socketConnectorsTableModel.getRowObject(rowSorter.convertRowIndexToModel(indices[i]));
                }

                for (ExtensibleSocketConnectorEntity configToDelete : configsToDelete) {
                    try {
                        getEntityManager().delete(configToDelete);
                        socketConnectorsTableModel.removeRow(configToDelete);
                    } catch (DeleteException ex) {
                        JOptionPane.showMessageDialog(ExtensibleSocketConnectorsDialog.this, "Failed to delete the listener.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (FindException ex) {
                        JOptionPane.showMessageDialog(ExtensibleSocketConnectorsDialog.this, "Failed to delete the listener.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                loadListenerConfigurations();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        setContentPane(mainPanel);
        pack();

        enableDisableComponents();
        Utilities.setDoubleClickAction(socketConnectorsTable, editButton);
    }

    private void enableDisableComponents() {
        final int[] selectedRows = socketConnectorsTable.getSelectedRows();
        editButton.setEnabled(selectedRows.length == 1);
        removeButton.setEnabled(selectedRows.length > 0);
    }

    private SimpleTableModel<ExtensibleSocketConnectorEntity> buildListenersTableModel() {
        return TableUtil.configureTable(
                socketConnectorsTable,
                TableUtil.column("Name", 40, 200, 100000, new Functions.Unary<String, ExtensibleSocketConnectorEntity>() {
                    @Override
                    public String call(final ExtensibleSocketConnectorEntity config) {
                        return config.getName();
                    }
                }, String.class),
                TableUtil.column("Direction", 40, 80, 180, new Functions.Unary<String, ExtensibleSocketConnectorEntity>() {
                    @Override
                    public String call(ExtensibleSocketConnectorEntity config) {
                        return config.isIn() ? "In" : "Out";
                    }
                }, String.class)
        );
    }

    private void loadListenerConfigurations() {
        java.util.List<ExtensibleSocketConnectorEntity> configsList = new ArrayList<ExtensibleSocketConnectorEntity>();
        try {
            configsList.addAll(getEntityManager().findAll());
        } catch (FindException e) {
            // Ignore
        }

        socketConnectorsTableModel.setRows(configsList);
    }

    private static ExtensibleSocketConnectorEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(ExtensibleSocketConnectorEntityAdmin.class, null);
    }
}
