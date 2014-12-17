package com.l7tech.external.assertions.cassandra.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.cassandra.CassandraQueryAssertion;
import com.l7tech.external.assertions.cassandra.CassandraNamedParameter;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;
import com.l7tech.util.MutablePair;
import com.l7tech.util.ValidationUtils;

import java.text.MessageFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.policy.variable.Syntax.getReferencedNames;
import static com.l7tech.util.ValidationUtils.isValidInteger;


/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class CassandraAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<CassandraQueryAssertion> {

    private static final Logger logger = Logger.getLogger(CassandraAssertionPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.cassandra.console.CassandraQueryAssertionPropertiesDialog");
    private static final int LOWER_BOUND_MAX_RECORDS = 1;
    private static final int UPPER_BOUND_MAX_RECORDS = 10000;
    private static final String MAX_RECORDS_SIZE = "maxRecords";
    private static final String FETCH_SIZE = "fetchSize";

    private JPanel mainPanel;
    /**
     * Should never have a null selection as a default is always set in @{link #modelToView}
     */
    private JComboBox<String> connectionComboBox;
    private JTextArea cqlQueryTextArea;
    private JButton cancelButton;
    private JButton okButton;
    private JCheckBox failIfNoResultsCheckBox;
    private JTable variableNamingTable;
    private TableRowSorter<SimpleTableModel<CassandraNamedParameter>> rowSorter;
    private SimpleTableModel<MutablePair<String, String>> variableNamingTableModel;
    private JButton addMappingButton;
    private JButton removeMappingButton;
    private JButton editMappingButton;
    private TargetVariablePanel variablePrefixPanel;
    private JLabel connectionLabel;
    private JPanel queryPanel;
    private JCheckBox generateXMLResultCheckBox;
    private JSpinner maxRecordsSpinner;
    private JSpinner fetchSizeSpinner;
    private JButton testQueryButton;
    private JTextField queryTimeoutTextField;
    private CassandraQueryAssertion assertion;
    private boolean confirmed;
    private Map<String, String> variableNamingMap = new HashMap<>();
    private InputValidator inputValidator;
    private CassandraConnectionManagerAdmin cassandraConnectionEntityAdmin;
    private ClusterStatusAdmin clusterStatusAdmin;

    public CassandraAssertionPropertiesDialog(Window owner, CassandraQueryAssertion assertion) {
        super(owner, resources.getString("dialog.title.cassandra.query.props"), AssertionPropertiesEditorSupport.DEFAULT_MODALITY_TYPE);
        this.assertion = assertion;
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CassandraQueryAssertion assertion) {
        this.assertion = assertion;
        modelToView();
        configureView();
    }

    @Override
    public CassandraQueryAssertion getData(final CassandraQueryAssertion assertion) {
        viewToModel(assertion);
        return assertion;
    }

    @Override
    protected void configureView() {
       enableOrDisableOkButton();
    }

    private SimpleTableModel<MutablePair<String,String>> buildVariableNamingTableModel() {
        return TableUtil.configureTable(
                variableNamingTable,
                TableUtil.column(resources.getString("column.label.column.label"), 40, 200, 1000000, new Functions.Unary<String, MutablePair<String,String>>() {
                    @Override
                    public String call(MutablePair<String, String> row) {
                        return row.left;
                    }
                }, String.class),
                TableUtil.column(resources.getString("column.label.variable.name"), 40, 200, 180, new Functions.Unary<String, MutablePair<String,String>>() {
                    @Override
                    public String call(MutablePair<String,String> row) {
                        return row.right;
                    }
                }, String.class)
        );
    }

    private void enableDisableComponents() {
        boolean isValidPrefix = variablePrefixPanel.isEntryValid();
        final int[] selectedRows = variableNamingTable.getSelectedRows();
        addMappingButton.setEnabled(isValidPrefix);
        editMappingButton.setEnabled(isValidPrefix & selectedRows.length == 1);
        removeMappingButton.setEnabled(isValidPrefix & selectedRows.length > 0);
    }

    private void initialize() {
        clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
        inputValidator = new InputValidator(this, this.getTitle());

        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };
        maxRecordsSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_MAX_RECORDS, UPPER_BOUND_MAX_RECORDS, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxRecordsSpinner, resources.getString("validator.max.records")));

        fetchSizeSpinner.setModel(new SpinnerNumberModel(1, LOWER_BOUND_MAX_RECORDS, Integer.MAX_VALUE, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(fetchSizeSpinner, resources.getString("validator.fetch.size")));
        //mapping table
        variableNamingTableModel = buildVariableNamingTableModel();
        variableNamingTable.setModel(variableNamingTableModel);
        variableNamingTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        variableNamingTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        Utilities.setRowSorter(variableNamingTable, variableNamingTableModel, new int[] {0}, new boolean[] {true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<CassandraNamedParameter>>) variableNamingTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);

        inputValidator.constrainTextFieldToBeNonEmpty(queryPanel.getName(), cqlQueryTextArea, null);
        inputValidator.constrainTextFieldToBeNonEmpty("Query Timeout", queryTimeoutTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String errMsg = null;
                final String timeout = queryTimeoutTextField.getText().trim();
                if (!ValidationUtils.isValidLong(timeout, true, 0L, Long.MAX_VALUE) && getReferencedNames(timeout).length == 0) {
                    errMsg = "The value for the timeout must be a valid positive number.";
                }
                return errMsg;
            }
        });

        addMappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MutablePair<String, String> columnAlias = new MutablePair<>();
                ContextVariableNamingDialog dialog = new ContextVariableNamingDialog(CassandraAssertionPropertiesDialog.this, columnAlias, variablePrefixPanel.getVariable());
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    columnAlias = dialog.getData(columnAlias);
                    if (columnAlias != null) {
                        try {
                            variableNamingMap.put(columnAlias.getKey(), columnAlias.getValue());
                            variableNamingTableModel.setRows(getNamedMappingList());
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(CassandraAssertionPropertiesDialog.this, resources.getString("message.context.variable.naming.new.failed"), resources.getString("dialog.title.error"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        editMappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int rowIndex = rowSorter.convertRowIndexToModel(variableNamingTable.getSelectedRow());
                MutablePair<String, String> columnAlias = variableNamingTableModel.getRowObject(rowIndex);
                final String prevKey = columnAlias.getKey();
                ContextVariableNamingDialog dialog =
                        new ContextVariableNamingDialog(CassandraAssertionPropertiesDialog.this, columnAlias, variablePrefixPanel.getVariable());
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if(dialog.isConfirmed()){
                    columnAlias = dialog.getData(columnAlias);
                    variableNamingMap.remove(prevKey);//remove previous mapping
                    variableNamingMap.put(columnAlias.getKey(), columnAlias.getValue()); //this is the problem
                    variableNamingTableModel.setRowObject(rowIndex, columnAlias);
                }
            }
        });

        removeMappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int rowIndex = rowSorter.convertRowIndexToModel(variableNamingTable.getSelectedRow());
                MutablePair<String, String> cassandraNamedParameter = variableNamingTableModel.getRowObject(rowIndex);

                Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
                int result = JOptionPane.showOptionDialog(
                        CassandraAssertionPropertiesDialog.this, MessageFormat.format(resources.getString("confirmation.remove.context.variable"), cassandraNamedParameter.left),
                        resources.getString("dialog.title.remove.context.variable"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if(result == 0) {
                    variableNamingTableModel.removeRow(cassandraNamedParameter);
                    variableNamingMap.remove(cassandraNamedParameter.getKey());
                    variableNamingTableModel.setRows(getNamedMappingList());
                }
            }
        });

        cqlQueryTextArea.setDocument(new MaxLengthDocument(JdbcAdmin.MAX_QUERY_LENGTH));
        inputValidator.constrainTextFieldToBeNonEmpty("Query", cqlQueryTextArea, null);
        inputValidator.ensureComboBoxSelection(connectionLabel.getText(), connectionComboBox);

        final RunOnChangeListener connectionListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                if (connectionComboBox.getSelectedIndex() != -1) {
                    if(!connectionComboBox.getSelectedItem().equals(assertion.getConnectionName())) {
                        String connName = (String) connectionComboBox.getSelectedItem();

                        CassandraConnection cassandraConnection = null;
                        try {
                            cassandraConnection = cassandraConnectionEntityAdmin.getCassandraConnection(connName);
                        } catch (FindException e) {
                            JOptionPane.showMessageDialog(CassandraAssertionPropertiesDialog.this, MessageFormat.format(resources.getString("message.error.find.connection"), connName), "Error", JOptionPane.ERROR);
                            return;
                        }

                        Map<String, String> props = cassandraConnection.getProperties();

                        setSpinnerValue(props.get(MAX_RECORDS_SIZE), maxRecordsSpinner, "cassandra.maxRecords", CassandraQueryAssertion.DEFAULT_MAX_RECORDS);
                        setSpinnerValue(props.get(FETCH_SIZE), fetchSizeSpinner, "cassandra.fetchSize", CassandraQueryAssertion.DEFAULT_FETCH_SIZE);
                    }
                    else {
                        maxRecordsSpinner.setValue(assertion.getMaxRecords());
                        fetchSizeSpinner.setValue(assertion.getFetchSize());
                    }
                    enableOrDisableOkButton();
                }
            }
        });

        ((JTextField)connectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(connectionListener);
        connectionComboBox.addItemListener(connectionListener);


        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });

        cqlQueryTextArea.getDocument().addDocumentListener(changeListener);

        variablePrefixPanel.addChangeListener(changeListener);

        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        addMappingButton.setEnabled(true);
        //==================================================================
        //TODO: once query testing is implemented remove the statement below
        testQueryButton.setEnabled(false);
        //==================================================================
        Utilities.setDoubleClickAction(variableNamingTable, editMappingButton);

        setContentPane(mainPanel);
        pack();
    }

    private void setSpinnerValue(String connectionPropValue, JSpinner spinner, String clusterPropName, int spinnerDefault) {
        String propName = null;
        try {
            if (connectionPropValue == null) {
                ClusterProperty property = clusterStatusAdmin.findPropertyByName(clusterPropName);
                if (property != null && property.getValue() != null) {
                    connectionPropValue = property.getValue();
                    propName = property.getName();

                }
            }

            spinner.setValue(getIntOrDefault(connectionPropValue, spinnerDefault));
        } catch (FindException e) {
            JOptionPane.showMessageDialog(this, MessageFormat.format(resources.getString("message.error.find.prop"), propName), "Error", JOptionPane.ERROR);
            maxRecordsSpinner.setValue(LOWER_BOUND_MAX_RECORDS);
        }
    }

    private Integer getIntOrDefault(String s, int defaultValue) {
        try{
            return Integer.parseInt(s);
        } catch(NumberFormatException nfe) {
            return new Integer(defaultValue);
        }
    }

    private List<MutablePair<String, String>> getNamedMappingList() {
        List<MutablePair<String, String>> namedMapingList = new ArrayList<>();
        for(String key : variableNamingMap.keySet()) {
            namedMapingList.add(new MutablePair<String, String>(key, variableNamingMap.get(key)));
        }
        Collections.sort(namedMapingList, new Comparator<MutablePair<String, String>>() {
            @Override
            public int compare(MutablePair<String, String> o1, MutablePair<String, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return namedMapingList;
    }

    private void ok(){
        confirmed = true;
        dispose();
    }

    private void modelToView() {
        populateConnectionCombobox();
        cqlQueryTextArea.setText(assertion.getQueryDocument());
        cqlQueryTextArea.setCaretPosition(0);
        failIfNoResultsCheckBox.setSelected(assertion.isFailIfNoResults());
        generateXMLResultCheckBox.setSelected(assertion.isGenerateXmlResult());
        variablePrefixPanel.setVariable(assertion.getPrefix());
        maxRecordsSpinner.setValue(assertion.getMaxRecords());
        fetchSizeSpinner.setValue(assertion.getFetchSize());
        variableNamingMap.clear();
        variableNamingMap.putAll(assertion.getNamingMap());
        variableNamingTableModel.setRows(getNamedMappingList());
        enableDisableComponents();
        queryTimeoutTextField.setText(String.valueOf(assertion.getQueryTimeout()));
    }

    private void viewToModel(final CassandraQueryAssertion assertion) {
        assertion.setConnectionName((String)connectionComboBox.getSelectedItem());
        assertion.setQueryDocument(cqlQueryTextArea.getText());
        assertion.setFailIfNoResults(failIfNoResultsCheckBox.isSelected());
        assertion.setGenerateXmlResult(generateXMLResultCheckBox.isSelected());
        assertion.setPrefix(variablePrefixPanel.getVariable());
        assertion.setMaxRecords((Integer)maxRecordsSpinner.getValue());
        assertion.setFetchSize((Integer)fetchSizeSpinner.getValue());
        variableNamingMap = new HashMap<>();
        for(MutablePair<String,String> pair : variableNamingTableModel.getRows()) {
            variableNamingMap.put(pair.left, pair.right);
        }
        assertion.setNamingMap(variableNamingMap);
        assertion.setQueryTimeout(Long.parseLong(queryTimeoutTextField.getText()));
    }

    private void populateConnectionCombobox() {
        String selectedConnection = null;

        try {
           DefaultComboBoxModel model = new DefaultComboBoxModel();
            cassandraConnectionEntityAdmin = Registry.getDefault().getCassandraConnectionAdmin();

           for(String connectionName : cassandraConnectionEntityAdmin.getAllCassandraConnectionNames()) {
               model.addElement(connectionName);
               if(connectionName.equals(assertion.getConnectionName())){
                   selectedConnection = connectionName;
               }
           }

           connectionComboBox.setModel(model);
           connectionComboBox.setSelectedItem(selectedConnection);
       } catch(FindException fe) {
            fe.printStackTrace();
       }

    }

    private void enableOrDisableOkButton() {
        boolean enabled = inputValidator.isValid() &&
                variablePrefixPanel.isEntryValid();
        enableDisableComponents();
        okButton.setEnabled(enabled);
    }

    private void doCancel() {
        CassandraAssertionPropertiesDialog.this.dispose();
    }

}