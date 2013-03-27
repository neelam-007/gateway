package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * GUI for managing JDBC Connection entities (Add, Edit, or Remove)
 *
 * @author ghuang
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
    private JButton cloneButton;

    private java.util.List<JdbcConnection> connectionList = new ArrayList<JdbcConnection>();
    private AbstractTableModel connectionTableModel;
    private PermissionFlags flags;

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

        // Initialize GUI components
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscKeyStrokeDisposes(this);

        // Initialize JDBC Connection table
        initJdbcConnectionTable();

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

    /**
     * Reload ConnectionList from the database and resort it as well.
     */
    private void loadJdbcConnectionList() {
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin != null) {
            try {
                connectionList = admin.getAllJdbcConnections();
            } catch (FindException e) {
                logger.warning("Cannot find JDBC Connections.");
            }
            Collections.sort(connectionList);
        }
    }

    private void initJdbcConnectionTable() {
        // Refresh connection list
        loadJdbcConnectionList();

        // Initialize the table model
        connectionTableModel = new JdbcConnectionTableModel();

        // Initialize the table
        connectionTable.setModel(connectionTableModel);
        connectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connectionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });
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
                    return resources.getString("column.label.enabled");          // Column: Enabled
                case 1:
                    return resources.getString("column.label.connection.name");  // Column: Connection Name
                case 2:
                    return resources.getString("column.label.driver.class");     // Column: Driver Class
                case 3:
                    return resources.getString("column.label.jdbc.url");         // Column: JDBC URL
                case 4:
                    return resources.getString("column.label.user.name");        // Column: User Name
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

        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin != null) {
            connection.setMinPoolSize(admin.getPropertyDefaultMinPoolSize());
            connection.setMaxPoolSize(admin.getPropertyDefaultMaxPoolSize());
        }

        boolean addEnableCancelTimeoutProperty = SyspropUtil.getBoolean("com.l7tech.console.panels.JdbcConnectionPropertiesDialog.addEnableCancelTimeoutProperty", true);
        if(addEnableCancelTimeoutProperty) {
            connection.setAdditionalProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("EnableCancelTimeout", "true").map());
        }

        editAndSave(connection,true);
    }

    private void doClone() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow < 0) return;

        JdbcConnection newConnection = new JdbcConnection();
        newConnection.copyFrom( connectionList.get( selectedRow ) );
        EntityUtils.updateCopy( newConnection );
        editAndSave(newConnection, true);
    }

    private void doEdit() {
        int selectedRow = connectionTable.getSelectedRow();
        if (selectedRow < 0) return;

        editAndSave(connectionList.get(selectedRow), false);
    }

    private void editAndSave(final JdbcConnection connection, final boolean selectName) {
        final JdbcConnectionPropertiesDialog dlg = new JdbcConnectionPropertiesDialog(JdbcConnectionManagerWindow.this, connection);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        if(selectName)
            dlg.selectName();
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            loadJdbcConnectionList();
                            editAndSave(connection,selectName);
                        }
                    };

                    // Save the connection
                    JdbcAdmin admin = getJdbcConnectionAdmin();
                    if (admin == null) return;
                    try {
                        admin.saveJdbcConnection(connection);
                    } catch (UpdateException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                        return;
                    }

                    // Refresh the list
                    loadJdbcConnectionList();

                    // Refresh the table
                    connectionTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    int currentRow = 0;
                    for (JdbcConnection conn: connectionList) {
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

            JdbcAdmin admin = getJdbcConnectionAdmin();
            if (admin == null) return;
            try {
                admin.deleteJdbcConnection(connection);
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

    private JdbcAdmin getJdbcConnectionAdmin() {
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
        boolean copyEnabled = selectedRow >= 0;


        addButton.setEnabled(flags.canCreateSome() && addEnabled);
        editButton.setEnabled(editEnabled);  // Not using flags.canUpdateSome(), since we still allow users to view the properties.
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        cloneButton.setEnabled(flags.canCreateSome() && copyEnabled);
    }
}
