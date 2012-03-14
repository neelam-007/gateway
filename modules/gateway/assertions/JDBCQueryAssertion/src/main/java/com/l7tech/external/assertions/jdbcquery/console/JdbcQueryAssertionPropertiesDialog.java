package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.util.MutablePair;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;

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
 * Properties dialog for the JDBC Query Assertion.
 */
public class JdbcQueryAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<JdbcQueryAssertion> {
    private static final int MAX_TABLE_COLUMN_NUM = 2;
    private static final int LOWER_BOUND_MAX_RECORDS = 1;
    private static final Logger logger = Logger.getLogger(JdbcQueryAssertionPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.jdbcquery.console.resources.JdbcQueryAssertionPropertiesDialog");

    private JPanel mainPanel;
    private JComboBox connectionComboBox;
    private JTextArea sqlQueryTextArea;
    private JTable namingTable;
    private JPanel variablePrefixTextPanel;
    private TargetVariablePanel variablePrefixTextField;
    private JButton testButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton cancelButton;
    private JButton okButton;
    private JCheckBox failAssertionCheckBox;
    private JSpinner maxRecordsSpinner;
    private JTextField queryNameTextField;
    private JCheckBox allowMutiValuedVariablesCheckBox;

    private NamingTableModel namingTableModel;
    private Map<String, String> namingMap;
    private JdbcQueryAssertion assertion;
    private boolean confirmed;
    private PermissionFlags jdbcConnPermFlags;

    public JdbcQueryAssertionPropertiesDialog(Window owner, JdbcQueryAssertion assertion) {
        super(owner, assertion);
        this.assertion = assertion;
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(JdbcQueryAssertion assertion) {
        this.assertion = assertion;
        modelToView();
        configureView();
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
        enableOrDisableJdbcConnList();
    }

    private void initialize() {
        jdbcConnPermFlags = PermissionFlags.get(EntityType.JDBC_CONNECTION);

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        variablePrefixTextField = new TargetVariablePanel();
        variablePrefixTextPanel.setLayout(new BorderLayout());
        variablePrefixTextPanel.add(variablePrefixTextField, BorderLayout.CENTER);

        sqlQueryTextArea.setDocument(new MaxLengthDocument(JdbcAdmin.MAX_QUERY_LENGTH));
        queryNameTextField.setDocument(new MaxLengthDocument(128));

        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        });
        ((JTextField)connectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(changeListener);
        connectionComboBox.addItemListener(changeListener);
        sqlQueryTextArea.getDocument().addDocumentListener(changeListener);
        variablePrefixTextField.addChangeListener(changeListener);

        initNamingTable();

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title.jdbc.query.props"));
        // The values in the spinners will be initialized in the method modelToView().
        maxRecordsSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_MAX_RECORDS, JdbcAdmin.UPPER_BOUND_MAX_RECORDS, 1));
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
    }

    private void modelToView() {
        populateConnectionCombobox();
        sqlQueryTextArea.setText(assertion.getSqlQuery());
        namingMap = assertion.getNamingMap();
        variablePrefixTextField.setVariable(assertion.getVariablePrefix());
        variablePrefixTextField.setAssertion(assertion,getPreviousAssertion());
        variablePrefixTextField.setSuffixes(getSuffixes());
        maxRecordsSpinner.setValue(assertion.getMaxRecords());
        if (assertion.getConnectionName() == null) { // This is a case where the assertion is a new one.  It means to load maxRecords from the global cluster properties.
            JdbcAdmin admin = getJdbcConnectionAdmin();
            if (admin != null) {
                maxRecordsSpinner.setValue(admin.getPropertyDefaultMaxRecords());
            }
        }
        failAssertionCheckBox.setSelected(assertion.isAssertionFailureEnabled());
        queryNameTextField.setText(assertion.getQueryName());
        allowMutiValuedVariablesCheckBox.setSelected(assertion.isAllowMultiValuedVariables());
        
    }

    private void viewToModel(final JdbcQueryAssertion assertion) {
        assertion.setConnectionName(((String) connectionComboBox.getSelectedItem()));
        assertion.setSqlQuery(sqlQueryTextArea.getText());
        assertion.setNamingMap(namingMap);
        assertion.setVariablePrefix(variablePrefixTextField.getVariable());
        assertion.setMaxRecords((Integer) maxRecordsSpinner.getValue());
        assertion.setAssertionFailureEnabled(failAssertionCheckBox.isSelected());
        assertion.setQueryName(queryNameTextField.getText());
        assertion.setAllowMultiValuedVariables(allowMutiValuedVariablesCheckBox.isSelected());
    }

    private void populateConnectionCombobox() {
        java.util.List<String> connNameList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) {
            return;
        } else {
            try {
                connNameList = admin.getAllJdbcConnectionNames();
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
        boolean enabled = !isReadOnly() &&
            isNonEmptyRequiredTextField(((JTextField)connectionComboBox.getEditor().getEditorComponent()).getText()) &&
            isNonEmptyRequiredTextField(sqlQueryTextArea.getText()) &&
            variablePrefixTextField.isEntryValid();

        okButton.setEnabled(enabled);
    }

    private void enableOrDisableJdbcConnList() {
        connectionComboBox.setEnabled(jdbcConnPermFlags.canReadAll());
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
                        displayQueryTestingResult("TestingCanceled");
                        return;
                    }

                    final String connName = (String) connectionComboBox.getSelectedItem();
                    if (Syntax.getReferencedNames(connName).length > 0) {
                        displayQueryTestingResult("Cannot process testing due to JDBC Connection name containing context variable(s).");
                        return;
                    }

                    final String query = sqlQueryTextArea.getText();
                    final int numOfContextVariablesUsed = Syntax.getReferencedNames(query).length;
                    if (numOfContextVariablesUsed > 0) {
                        displayQueryTestingResult("Unable to evaluate a query containing context variable" + (numOfContextVariablesUsed > 1 ? "s." : "."));
                        return;
                    }

                    JdbcAdmin admin = getJdbcConnectionAdmin();
                    try {
                        displayQueryTestingResult(admin == null ?
                            "Cannot process testing due to JDBC Conneciton Admin unavailable." : doAsyncAdmin(
                            admin,
                            JdbcQueryAssertionPropertiesDialog.this,
                            resources.getString("dialog.title.test.query"),
                            resources.getString("dialog.title.test.query"),
                            admin.testJdbcQuery(connName, query)).right());
                    } catch (InterruptedException e) {
                        // operation cancelled by user, do nothing
                    } catch (InvocationTargetException e) {
                        displayQueryTestingResult(e.getMessage());
                    }
                }
            }
        );
    }

    /**
     * The testing result could be test success message or failure messages.
     * @param resultMessage: the result message of testing
     */
    private void displayQueryTestingResult(String resultMessage) {
        if ("TestingCanceled".equals(resultMessage)) return;

        DialogDisplayer.showMessageDialog(
            JdbcQueryAssertionPropertiesDialog.this,
            (resultMessage == null)? resources.getString("message.query.testing.passed") : resources.getString("message.query.testing.failed") + " " + resultMessage,
            resources.getString("dialog.title.test.query"),
            (resultMessage == null)? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
            null);
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
    public String[] getSuffixes(){
        List<String> suffixes = new ArrayList<String>();
        suffixes.add(JdbcQueryAssertion.VARIABLE_COUNT);
        for (String key: namingMap.keySet()) {
            suffixes.add(namingMap.get(key));
        }
        return suffixes.toArray(new String[suffixes.size()]);
    }

    private void editAndSave(final MutablePair<String, String> namePair) {
        if (namePair == null || namePair.left == null || namePair.right == null) return;
        final MutablePair<String, String> originalPair = new MutablePair<String, String>(namePair.left, namePair.right);

        String suffix = variablePrefixTextField.getVariable();
        final ContextVariableNamingDialog dlg = new ContextVariableNamingDialog(this, namePair,suffix,assertion,getPreviousAssertion());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    // Check if the input entry is duplicated.
                    String warningMessage = isDuplicatedColumnOrVariable(namePair, originalPair);
                    if (warningMessage != null) {
                        DialogDisplayer.showMessageDialog(JdbcQueryAssertionPropertiesDialog.this, warningMessage,
                            resources.getString("dialog.titie.invalid.input"), JOptionPane.WARNING_MESSAGE, null);
                        return;
                    }

                    // Save the namePair into the map
                    String originalColumnLabel = originalPair.left;
                    if (! originalColumnLabel.isEmpty()) { // This is for doEdit
                        namingMap.remove(originalColumnLabel);
                    }
                    namingMap.put(namePair.left, namePair.right);

                    // Refresh the table
                    namingTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    ArrayList<String> keyset = new ArrayList<String>();
                    keyset.addAll(namingMap.keySet());
                    int currentRow = keyset.indexOf(namePair.left);
                    namingTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);

                    variablePrefixTextField.setSuffixes(getSuffixes());

                }
            }
        });
    }

    private String isDuplicatedColumnOrVariable(final MutablePair<String, String> newPair, final MutablePair<String, String> originalPair) {
        // Check Column
        String originalColumnLabel = originalPair.left;
        for (String key: namingMap.keySet()) {
            if (originalColumnLabel.compareToIgnoreCase(key) != 0 // make sure not to compare itself
                && newPair.left.compareToIgnoreCase(key) == 0) {
                return MessageFormat.format(resources.getString("warning.message.duplicated.column"), newPair.left);
            }
        }

        // Check Variable
        String originalVariable = originalPair.right;
        for (String value: namingMap.values()) {
            if (originalVariable.compareToIgnoreCase(value) != 0 // make sure not to compare itself
                && newPair.right.compareToIgnoreCase(value) == 0) {
                return MessageFormat.format(resources.getString("warning.message.duplicated.variable"), newPair.right);
            }
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
        JdbcQueryAssertionPropertiesDialog.this.dispose();
    }

    private void doCancel() {
        JdbcQueryAssertionPropertiesDialog.this.dispose();
    }

    private JdbcAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }
}