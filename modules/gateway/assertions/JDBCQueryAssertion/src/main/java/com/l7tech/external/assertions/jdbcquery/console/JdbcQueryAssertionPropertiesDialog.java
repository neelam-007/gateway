package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.MutablePair;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;
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
    /**
     * Should never have a null selection as a default is always set in @{link #modelToView}
     */
    private JComboBox<String> connectionComboBox;
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
    private JCheckBox convertVariablesToStringsCheckBox;
    private JCheckBox generateResultsAsXMLCheckBox;
    private JTextField schemaTextField;
    private JCheckBox schemaCheckBox;
    private JTextField queryTimeoutTextField;
    private JLabel queryWarningLabel;
    private JCheckBox saveResultsToContextCheckBox;

    private NamingTableModel namingTableModel;
    private Map<String, String> namingMap;
    private JdbcQueryAssertion assertion;
    private boolean confirmed;
    private PermissionFlags jdbcConnPermFlags;
    private final Map<String,String> connToDriverMap = new HashMap<String, String>();

    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

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
        enableOrDisableNamingTable();
        enableOrDisableJdbcConnList();
        enableOrDisableQueryControls();
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

        final RunOnChangeListener connectionListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableQueryControls();
                enableOrDisableOkButton();
            }
        });
        ((JTextField)connectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(connectionListener);
        connectionComboBox.addItemListener(connectionListener);

        schemaCheckBox.addChangeListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                schemaTextField.setEnabled(schemaCheckBox.isEnabled() && schemaCheckBox.isSelected());
                enableOrDisableOkButton();
            }
        }));


        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });
        sqlQueryTextArea.getDocument().addDocumentListener(changeListener);
        TextComponentPauseListenerManager.registerPauseListener(
                sqlQueryTextArea,
                new PauseListener() {
                    @Override
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        enableOrDisableQueryControls();
                    }

                    @Override
                    public void textEntryResumed(JTextComponent component) {
                        enableOrDisableQueryControls();
                    }
                },
                300);

        schemaTextField.getDocument().addDocumentListener(changeListener);

        variablePrefixTextField.addChangeListener(changeListener);

        final RunOnChangeListener queryPanelChangeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableQueryControls();
                enableOrDisableOkButton();
            }
        });

        initNamingTable();

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title.jdbc.query.props"));
        // The values in the spinners will be initialized in the method modelToView().
        maxRecordsSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_MAX_RECORDS, JdbcAdmin.UPPER_BOUND_MAX_RECORDS, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxRecordsSpinner, resources.getString("short.lable.max.records")));

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doTest();
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

        Utilities.setDoubleClickAction(namingTable, editButton);

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRemove();
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

        saveResultsToContextCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Note that enabling or disabling the namingTable has no effect.
                enableOrDisableTableButtons();
                enableOrDisableNamingTable();
            }
        });
        namingTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent
                    (JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                setEnabled(table.isEnabled());
                return super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            }
        });
    }

    private void enableOrDisableNamingTable() {
        final boolean saveResultsToContextVariable = saveResultsToContextCheckBox.isSelected();
        namingTable.setEnabled(saveResultsToContextVariable);
        namingTable.getTableHeader().setForeground(saveResultsToContextVariable ? Color.BLACK : Color.GRAY);
        namingTable.repaint();
    }

    private void modelToView() {
        populateConnectionCombobox();
        sqlQueryTextArea.setText(assertion.getSqlQuery());
        sqlQueryTextArea.setCaretPosition(0);
        testButton.setEnabled(assertion.getSqlQuery()!=null);
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
        generateResultsAsXMLCheckBox.setSelected(assertion.isGenerateXmlResult());
        saveResultsToContextCheckBox.setSelected(assertion.isSaveResultsAsContextVariables());
        queryNameTextField.setText(assertion.getQueryName());
        convertVariablesToStringsCheckBox.setSelected(assertion.isConvertVariablesToStrings());

        final String connName = assertion.getConnectionName();
        if (connName != null) {
            connectionComboBox.setSelectedItem(connName);

            final String schema = assertion.getSchema();
            final boolean hasSchema = schema != null && !schema.trim().isEmpty();
            if (hasSchema) {
                schemaTextField.setText(schema);
                schemaCheckBox.setSelected(true);
            }
        } else {
            // default selection is no selection
            connectionComboBox.setSelectedItem("");
        }

        final String queryTimeout = assertion.getQueryTimeout();
        queryTimeoutTextField.setText((queryTimeout != null) ? queryTimeout : "0");
        enableOrDisableSchemaControls();

    }

    private void viewToModel(final JdbcQueryAssertion assertion) {
        assertion.setConnectionName(( connectionComboBox.getSelectedItem()).toString());
        assertion.setSqlQuery(sqlQueryTextArea.getText());
        assertion.setNamingMap(namingMap);
        assertion.setVariablePrefix(variablePrefixTextField.getVariable());
        assertion.setMaxRecords((Integer) maxRecordsSpinner.getValue());
        final String queryTimeout = queryTimeoutTextField.getText().trim();
        assertion.setQueryTimeout(("0".equals(queryTimeout)) ? null : queryTimeout);
        assertion.setAssertionFailureEnabled(failAssertionCheckBox.isSelected());
        assertion.setGenerateXmlResult(generateResultsAsXMLCheckBox.isSelected());
        assertion.setSaveResultsAsContextVariables(saveResultsToContextCheckBox.isSelected());
        assertion.setQueryName(queryNameTextField.getText());
        assertion.setConvertVariablesToStrings(convertVariablesToStringsCheckBox.isSelected());
        final String schemaValue = schemaTextField.getText().trim();
        assertion.setSchema((schemaCheckBox.isSelected() && isSchemaCapable(connectionComboBox.getSelectedItem().toString()) && schemaTextField.isEnabled() && !schemaValue.isEmpty())? schemaValue: null);
    }

    private void enableOrDisableSchemaControls() {
        final String connName = connectionComboBox.getSelectedItem().toString();
        schemaCheckBox.setEnabled(isSchemaCapable(connName) && JdbcUtil.isStoredProcedure(sqlQueryTextArea.getText().toLowerCase()));
    }

    private boolean isSchemaCapable(final String connName) {
        if (connToDriverMap.containsKey(connName)) {
            final String driverClass = connToDriverMap.get(connName);
            return driverClass.contains("oracle") || driverClass.contains("sqlserver");
        }

        // if we don't know about the connection and a variable is entered, then we need to allow it
        return Syntax.isAnyVariableReferenced(connName);
    }

    private void populateConnectionCombobox() {
        java.util.List<JdbcConnection> connectionList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) {
            return;
        } else {
            try {
                connectionList = admin.getAllJdbcConnections();
            } catch (FindException e) {
                logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connectionList);

        connectionComboBox.removeAllItems();

        // Add an empty driver class at the first position of the list
        connectionComboBox.addItem("");

        for (JdbcConnection conn: connectionList) {
            final String connName = conn.getName();
            connToDriverMap.put(connName, conn.getDriverClass());
            connectionComboBox.addItem(connName);
        }

    }

    private void initNamingTable() {

        namingTableModel = new NamingTableModel();
        namingTable.setModel(namingTableModel);
        namingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        namingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableTableButtons();
            }
        });

        enableOrDisableTableButtons();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
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

    private void enableOrDisableQueryControls(){
        String query = sqlQueryTextArea.getText();
        testButton.setEnabled(!query.trim().isEmpty());
        enableOrDisableSchemaControls();

        if (JdbcUtil.isStoredProcedure(query)) {
            String procedureName = JdbcUtil.getName(query);
            if (!procedureName.isEmpty()) {

                boolean isValidQuery = procedureName.indexOf('.') == procedureName.lastIndexOf('.');
                if (!isValidQuery && isSchemaCapable(connectionComboBox.getSelectedItem().toString())) {
                    queryWarningLabel.setIcon(WARNING_ICON);
                    queryWarningLabel.setText("Query may not reference schema from query, specify schema below instead.");
                } else {
                    queryWarningLabel.setIcon(null);
                    queryWarningLabel.setText("");
                }
            }
        }
    }

    private void enableOrDisableTableButtons() {
        int selectedRow = namingTable.getSelectedRow();
        boolean saveResultsToContextVariable = saveResultsToContextCheckBox.isSelected();

        boolean addEnabled = saveResultsToContextVariable;
        boolean editEnabled = saveResultsToContextVariable && selectedRow >= 0;
        boolean removeEnabled = saveResultsToContextVariable && selectedRow >= 0;

        addButton.setEnabled(addEnabled);
        editButton.setEnabled(editEnabled);
        removeButton.setEnabled(removeEnabled);
    }

    private void enableOrDisableOkButton() {
        boolean enabled = !isReadOnly() &&
            isNonEmptyRequiredTextField(((JTextField)connectionComboBox.getEditor().getEditorComponent()).getText()) &&
            isNonEmptyRequiredTextField(sqlQueryTextArea.getText()) &&
            variablePrefixTextField.isEntryValid() && !(schemaCheckBox.isSelected() && schemaTextField.getText().isEmpty());

        okButton.setEnabled(enabled);
    }

    private void enableOrDisableJdbcConnList() {
        connectionComboBox.setEnabled(jdbcConnPermFlags.canReadSome());
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private void doTest() {
        DialogDisplayer.showSafeConfirmDialog(
            TopComponents.getInstance().getTopParent(),
            MessageFormat.format(resources.getString("confirmation.test.query"), connectionComboBox.getSelectedItem().toString()),
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

                    final String connName = connectionComboBox.getSelectedItem().toString();
                    if ("".equals(connName)) {
                        displayQueryTestingResult("Please select a JDBC Connection to test against.");
                        return;
                    }

                    final String schemaName = schemaCheckBox.isEnabled() && schemaCheckBox.isSelected() ? schemaTextField.getText().trim() : "";
                    if (Syntax.getReferencedNames(schemaName).length > 0) {
                        displayQueryTestingResult("Cannot process testing due to JDBC Schema name containing context variable(s).");
                        return;
                    } else if (schemaName.matches(".*\\s.*")) {
                        displayQueryTestingResult("Cannot process testing due to JDBC Schema name containing spaces.");
                        return;
                    }

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

                    if (Syntax.getReferencedNames(queryTimeoutTextField.getText()).length > 0) {
                        displayQueryTestingResult("Unable to evaluate a query with a context variable for query timeout.");
                        return;
                    }

                    if (!isQueryTimeoutValid()) {
                        displayQueryTestingResult("Unable to evaluate a query with an invalid query timeout.");
                        return;
                    }
                    final int queryTimeout = Integer.parseInt(queryTimeoutTextField.getText());

                    final JdbcAdmin admin = getJdbcConnectionAdmin();
                    try {
                        displayQueryTestingResult(admin == null ?
                                "Cannot process testing due to JDBC Connection Admin unavailable." : doAsyncAdmin(
                                admin,
                                JdbcQueryAssertionPropertiesDialog.this,
                                resources.getString("dialog.title.test.query"),
                                resources.getString("dialog.title.test.query"),
                                admin.testJdbcQuery(connName, query, schemaName.isEmpty() ? null : schemaName, queryTimeout)).right());
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

        // validate an oracle query does not try and reference a schema name. This is a heuristic as it cannot be
        // validated for sure without knowing the list of valid package names.
        boolean isValidQuery = true;
        final String connName = connectionComboBox.getSelectedItem().toString();
        final String query = sqlQueryTextArea.getText().trim();

        if (isSchemaCapable(connName) && JdbcUtil.isStoredProcedure(query)) {
            String procedureName = JdbcUtil.getName(query);
            if (!procedureName.isEmpty()) {
                // if there are no dots e.g. -1 or a single dot, then query is valid. If there are 2 dots then it's invalid.
                // 2 dots means the user is trying to do schema.package.name and this is not the name of a literal object
                // in oracle so this will never work.
                final boolean enableOracleSchemaCheck = SyspropUtil.getBoolean(
                        "com.l7tech.external.assertions.jdbcquery.console.enableOracleNoSchemaInQuery",
                        true);

                if (enableOracleSchemaCheck) {
                    isValidQuery = procedureName.indexOf('.') == procedureName.lastIndexOf('.');
                }

                if (!isValidQuery) {
                    DialogDisplayer.showMessageDialog(this, "Query cannot reference schema from query, use schema field instead", "Invalid Query", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
            }
        }

        if(schemaTextField.isEnabled() && !isSchemaValid()) {
            DialogDisplayer.showMessageDialog(this, "Schema must be a single variable reference or a string that does not contain spaces.", "Invalid Schema", JOptionPane.INFORMATION_MESSAGE, null);
            return;
        }

        if (!isQueryTimeoutValid()) {
            DialogDisplayer.showMessageDialog(this, "Query Timeout must be a single variable reference or a valid Integer >= 0", "Invalid Query Timeout", JOptionPane.INFORMATION_MESSAGE, null);
            return;
        }

        getData(assertion);
        confirmed = true;
        JdbcQueryAssertionPropertiesDialog.this.dispose();
    }

    private boolean isQueryTimeoutValid(){
        boolean isQueryValid = true;
        final String text = queryTimeoutTextField.getText().trim();
        if (!Syntax.isOnlyASingleVariableReferenced(text)) {
            isQueryValid = false;
        }

        if (Syntax.getReferencedNames(text).length == 0) {
            isQueryValid = ValidationUtils.isValidInteger(text, false, 0, Integer.MAX_VALUE);
        }

        return isQueryValid;

    }

    private boolean isSchemaValid(){
        final String schema = schemaTextField.getText().trim();
        try {
            return Syntax.isOnlyASingleVariableReferenced(schema) || (!Syntax.isAnyVariableReferenced(schema) && !schema.matches(".*\\s.*"));
        } catch (VariableNameSyntaxException e) {
            return false;
        }
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