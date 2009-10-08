package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnectionAdmin;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * @author: ghuang
 */
public class JdbcConnectionManagerWindow extends JDialog {
    private static final int MAX_TABLE_COLUMN_NUM = 5;
    private static final Logger logger = Logger.getLogger(JdbcConnectionManagerWindow.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.JdbcConnectionManagerWindow");

    private JPanel mainPanel;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;
    private JTable connectionTable;

    private java.util.List<JdbcConnection> connectionList = new ArrayList<JdbcConnection>();
    private AbstractTableModel connectionTableModel;
    private PermissionFlags flags; // todo: check flags usage in SsgConnectorManagerWindow Line 341

    public JdbcConnectionManagerWindow(Frame owner) {
        super(owner, resources.getString("dialog.title.manage.jdbc.connections"));
        initialize();
    }

    public JdbcConnectionManagerWindow(Dialog owner) {
        super(owner, resources.getString("dialog.title.manage.jdbc.connections"));
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.JDBC_CONNECTION);

        // Initialize JDBC Connection List
        initJdbcConnectionList();

        // Initialize GUI components
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscKeyStrokeDisposes(this);

        initJdbcConnectionTable();

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAdd();
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

        enableOrDisableButtons();
    }

    private void initJdbcConnectionList() {
        JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
        if (connectionAdmin != null) {
            try {
                connectionList = connectionAdmin.getAllJdbcConnections();
            } catch (FindException e) {
                logger.warning("Cannot find JDBC Connections.");
            }
            Collections.sort(connectionList);
        }
    }

    private void initJdbcConnectionTable() {
        connectionTableModel = new JdbcConnectionTableModel();

        connectionTable.setModel(connectionTableModel);
        connectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connectionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });
        Utilities.setDoubleClickAction(connectionTable, editButton);
    }

    private class JdbcConnectionTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public void fireTableDataChanged() {
            super.fireTableDataChanged();
            enableOrDisableButtons();
        }

        @Override
        public int getRowCount() {
            return connectionList.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return resources.getString("column.label.enabled");
                case 1:
                    return resources.getString("column.label.connection.name");
                case 2:
                    return resources.getString("column.label.driver.class");
                case 3:
                    return resources.getString("column.label.jdbc.url");
                case 4:
                    return resources.getString("column.label.user.name");
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int col) {
            JdbcConnection connection = connectionList.get(row);

            switch (col) {
                case 0:
                    return connection.isEnabled()?"Yes":"No";
                case 1:
                    return connection.getName();
                case 2:
                    return connection.getDriverClass();
                case 3:
                    return connection.getJdbcUrl();
                case 4:
                    return connection.getUserName();
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }
    }

    private void doAdd() {
        JdbcConnection connection = new JdbcConnection();

        JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
        if (connectionAdmin != null) {
            connection.setMinPoolSize(connectionAdmin.getPropertyDefaultMinPoolSize());
            connection.setMaxPoolSize(connectionAdmin.getPropertyDefaultMaxPoolSize());
        }

        editAndSave(-1, connection); // -1 means this is a new connection.
    }

    private void doEdit() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow < 0) return;

        JdbcConnection connection = connectionList.get(selectedRow);
        editAndSave(selectedRow, connection);
    }

    private void editAndSave(final int row, final JdbcConnection connection) {
        final JdbcConnectionPropertiesDialog dlg = new JdbcConnectionPropertiesDialog(JdbcConnectionManagerWindow.this, connection);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    // Save the connection
                    JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
                    if (connectionAdmin == null) return;
                    try {
                        connectionAdmin.saveJdbcConnection(connection);
                    } catch (UpdateException e) {
                        logger.warning("Cannot save a JDBC connection, " + connection.getName());
                    }

                    // Refresh the list
                    if (row >= 0) connectionList.remove(row);
                    connectionList.add(connection);
                    Collections.sort(connectionList);

                    // Refresh the table
                    connectionTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    int currentRow = connectionList.indexOf(connection);
                    connectionTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        });
    }

    private void doRemove() {
        int currentRow = connectionTable.getSelectedRow();
        if (currentRow < 0) return;

        JdbcConnection connection = connectionList.get(currentRow);
        Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
        int result = JOptionPane.showOptionDialog(
                    this, MessageFormat.format(resources.getString("confirmation.remove.connection"), connection.getName()),
                    resources.getString("dialog.title.remove.connection"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            connectionList.remove(currentRow);

            JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
            if (connectionAdmin == null) return;
            try {
                connectionAdmin.deleteJdbcConnection(connection);
            } catch (DeleteException e) {
                logger.warning("Cannot delete the JDBC connection " + connection.getName());
                return;
            }

            // Refresh the list
            Collections.sort(connectionList);

            // Refresh the table
            connectionTableModel.fireTableDataChanged();

            // Refresh the selection highlight
            if (currentRow == connectionList.size()) currentRow--; // If the previous deleted row was the last row
            if (currentRow >= 0) connectionTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private JdbcConnectionAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getJdbcConnectionAdmin();
    }

    private void enableOrDisableButtons() {
        int selectedRow = connectionTable.getSelectedRow();

        boolean addEnabled = true;
        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;

        addButton.setEnabled(flags.canCreateSome() && addEnabled);
        editButton.setEnabled(editEnabled);
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
    }
}
