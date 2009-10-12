package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnectionAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.MutablePair;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.text.MessageFormat;
import java.io.IOException;

/**
 * @author ghuang
 */
public class JdbcConnectionPropertiesDialog extends JDialog {
    private static final int MAX_TABLE_COLUMN_NUM = 2;
    private static final int LOWER_BOUND_C3P0_POOL_SIZE = 1;
    private static final int UPPER_BOUND_C3P0_POOL_SIZE = 10000;
    private static final Logger logger = Logger.getLogger(JdbcConnectionPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.JdbcConnectionPropertiesDialog");

    private JPanel mainPanel;
    private JTextField connectionNameTextField;
    private JComboBox driverClassComboBox;
    private JTextField jdbcUrlTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JSpinner minPoolSizeSpinner;//todo: check boundaries for min and max
    private JSpinner maxPoolSizeSpinner;

    private JTable additionalPropertiesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private JCheckBox disableConnectionCheckBox;
    private JButton testButton;
    private JButton okButton;
    private JButton cancelButton;

    private JdbcConnection connection;
    private Map<String, Object> additionalPropMap;
    private AbstractTableModel additionalPropertyTableModel;

    private boolean confirmed;
    private PermissionFlags flags;

    public JdbcConnectionPropertiesDialog(Frame owner, JdbcConnection connection) {
        super(owner, resources.getString("dialog.title.jdbc.conn.props"));
        initialize(connection);
    }

    public JdbcConnectionPropertiesDialog(Dialog owner, JdbcConnection connection) {
        super(owner, resources.getString("dialog.title.jdbc.conn.props"));
        initialize(connection);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initialize(JdbcConnection connection) {
        flags = PermissionFlags.get(EntityType.JDBC_CONNECTION);
        this.connection = connection;

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(cancelButton);
        Utilities.setEscKeyStrokeDisposes(this);

        final DocumentListener docListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });
        connectionNameTextField.getDocument().addDocumentListener(docListener);
        ((JTextField)driverClassComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(docListener);
        jdbcUrlTextField.getDocument().addDocumentListener(docListener);
        usernameTextField.getDocument().addDocumentListener(docListener);
        passwordField.getDocument().addDocumentListener(docListener);

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title.jdbc.conn.props"));
        // The values in the spinners will be initialized in the method modelToView().
        minPoolSizeSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_C3P0_POOL_SIZE, UPPER_BOUND_C3P0_POOL_SIZE, 1));
        maxPoolSizeSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_C3P0_POOL_SIZE, UPPER_BOUND_C3P0_POOL_SIZE, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(minPoolSizeSpinner, resources.getString("label.minPoolSize")));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxPoolSizeSpinner, resources.getString("label.maxPoolSize")));

        initAdditionalPropertyTable();

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

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doTest();
            }
        });

        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        modelToView();
        enableOrDisableOkButton();
    }

    private void modelToView() {
        connectionNameTextField.setText(connection.getName());
        loadDriverClassComboBox();
        jdbcUrlTextField.setText(connection.getJdbcUrl());
        usernameTextField.setText(connection.getUserName());
        passwordField.setText(connection.getPassword());
        minPoolSizeSpinner.setValue(connection.getMinPoolSize());
        maxPoolSizeSpinner.setValue(connection.getMaxPoolSize());
        disableConnectionCheckBox.setSelected(!connection.isEnabled());
    }

    private void viewToModel() {
        connection.setName(connectionNameTextField.getText().trim());
        connection.setDriverClass(((String) driverClassComboBox.getSelectedItem()).trim());
        connection.setJdbcUrl(jdbcUrlTextField.getText().trim());
        connection.setUserName(usernameTextField.getText().trim());
        connection.setPassword(passwordField.getText()); //todo: passwordField.getPassword()
        connection.setMinPoolSize((Integer) minPoolSizeSpinner.getValue());
        connection.setMaxPoolSize((Integer) maxPoolSizeSpinner.getValue());
        connection.setEnabled(!disableConnectionCheckBox.isSelected());
        try {
            connection.recreateSerializedProps();
        } catch (IOException e) {
            logger.warning("Cannot recreate additional properties XML.");
        }
    }

    private void loadDriverClassComboBox() {
        java.util.List<String> driverClassList;
        JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
        if (connectionAdmin == null) {
            return;
        } else {
            driverClassList = connectionAdmin.getPropertyDefaultDriverClassList();
        }

        // Get the driver class of the current connection
        String currentDriverClass = connection.getDriverClass();
        if (currentDriverClass == null) {
            currentDriverClass = "";
        } else {
            currentDriverClass = currentDriverClass.trim();
        }

        // Add the current driver class into the driver class list
        if (!currentDriverClass.isEmpty() && !driverClassList.contains(currentDriverClass)) {
            driverClassList.add(currentDriverClass);
        }

        // Sort the driver class list
        Collections.sort(driverClassList);

        // Add an empty driver class at the first position of the driver class list
        driverClassList.add(0, "");

        // Add all items into the combox box.
        driverClassComboBox.removeAllItems();
        for (String driverClass: driverClassList) {
            driverClassComboBox.addItem(driverClass);
        }

        // Set the current driver class as selected.
        driverClassComboBox.setSelectedItem(currentDriverClass);
    }

    private void enableOrDisableOkButton() {
        boolean enabled =
            isNonEmptyRequiredTextField(connectionNameTextField.getText()) &&
            isNonEmptyRequiredTextField((String) driverClassComboBox.getEditor().getItem()) &&
            isNonEmptyRequiredTextField(jdbcUrlTextField.getText()) &&
            isNonEmptyRequiredTextField(usernameTextField.getText()) &&
            isNonEmptyRequiredTextField(passwordField.getText());
        
        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private JdbcConnectionAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }

    private void initAdditionalPropertyTable() {
        if (connection == null)
            throw new IllegalStateException("A JDBC connection must be initialized first before additional properties are loaded.");
        else
            additionalPropMap = connection.getAllAddtionalProperties();
        
        additionalPropertyTableModel = new AdditionalPropertyTableModel();
        additionalPropertiesTable.setModel(additionalPropertyTableModel);
        additionalPropertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        additionalPropertiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableTableButtons();
            }
        });
        Utilities.setDoubleClickAction(additionalPropertiesTable, editButton);

        enableOrDisableTableButtons();
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
        editAndSave(new MutablePair<String, String>("", ""));
    }

    private void doEdit() {
        int selectedRow = additionalPropertiesTable.getSelectedRow();
        if (selectedRow < 0) return;

        String propName = (String) additionalPropMap.keySet().toArray()[selectedRow];
        String propValue = (String)additionalPropMap.get(propName);

        editAndSave(new MutablePair<String, String>(propName, propValue));
    }

    private void editAndSave(final MutablePair<String, String> property) {
        if (property == null || property.left == null || property.right == null) return;
        final String originalPropName = property.left;

        final AdditionalPropertiesDialog dlg = new AdditionalPropertiesDialog(this, property);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    // Save the property into the map
                    if (! originalPropName.isEmpty()) { // This is for doEdit
                        additionalPropMap.remove(originalPropName);
                    }
                    additionalPropMap.put(property.left, property.right);
                    // Refresh the table
                    additionalPropertyTableModel.fireTableDataChanged();
                    // Refresh the selection highlight
                    ArrayList<String> keyset = new ArrayList<String>();
                    keyset.addAll(additionalPropMap.keySet());
                    int currentRow = keyset.indexOf(property.left);
                    additionalPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        });
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
            if (currentRow >= 0) additionalPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private void doTest() {
        JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
        if (connectionAdmin == null) return;

        viewToModel();

        // todo: add detail for failure
        String message = connectionAdmin.testConnection(connection)?
            "JDBC connection testing passed" : "JDBC connection testing failed with detail here";
        DialogDisplayer.showMessageDialog(this, message, null);
    }

    private void doOk() {
        confirmed = true;
        viewToModel();
        dispose();
    }

    private void doCancel() {
        dispose();
    }
}
