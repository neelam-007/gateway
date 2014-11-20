package com.l7tech.console.panels;

import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.MutablePair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.policy.variable.Syntax.getReferencedNames;
import static com.l7tech.util.ValidationUtils.isValidInteger;

/**
 * Created by yuri on 11/4/14.
 */
public class CassandraConnectionPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(CassandraConnectionPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.CassandraConnectionPropertiesDialog");
    private static final int MAX_TABLE_COLUMN_NUM = 2;

    private JPanel mainPanel;
    private JTextField nameTextField;
    private JTextField contactPointsTextField;
    private JTextField portTextField;
    private JTextField keyspaceTextField;
    private JButton manageStoredPasswordsButton;
    private SecurePasswordComboBox securePasswordComboBox;
    private JComboBox compressionComboBox;
    private JCheckBox useSSLCheckBox;
    private JTable additionalPropertiesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JCheckBox disableConfigurationCheckBox;
    private JButton testConnectionButton;
    private JButton cancelButton;
    private JButton OKButton;
    private JTextField credentialsTextField;
    private SecurityZoneWidget zoneControl;

    private final Map<String, String> additionalPropMap = new TreeMap<>();
    private AbstractTableModel additionalPropertyTableModel;
    private InputValidator validator;
    private final CassandraConnection connection;
    private PermissionFlags flags;
    private boolean confirmed;

    public CassandraConnectionPropertiesDialog(Dialog owner, CassandraConnection connection) {
        super(owner, resources.getString("dialog.title.manage.cassandra.connection.properties"), true);
        this.connection = connection;
        initialize();
    }

    @SuppressWarnings("unchecked")
    private void initialize() {
        flags = PermissionFlags.get(EntityType.CASSANDRA_CONFIGURATION);
        setContentPane(mainPanel);
        validator = new InputValidator(this, this.getTitle());
        Utilities.setEscKeyStrokeDisposes(this);

        nameTextField.setDocument(new MaxLengthDocument(128));
        keyspaceTextField.setDocument(new MaxLengthDocument(255));
        contactPointsTextField.setDocument(new MaxLengthDocument(4096));
        portTextField.setDocument(new MaxLengthDocument(255));
        credentialsTextField.setDocument(new MaxLengthDocument(255));
        validator.constrainTextFieldToBeNonEmpty("Connection Name", nameTextField, null);
        validator.constrainTextFieldToBeNonEmpty("Contact Points", contactPointsTextField, null);
        validator.constrainTextFieldToBeNonEmpty("Port", portTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String errMsg = null;
                final String port = portTextField.getText().trim();
                if ( !isValidInteger( port, true, 0, Integer.MAX_VALUE ) && getReferencedNames( port ).length == 0  ) {
                    errMsg = "The value for the port must be a valid positive number.";
                }
                return errMsg;
            }
        });

        securePasswordComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());
        manageStoredPasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManagePasswords();
            }
        });

        validator.attachToButton(OKButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

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

        testConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doTest();
            }
        });

        initAdditionalPropertyTable();

        compressionComboBox.setModel(new DefaultComboBoxModel(new String[]{
                CassandraConnection.COMPRESS_LZ4,
                CassandraConnection.COMPRESS_NONE,
                CassandraConnection.COMPRESS_SNAPPY
        }));

        zoneControl.configure(connection);
        modelToView();
    }

    private void initAdditionalPropertyTable() {
        additionalPropertyTableModel = new AdditionalPropertyTableModel();
        additionalPropertiesTable.setModel(additionalPropertyTableModel);
        additionalPropertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        additionalPropertiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableTableButtons();
            }
        });
        additionalPropertiesTable.getTableHeader().setReorderingAllowed(false);
        Utilities.setDoubleClickAction(additionalPropertiesTable, editButton);

        enableOrDisableTableButtons();
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

    private void onOk() {
        String warningMessage = checkDuplicateCassandraConnection();
        if (warningMessage != null) {
            DialogDisplayer.showMessageDialog(CassandraConnectionPropertiesDialog.this, warningMessage,
                    resources.getString("dialog.title.error.saving.conn"), JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        // Assign new attributes to the connect
        viewToModel(this.connection);

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void selectName() {
        nameTextField.requestFocus();
        nameTextField.selectAll();
    }

    private void modelToView() {
        nameTextField.setText(connection.getName());
        keyspaceTextField.setText(connection.getKeyspaceName());
        contactPointsTextField.setText(connection.getContactPoints());
        portTextField.setText(connection.getPort());
        credentialsTextField.setText(connection.getUsername());
        if (connection.getPasswordGoid() != null) {
            securePasswordComboBox.setSelectedSecurePassword(connection.getPasswordGoid());
        } else {
            securePasswordComboBox.setSelectedIndex(-1);
        }
        compressionComboBox.setSelectedItem(connection.getCompression());
        useSSLCheckBox.setSelected(connection.isSsl());
        disableConfigurationCheckBox.setSelected(!connection.isEnabled());
        additionalPropMap.clear();
        additionalPropMap.putAll(connection.getProperties());
        additionalPropertyTableModel.fireTableDataChanged();
    }

    private void viewToModel(CassandraConnection connection) {
        connection.setName(nameTextField.getText().trim());
        connection.setKeyspaceName(keyspaceTextField.getText().trim());
        connection.setContactPoints(contactPointsTextField.getText().trim());
        connection.setPort(portTextField.getText().trim());
        connection.setUsername(credentialsTextField.getText().trim());
        if (securePasswordComboBox.getSelectedSecurePassword() != null) {
            connection.setPasswordGoid(securePasswordComboBox.getSelectedSecurePassword().getGoid());
        } else {
            connection.setPasswordGoid(null);
        }
        connection.setCompression(((String) compressionComboBox.getSelectedItem()));
        connection.setSsl(useSSLCheckBox.isSelected());
        connection.setEnabled(!disableConfigurationCheckBox.isSelected());
        connection.setProperties(additionalPropMap);
        connection.setSecurityZone(zoneControl.getSelectedZone());
    }

    private void doManagePasswords() {
        final SecurePassword password = securePasswordComboBox.getSelectedSecurePassword();
        final SecurePasswordManagerWindow securePasswordManagerWindow = new SecurePasswordManagerWindow(getOwner());

        securePasswordManagerWindow.pack();
        Utilities.centerOnParentWindow(securePasswordManagerWindow);
        DialogDisplayer.display(securePasswordManagerWindow, new Runnable() {
            @Override
            public void run() {
                securePasswordComboBox.reloadPasswordList();
                enableDisableComponents();
                DialogDisplayer.pack(CassandraConnectionPropertiesDialog.this);

            }
        });
    }

    private void enableDisableComponents() {
    }

    private void onCancel() {
        this.dispose();
    }

    private void doTest() {
        CassandraConnectionManagerAdmin admin = getCassandraManagerAdmin();
        if (admin == null) return;

        final CassandraConnection connForTest = new CassandraConnection();
        // Test with current values from the UI
        viewToModel(connForTest);

        try {
            String result = AdminGuiUtils.doAsyncAdmin(
                    admin,
                    CassandraConnectionPropertiesDialog.this,
                    resources.getString("message.testing.progress"),
                    resources.getString("message.testing"),
                    admin.testCassandraConnection(connForTest)).right();

            String message = result.isEmpty() ? resources.getString("message.testing.cassandra.conn.passed") :
                    MessageFormat.format(resources.getString("message.testing.cassandra.conn.failed"), result);

            DialogDisplayer.showMessageDialog(this, message, resources.getString("dialog.title.cassandra.conn.test"),
                    result.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE, null);

        } catch (InterruptedException e) {
            // do nothing, user cancelled
        } catch (InvocationTargetException | RuntimeException e) {
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(resources.getString("message.testing.cassandra.conn.failed"),
                    e.getMessage()), resources.getString("dialog.title.cassandra.conn.test"), JOptionPane.WARNING_MESSAGE, null);
        }
    }

    private class AdditionalPropertyTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public void fireTableDataChanged() {
            super.fireTableDataChanged();
            enableOrDisableTableButtons();
        }

        @Override
        public int getRowCount() {
            return additionalPropMap.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return resources.getString("column.label.property.name");
                case 1:
                    return resources.getString("column.label.property.value");
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
            String name = (String) additionalPropMap.keySet().toArray()[row];

            switch (col) {
                case 0:
                    return name;
                case 1:
                    return additionalPropMap.get(name);
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }
    }

    private void enableOrDisableTableButtons() {
        int selectedRow = additionalPropertiesTable.getSelectedRow();

        boolean addEnabled = true;
        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;

        addButton.setEnabled(flags.canCreateSome() && addEnabled);
        editButton.setEnabled(editEnabled);
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
    }

    private void doAdd() {
        editAndSave(new MutablePair<>("", ""));
    }

    private void doEdit() {
        int selectedRow = additionalPropertiesTable.getSelectedRow();
        if (selectedRow < 0) return;

        String propName = (String) additionalPropMap.keySet().toArray()[selectedRow];
        String propValue = additionalPropMap.get(propName);

        editAndSave(new MutablePair<>(propName, propValue));
    }

    private void editAndSave(final MutablePair<String, String> property) {
        if (property == null || property.left == null || property.right == null) return;
        final String originalPropName = property.left;

        final CassandraPropertiesDialog dlg = new CassandraPropertiesDialog(this, property);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    String warningMessage = checkDuplicateProperty(property.left, originalPropName);
                    if (warningMessage != null) {
                        DialogDisplayer.showMessageDialog(CassandraConnectionPropertiesDialog.this, warningMessage,
                                resources.getString("dialog.title.duplicate.property"), JOptionPane.WARNING_MESSAGE, null);
                        return;
                    }

                    // Save the property into the map
                    if (!originalPropName.isEmpty()) { // This is for doEdit
                        additionalPropMap.remove(originalPropName);
                    }
                    additionalPropMap.put(property.left, property.right);

                    // Refresh the table
                    additionalPropertyTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    ArrayList<String> keyset = new ArrayList<>();
                    keyset.addAll(additionalPropMap.keySet());
                    int currentRow = keyset.indexOf(property.left);
                    additionalPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        });
    }

    private String checkDuplicateProperty(String newPropName, final String originalPropName) {
        //TODO: Add connection options checks

        // Check if there exists a duplicate with other properties.
        for (String key : additionalPropMap.keySet()) {
            if (originalPropName.compareToIgnoreCase(key) != 0 // make sure not to compare itself
                    && newPropName.compareToIgnoreCase(key) == 0) {
                return MessageFormat.format(resources.getString("warning.message.duplicated.property"), newPropName);
            }
        }

        return null;
    }

    private void doRemove() {
        int currentRow = additionalPropertiesTable.getSelectedRow();
        if (currentRow < 0) return;

        String propName = (String) additionalPropMap.keySet().toArray()[currentRow];
        Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
        int result = JOptionPane.showOptionDialog(
                this, MessageFormat.format(resources.getString("confirmation.remove.additional.property"), propName),
                resources.getString("dialog.title.remove.additional.property"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            // Refresh the list
            additionalPropMap.remove(propName);
            // Refresh the table
            additionalPropertyTableModel.fireTableDataChanged();
            // Refresh the selection highlight
            if (currentRow == additionalPropMap.size()) currentRow--; // If the previous deleted row was the last row
            if (currentRow >= 0)
                additionalPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private String checkDuplicateCassandraConnection() {
        CassandraConnectionManagerAdmin admin = getCassandraManagerAdmin();
        if (admin == null) return "Cannot get Cassandra Connection Admin.  Check the log and try again.";

        String originalConnName = connection.getName();
        String connName = nameTextField.getText();
        if (originalConnName.compareToIgnoreCase(connName) == 0) return null;

        try {
            for (String name : admin.getAllCassandraConnectionNames()) {
                if (connName.compareToIgnoreCase(name) == 0) {
                    return "The connection name \"" + name + "\" already exists. Try a new name.";
                }
            }
        } catch (FindException e) {
            return "Cannot find Cassandra connections.  Check the log and Try again.";
        }
        return null;
    }
}
