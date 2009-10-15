package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.MutablePair;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnectionAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Properties dialog for the JDBC Query Assertion.
 */
public class JdbcQueryAssertionDialog extends AssertionPropertiesEditorSupport<JdbcQueryAssertion> {
    private static final int MAX_TABLE_COLUMN_NUM = 2;
    private static final int LOWER_BOUND_MAX_RECORDS = 1;
    private static final int UPPER_BOUND_MAX_RECORDS = 10000;
    private static final Logger logger = Logger.getLogger(JdbcQueryAssertionDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.jdbcquery.console.resources.JdbcQueryAssertionDialog");

    private JPanel mainPanel;
    private JComboBox connectionComboBox;
    private JTextArea sqlQueryTextArea;
    private JTable namingTable;
    private JTextField variablePrefixTextField;
    private JButton testButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton cancelButton;
    private JButton okButton;
    private JCheckBox failAssertionCheckBox;
    private JSpinner maxRecordsSpinner;

    private NamingTableModel namingTableModel;
    private Map<String, String> namingMap;
    private JdbcQueryAssertion assertion;
    private boolean confirmed;

    public JdbcQueryAssertionDialog(Window owner, JdbcQueryAssertion assertion) {
        super(owner, assertion);
        setData(assertion);
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(JdbcQueryAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public JdbcQueryAssertion getData(final JdbcQueryAssertion assertion) {
        viewToModel(assertion);
        return assertion;
    }

    @Override
    protected void configureView() {
        enableOrDisableOkButton();
        enableOrDisableTableButtons();
    }

    private void initialize() {
        //setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
        setModal(true);
        //setMinimumSize(new Dimension(600, 400));
        getRootPane().setDefaultButton(cancelButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        final EventListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        });
        connectionComboBox.addItemListener((ItemListener)changeListener);
        sqlQueryTextArea.getDocument().addDocumentListener((DocumentListener)changeListener);
        variablePrefixTextField.getDocument().addDocumentListener((DocumentListener)changeListener);

        initNamingTable();

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title.jdbc.query.props"));
        // The values in the spinners will be initialized in the method modelToView().
        maxRecordsSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_MAX_RECORDS, UPPER_BOUND_MAX_RECORDS, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxRecordsSpinner, resources.getString("short.lable.max.records")));

        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTest();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAdd();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doEdit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });

        inputValidator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        modelToView();
        configureView();
    }

    private void modelToView() {
        populateConnectionCombobox();
        sqlQueryTextArea.setText(assertion.getSqlQuery());
        variablePrefixTextField.setText(assertion.getVariablePrefix());
        maxRecordsSpinner.setValue(assertion.getMaxRecords());
        if (assertion.getConnectionName() == null) { // This is a new assertion. It means to load maxRecords from the global cluster properties.
            JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
            if (connectionAdmin != null) {
                maxRecordsSpinner.setValue(connectionAdmin.getPropertyDefaultMaxRecords());
            }
        }
        failAssertionCheckBox.setSelected(assertion.isAssertionFailureEnabled());
    }

    private void viewToModel(final JdbcQueryAssertion assertion) {
        assertion.setConnectionName(((String) connectionComboBox.getSelectedItem()));
        assertion.setSqlQuery(sqlQueryTextArea.getText());
        assertion.setNamingMap(namingMap);
        assertion.setVariablePrefix(variablePrefixTextField.getText());
        assertion.setMaxRecords((Integer) maxRecordsSpinner.getValue());
        assertion.setAssertionFailureEnabled(failAssertionCheckBox.isSelected());
    }

    private void populateConnectionCombobox() {
        java.util.List<String> connNameList;
        JdbcConnectionAdmin connectionAdmin = getJdbcConnectionAdmin();
        if (connectionAdmin == null) {
            return;
        } else {
            try {
                connNameList = connectionAdmin.getAllJdbcConnectionNames();
            } catch (FindException e) {
                logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connNameList);
        // Add an empty driver class at the first position of the list
        connNameList.add(0, "");

        // Add all items into the combox box.
        connectionComboBox.removeAllItems();
        for (String driverClass: connNameList) {
            connectionComboBox.addItem(driverClass);
        }

        connectionComboBox.setSelectedItem(assertion.getConnectionName());
    }

    private void initNamingTable() {
        namingMap = assertion.getNamingMap();

        namingTableModel = new NamingTableModel();
        namingTable.setModel(namingTableModel);
        namingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        namingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableTableButtons();
            }
        });
        Utilities.setDoubleClickAction(namingTable, editButton);

        enableOrDisableTableButtons();
    }

    private class NamingTableModel extends AbstractTableModel {
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
            return namingMap.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return resources.getString("column.label.column.label");
                case 1:
                    return resources.getString("column.label.variable.name");
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
            String name = (String) namingMap.keySet().toArray()[row];

            switch (col) {
                case 0:
                    return name;
                case 1:
                    return namingMap.get(name);
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }
    }

    private void enableOrDisableTableButtons() {
        int selectedRow = namingTable.getSelectedRow();

        boolean addEnabled = true;
        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;

        addButton.setEnabled(addEnabled);
        editButton.setEnabled(editEnabled);
        removeButton.setEnabled(removeEnabled);
    }

    private void enableOrDisableOkButton() {
         boolean enabled =
             !isReadOnly() &&
             isNonEmptyRequiredTextField((String) connectionComboBox.getSelectedItem()) &&
             isNonEmptyRequiredTextField(sqlQueryTextArea.getText()) &&
             isNonEmptyRequiredTextField(variablePrefixTextField.getText());

        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private void doTest() {
        final String[] warningMsg = new String[1];
        DialogDisplayer.showSafeConfirmDialog(
            TopComponents.getInstance().getTopParent(),
            MessageFormat.format(resources.getString("confirmation.test.query"), connectionComboBox.getSelectedItem()),
            resources.getString("dialog.title.test.query"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (option == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                    String connName = (String) connectionComboBox.getSelectedItem();
                    String query = sqlQueryTextArea.getText();
                    int numOfContextVariablesUsed = Syntax.getReferencedNames(query).length;

                    if (numOfContextVariablesUsed > 0) {
                        warningMsg[0] = "Unable to evaluate a query containing context variable" +  (numOfContextVariablesUsed > 1? "s." : ".");
                    } else {
                        JdbcConnectionAdmin admin = getJdbcConnectionAdmin();
                        if (admin == null) {
                            warningMsg[0] = "Cannot process testing due to JDBC Conneciton Admin unavailable.";
                        } else {
                            warningMsg[0] = admin.testJdbcQuery(connName, query);
                        }
                    }
                }
            }
        );
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.showMessageDialog(
                    JdbcQueryAssertionDialog.this,
                    (warningMsg[0] == null)? resources.getString("message.query.testing.passed") : resources.getString("message.query.testing.failed") + " " + warningMsg[0],
                    resources.getString("dialog.title.test.query"),
                    (warningMsg[0] == null)? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
                    null);
            }
        });
    }

    private void doAdd() {
        editAndSave(new MutablePair<String, String>("", ""));
    }

    private void doEdit() {
        int selectedRow = namingTable.getSelectedRow();
        if (selectedRow < 0) return;

        String queryResultName = (String) namingMap.keySet().toArray()[selectedRow];
        String contextVarName = namingMap.get(queryResultName);

        editAndSave(new MutablePair<String, String>(queryResultName, contextVarName));
    }

    private void editAndSave(final MutablePair<String, String> namePair) {
        if (namePair == null || namePair.left == null || namePair.right == null) return;
        final String originalQueryResultName = namePair.left;

        final ContextVariableNamingDialog dlg = new ContextVariableNamingDialog(this, namePair);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    // Check if the input entry is duplicated.
                    String warningMessage = isDuplicatedColumnOrVariable(namePair);
                    if (warningMessage != null) {
                        DialogDisplayer.showMessageDialog(JdbcQueryAssertionDialog.this, warningMessage,
                            resources.getString("dialog.titie.invalid.input"), JOptionPane.WARNING_MESSAGE, null);
                        return;
                    }

                    // Save the namePair into the map
                    if (! originalQueryResultName.isEmpty()) { // This is for doEdit
                        namingMap.remove(originalQueryResultName);
                    }
                    namingMap.put(namePair.left, namePair.right);
                    // Refresh the table
                    namingTableModel.fireTableDataChanged();
                    // Refresh the selection highlight
                    ArrayList<String> keyset = new ArrayList<String>();
                    keyset.addAll(namingMap.keySet());
                    int currentRow = keyset.indexOf(namePair.left);
                    namingTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        });
    }

    private String isDuplicatedColumnOrVariable(final MutablePair<String, String> pair) {

        // Check Column
        boolean duplicated = false;
        for (String key: namingMap.keySet()) {
            if (pair.left.compareToIgnoreCase(key) == 0) {
                duplicated = true;
                break;
            }
        }
        if (duplicated) {
            return MessageFormat.format(resources.getString("warning.message.duplicated.column"), pair.left);
        }

        // Check Variable
        duplicated = false;
        for (String value: namingMap.values()) {
            if (pair.right.compareToIgnoreCase(value) == 0) {
                duplicated = true;
                break;
            }
        }
        if (duplicated) {
            return MessageFormat.format(resources.getString("warning.message.duplicated.variable"), pair.right);
        }

        return null;
    }

    private void doRemove() {
        int currentRow = namingTable.getSelectedRow();
        if (currentRow < 0) return;

        String propName = (String) namingMap.keySet().toArray()[currentRow];
        Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
        int result = JOptionPane.showOptionDialog(
            this, MessageFormat.format(resources.getString("confirmation.remove.context.variable"), propName),
            resources.getString("dialog.title.remove.context.variable"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            // Refresh the list
            namingMap.remove(propName);
            // Refresh the table
            namingTableModel.fireTableDataChanged();
            // Refresh the selection highlight
            if (currentRow == namingMap.size()) currentRow--; // If the previous deleted row was the last row
            if (currentRow >= 0) namingTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private void doOk() {
        getData(assertion);
        confirmed = true;
        JdbcQueryAssertionDialog.this.dispose();
    }

    private void doCancel() {
        JdbcQueryAssertionDialog.this.dispose();
    }

    private JdbcConnectionAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }
}
