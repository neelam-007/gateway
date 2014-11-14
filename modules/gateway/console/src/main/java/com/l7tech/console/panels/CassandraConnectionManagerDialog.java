package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/3/14
 */
public class CassandraConnectionManagerDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(CassandraConnectionManagerDialog.class.getName());

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.CassandraConnectionManagerDialog");

    private JPanel mainPanel;
    private JTable connectionTable;
    private JButton addButton;
    private JButton cloneButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;

    private SimpleTableModel<CassandraConnection> cassandraConnectionsTableModel;
    private TableRowSorter<SimpleTableModel<CassandraConnection>> cassandraConnectionsRawSorter;

    private PermissionFlags flags;

    /**
     * Creates a modeless dialog with the specified {@code Frame}
     * as its owner and an empty title. If {@code owner}
     * is {@code null}, a shared, hidden frame will be set as the
     * owner of the dialog.
     * <p/>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p/>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *                           returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see javax.swing.JComponent#getDefaultLocale
     */
    public CassandraConnectionManagerDialog(Frame owner) {
        super(owner, resources.getString("dialog.title.manage.cassandra.connections"));
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.CASSANDRA_CONFIGURATION);

        // Initialize GUI components
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscKeyStrokeDisposes(this);

        cassandraConnectionsTableModel = buildConnectionTableModel();
        loadCassandraConnections();

        connectionTable.setModel(cassandraConnectionsTableModel);
        connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            protected void run() {
                enableOrDisableButtons();
            }

        };
        connectionTable.getSelectionModel().addListSelectionListener(enableDisableListener);


        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAdd();
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClone();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });


        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setDoubleClickAction(connectionTable, editButton);
        enableOrDisableButtons();

    }

    private CassandraConnectionManagerAdmin getCassandraManagerAdmin() {
        CassandraConnectionManagerAdmin admin = null;
        if (Registry.getDefault().isAdminContextPresent()) {
            admin = Registry.getDefault().getCassandraConnectionAdmin();
        } else {
            logger.log(Level.WARNING, "No Admin Context present!");
        }
        return admin;
    }

    private void loadCassandraConnections() {
        CassandraConnectionManagerAdmin admin = getCassandraManagerAdmin();
        if (admin != null) {
            try {
                // Clear the table first
                final int totalCount = cassandraConnectionsTableModel.getRowCount();
                for (int i = 0; i < totalCount; i++) {
                    cassandraConnectionsTableModel.removeRowAt(0);
                }

                for (CassandraConnection connection : admin.getAllCassandraConnections())
                    cassandraConnectionsTableModel.addRow(connection);
            } catch (FindException e) {
                logger.log(Level.WARNING, resources.getString("errors.manage.cassandra.connections.loadconnections"));
            }
        }
    }

    private void enableOrDisableButtons() {
        int selectedRow = connectionTable.getSelectedRow();

        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;
        boolean copyEnabled = selectedRow >= 0;


        addButton.setEnabled(flags.canCreateSome());
        editButton.setEnabled(editEnabled);  // Not using flags.canUpdateSome(), since we still allow users to view the properties.
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        cloneButton.setEnabled(flags.canCreateSome() && copyEnabled);
    }

    private void doAdd() {
        CassandraConnection connection = new CassandraConnection();
        editAndSave(connection, true);
    }

    private void doEdit() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow < 0) return;

        editAndSave(cassandraConnectionsTableModel.getRowObject(selectedRow), false);
    }

    private void doClone() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow < 0) return;

        CassandraConnection newConnection = new CassandraConnection();
        newConnection.copyFrom(cassandraConnectionsTableModel.getRowObject(selectedRow));
        EntityUtils.updateCopy(newConnection);
        editAndSave(newConnection, true);
    }

    private void doRemove() {
        int currentRow = connectionTable.getSelectedRow();
        if (currentRow < 0) return;

        CassandraConnection connection = cassandraConnectionsTableModel.getRowObject(currentRow);
        Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
        int result = JOptionPane.showOptionDialog(
                this, MessageFormat.format(resources.getString("confirmation.remove.connection"), connection.getName()),
                resources.getString("dialog.title.remove.connection"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            cassandraConnectionsTableModel.removeRowAt(currentRow);

            CassandraConnectionManagerAdmin admin = getCassandraManagerAdmin();
            if (admin == null) return;
            try {
                admin.deleteCassandraConnection(connection);
            } catch (DeleteException e) {
                logger.warning("Cannot delete the Cassandra connection " + connection.getName());
                return;
            }

            // Refresh the list
            //Collections.sort(connectionList);

            // Refresh the table
            cassandraConnectionsTableModel.fireTableDataChanged();

            // Refresh the selection highlight
            if (currentRow == cassandraConnectionsTableModel.getRowCount()) currentRow--; // If the previous deleted row was the last row
            if (currentRow >= 0) connectionTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private void editAndSave(final CassandraConnection connection, final boolean selectName) {
        final CassandraConnectionPropertiesDialog dlg =
                new CassandraConnectionPropertiesDialog(CassandraConnectionManagerDialog.this, connection);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        if (selectName)
            dlg.selectName();
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            loadCassandraConnections();
                            editAndSave(connection, selectName);
                        }
                    };

                    // Save the connection
                    CassandraConnectionManagerAdmin admin = getCassandraManagerAdmin();
                    if (admin == null) return;
                    try {
                        admin.saveCassandraConnection(connection);
                    } catch (UpdateException | SaveException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                        return;
                    }

                    // Refresh the list
                    loadCassandraConnections();

                    // Refresh the table
                    cassandraConnectionsTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    int currentRow = 0;
                    for (int i = 0; i < cassandraConnectionsTableModel.getRowCount(); i++) {
                        CassandraConnection conn = cassandraConnectionsTableModel.getRowObject(i);
                        if (conn.getName().equals(connection.getName())) {
                            break;
                        }
                        currentRow++;
                    }
                    connectionTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        });
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private SimpleTableModel<CassandraConnection> buildConnectionTableModel() {
        return TableUtil.configureTable(
                connectionTable,
                TableUtil.column("Active", 40, 100, 100, new Functions.Unary<Boolean, CassandraConnection>() {
                    @Override
                    public Boolean call(CassandraConnection cassandraConnection) {
                        return new Boolean(cassandraConnection.isEnabled());
                    }
                }, Boolean.class),
                TableUtil.column("Name", 40, 200, 1000000, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getName();
                    }
                }, String.class),
                TableUtil.column("Contact Points", 40, 150, 180, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getContactPoints();
                    }
                }, String.class),
                TableUtil.column("Keyspace", 40, 100, 180, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getKeyspaceName();
                    }
                }, String.class),
                TableUtil.column("Port", 40, 100, 180, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getPort();
                    }
                }, String.class)
        );
    }
}
