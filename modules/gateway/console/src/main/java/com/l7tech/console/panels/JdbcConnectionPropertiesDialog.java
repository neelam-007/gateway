package com.l7tech.console.panels;

import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
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
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * GUI for creating or editing properties of a JDBC Connection entity.
 *
 * @author ghuang
 */
public class JdbcConnectionPropertiesDialog extends JDialog {
    private static final int MAX_TABLE_COLUMN_NUM = 2;
    private static final int LOWER_BOUND_C3P0_POOL_SIZE = 1;
    private static final int UPPER_BOUND_C3P0_POOL_SIZE = 10000;
    private static final Logger logger = Logger.getLogger(JdbcConnectionPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.JdbcConnectionPropertiesDialog");

    private JPanel mainPanel;
    private SquigglyTextField connectionNameTextField;
    private JComboBox driverClassComboBox;
    private JTextField jdbcUrlTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckBox;
    private JLabel plaintextPasswordWarningLabel;
    private JSpinner minPoolSizeSpinner;
    private JSpinner maxPoolSizeSpinner;

    private JTable additionalPropertiesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private JCheckBox disableConnectionCheckBox;
    private JButton testButton;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel driverClassDescription;
    private SecurityZoneWidget zoneControl;

    private JdbcConnection connection;
    private final Map<String, Object> additionalPropMap = new TreeMap<String,Object>();
    private AbstractTableModel additionalPropertyTableModel;
    private List<String> driverClassWhiteList = new ArrayList<String>();

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
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        Utilities.setMaxLength(connectionNameTextField.getDocument(), 128);
        ((JTextField)driverClassComboBox.getEditor().getEditorComponent()).setDocument(new MaxLengthDocument(256));
        jdbcUrlTextField.setDocument(new MaxLengthDocument(4096));
        usernameTextField.setDocument(new MaxLengthDocument(128));
        passwordField.setDocument(new MaxLengthDocument(64));

        final RunOnChangeListener docListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableButtons();
            }
        });
        ((JTextField)driverClassComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(docListener);
        jdbcUrlTextField.getDocument().addDocumentListener(docListener);
        usernameTextField.getDocument().addDocumentListener(docListener);
        passwordField.getDocument().addDocumentListener(docListener);

        connectionNameTextField.getDocument().addDocumentListener(new RunOnChangeListener() {
            @Override
            public void run() {
                if ( (! ( ExternalAuditStoreConfigWizard.STRICT_CONNECTION_NAME_PATTERN.matcher(connectionNameTextField.getText()).matches()
                        || connectionNameTextField.getText().equals(""))  )  )  {
                    connectionNameTextField.setToolTipText("You may set this name but you will not be able to use the " +
                            "connection in the external audit store configuration.");
                    connectionNameTextField.setAll();

                } else {
                    connectionNameTextField.setToolTipText(null);
                    connectionNameTextField.setNone();
                }
                enableOrDisableButtons();
            }
        });

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title.jdbc.conn.props"));
        // The values in the spinners will be initialized in the method modelToView().
        minPoolSizeSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_C3P0_POOL_SIZE, UPPER_BOUND_C3P0_POOL_SIZE, 1));
        maxPoolSizeSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_C3P0_POOL_SIZE, UPPER_BOUND_C3P0_POOL_SIZE, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(minPoolSizeSpinner, resources.getString("label.minPoolSize")));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxPoolSizeSpinner, resources.getString("label.maxPoolSize")));

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

        initAdditionalPropertyTable();

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

        PasswordGuiUtils.configureOptionalSecurePasswordField(passwordField, showPasswordCheckBox, plaintextPasswordWarningLabel);

        final JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin != null) {
            driverClassWhiteList = admin.getPropertySupportedDriverClass();
        }
        zoneControl.configure(connection);

        modelToView();
        enableOrDisableButtons();
        Utilities.setMinimumSize( this );


    }

    private void modelToView() {
        connectionNameTextField.setText(connection.getName());
        populateDriverClassComboBox();
        jdbcUrlTextField.setText(connection.getJdbcUrl());
        usernameTextField.setText(connection.getUserName());
        passwordField.setText(connection.getPassword());
        minPoolSizeSpinner.setValue(connection.getMinPoolSize());
        maxPoolSizeSpinner.setValue(connection.getMaxPoolSize());
        disableConnectionCheckBox.setSelected(!connection.isEnabled());
        additionalPropMap.clear();
        additionalPropMap.putAll( connection.getAdditionalProperties() );
        additionalPropertyTableModel.fireTableDataChanged();
    }

    private void viewToModel() {
        viewToModel(connection);
    }

    private void viewToModel( final JdbcConnection connection ) {
        connection.setName(connectionNameTextField.getText().trim());
        connection.setDriverClass(((String) driverClassComboBox.getSelectedItem()).trim());
        connection.setJdbcUrl(jdbcUrlTextField.getText().trim());
        connection.setUserName(usernameTextField.getText().trim());
        connection.setPassword(new String(passwordField.getPassword()));
        connection.setMinPoolSize((Integer) minPoolSizeSpinner.getValue());
        connection.setMaxPoolSize((Integer) maxPoolSizeSpinner.getValue());
        connection.setEnabled(!disableConnectionCheckBox.isSelected());
        connection.setAdditionalProperties( additionalPropMap );
        connection.setSecurityZone(zoneControl.getSelectedZone());
    }

    private void populateDriverClassComboBox() {
        java.util.List<String> driverClassList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) {
            return;
        } else {
            driverClassList = admin.getPropertyDefaultDriverClassList();
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

        // Compute the intersection of the two lists.
        driverClassList.retainAll(driverClassWhiteList);

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

    /**
     * Check if OK and Test buttons should be enabled or disabled.
     */
    private void enableOrDisableButtons() {
        final boolean hasUsername = isNonEmptyRequiredTextField(usernameTextField.getText());
        final boolean hasPassword = isNonEmptyRequiredTextField(new String(passwordField.getPassword()));

        boolean enabled =
            isNonEmptyRequiredTextField(connectionNameTextField.getText()) &&
            isNonEmptyRequiredTextField((String) driverClassComboBox.getEditor().getItem()) &&
            isNonEmptyRequiredTextField(jdbcUrlTextField.getText()) &&
            ( !hasPassword || hasUsername ); // username required if password present
        
        okButton.setEnabled(enabled);
        testButton.setEnabled(enabled);

        String customDriverString = resources.getString("custom.driver.description");
        try{
            if(driverClassComboBox.getSelectedItem()!=null){
                String driverClass = (((JTextField)driverClassComboBox.getEditor().getEditorComponent()).getText()).trim();
                String description = resources.getString(driverClass+".description");
                String tooltip = resources.getString(driverClass+".tooltip");
                driverClassDescription.setText(description);
                driverClassDescription.setToolTipText(tooltip);

            }
            else{
                driverClassDescription.setText(customDriverString);
                driverClassDescription.setToolTipText(null);
            }
        } catch(MissingResourceException e){
            driverClassDescription.setText(customDriverString);
            driverClassDescription.setToolTipText(null);
        }

    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private JdbcAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
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
        additionalPropertiesTable.getTableHeader().setReorderingAllowed( false );
        Utilities.setDoubleClickAction(additionalPropertiesTable, editButton);

        enableOrDisableTableButtons();
    }

    public void selectName() {
        connectionNameTextField.requestFocus();
        connectionNameTextField.selectAll();
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
                    String warningMessage = checkDuplicateProperty(property.left, originalPropName);
                    if (warningMessage != null) {
                        DialogDisplayer.showMessageDialog( JdbcConnectionPropertiesDialog.this, warningMessage,
                                resources.getString( "dialog.title.duplicate.property" ), JOptionPane.WARNING_MESSAGE, null );
                        return;
                    }
                    
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

    private String checkDuplicateProperty(String newPropName, final String originalPropName) {
        // Check if there exists a duplicate with Basic Connection Configuration.
        if ("driverClass".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.conn.prop.configured"), resources.getString("property.driver.class"));
        } else if ("jdbcUrl".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.conn.prop.configured"), resources.getString("property.jdbc.url"));
        } else if ("user".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.conn.prop.configured"), resources.getString("property.user.name"));
        } else if ("password".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.conn.prop.configured"), resources.getString("property.password"));
        }

        // Check if there exists a duplicate with C3P0 Pool Configuration.
        if ("minPoolSize".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.c3p0.pool.prop.configured"), resources.getString("property.minPoolSize"));
        } else if ("maxPoolSize".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.c3p0.pool.prop.configured"), resources.getString("property.maxPoolSize"));
        }

        // Check if there exists a duplicate with other properties.
        for (String key: additionalPropMap.keySet()) {
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
            if (currentRow >= 0) additionalPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private void doTest() {
        final JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) return;

        // Assign data to a tested JDBC connection
        final JdbcConnection connForTest = new JdbcConnection();
        viewToModel(connForTest);

        try {
            String result = doAsyncAdmin(
                    admin,
                    JdbcConnectionPropertiesDialog.this,
                    resources.getString("message.testing.progress"),
                    resources.getString("message.testing"),
                    admin.testJdbcConnection(connForTest)).right();

            String message = result.isEmpty() ?
                    resources.getString("message.testing.jdbc.conn.passed") : MessageFormat.format(resources.getString("message.testing.jdbc.conn.failed"), result);

            DialogDisplayer.showMessageDialog(this, message, resources.getString("dialog.title.jdbc.conn.test"),
                    result.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE, null);


        } catch (InterruptedException e) {
            // do nothing, user cancelled

        } catch (InvocationTargetException e) {
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(resources.getString("message.testing.jdbc.conn.failed"), e.getMessage()),
                    resources.getString("dialog.title.jdbc.conn.test"),
                    JOptionPane.WARNING_MESSAGE, null);
        } catch (RuntimeException e) {
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(resources.getString("message.testing.jdbc.conn.failed"), e.getMessage()),
                    resources.getString("dialog.title.jdbc.conn.test"),
                    JOptionPane.WARNING_MESSAGE, null);
        }
    }

    private void doOk() {
        String warningMessage = checkDuplicateJdbcConnection();
        if (warningMessage != null) {
            DialogDisplayer.showMessageDialog( JdbcConnectionPropertiesDialog.this, warningMessage,
                    resources.getString( "dialog.title.error.saving.conn" ), JOptionPane.WARNING_MESSAGE, null);
            return;
        } else if (isMinGreaterThanMax()) {
            DialogDisplayer.showMessageDialog( JdbcConnectionPropertiesDialog.this, resources.getString( "warning.minpoolsize.greaterthan.maxpoolsize" ),
                    resources.getString( "dialog.title.error.saving.conn" ), JOptionPane.WARNING_MESSAGE, null);
            return;
        } else if (!isDriverClassSupported()) {
            DialogDisplayer.showMessageDialog( JdbcConnectionPropertiesDialog.this,
                    MessageFormat.format(resources.getString( "warning.message.invalid.driverClass" ), driverClassComboBox.getSelectedItem()),
                    resources.getString( "dialog.title.error.saving.conn" ), JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        // Assign new attributes to the connect
        viewToModel();

        confirmed = true;
        dispose();
    }

    private void doCancel() {
        dispose();
    }

    private String checkDuplicateJdbcConnection() {
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) return "Cannot get JDBC Connection Admin.  Check the log and try again.";

        String originalConnName = connection.getName();
        String connName = connectionNameTextField.getText();
        if (originalConnName.compareToIgnoreCase(connName) == 0) return null;

        try {
            for (String name: admin.getAllJdbcConnectionNames()) {
                if (connName.compareToIgnoreCase(name) == 0) {
                    return "The connection name \"" + name + "\" already exists. Try a new name.";
                }
            }
        } catch (FindException e) {
            return "Cannot find JDBC connections.  Check the log and Try again.";
        }
        return null;
    }

    private boolean isMinGreaterThanMax() {
        int min = (Integer) minPoolSizeSpinner.getValue();
        int max = (Integer) maxPoolSizeSpinner.getValue();
        return min > max;
    }

    /**
     * Check to see the defined driver class is supported by the Gateway
     * @return True when support, False when not support
     */
    private boolean isDriverClassSupported() {
        final JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin != null) {
            if (driverClassComboBox.getSelectedItem() != null) {
                String driverClass = ((String)driverClassComboBox.getSelectedItem()).trim();
                if (!driverClass.isEmpty()) {
                    return admin.isDriverClassSupported(((String)driverClassComboBox.getSelectedItem()).trim());
                }
            }
            return false;
        }
        return true;
    }
}
