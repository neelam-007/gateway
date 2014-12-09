package com.l7tech.external.assertions.cassandra.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.cassandra.CassandraQueryAssertion;
import com.l7tech.external.assertions.cassandra.CassandraNamedParameter;

import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;
import com.l7tech.util.MutablePair;
import org.apache.commons.collections.map.HashedMap;

import java.util.*;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;


/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class CassandraAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<CassandraQueryAssertion> {

    private static final Logger logger = Logger.getLogger(CassandraAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    /**
     * Should never have a null selection as a default is always set in @{link #modelToView}
     */
    private JComboBox<String> connectionComboBox;
    private JTextArea cqlQueryTextArea;
    private JButton cancelButton;
    private JButton okButton;
    private JCheckBox failIfNoResultsCheckBox;
    private JTextField prefixTextField;
    private JTable variableNamingTable;
    private TableRowSorter<SimpleTableModel<CassandraNamedParameter>> rowSorter;
    private SimpleTableModel<MutablePair<String, String>> variableNamingTableModel;
    private JButton addParameterButton;
    private JButton removeParameterButton;
    private JButton editParameterButton;
    private CassandraQueryAssertion assertion;
    private boolean confirmed;
    private Map<String, String> variableNamingMap;

    public CassandraAssertionPropertiesDialog(Window owner, CassandraQueryAssertion assertion) {
        super(owner, assertion);
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
                TableUtil.column("Column Label", 40, 200, 1000000, new Functions.Unary<String, MutablePair<String,String>>() {
                    @Override
                    public String call(MutablePair<String, String> row) {
                        return row.left;
                    }
                }, String.class),
                TableUtil.column("Variable Name", 40, 100, 180, new Functions.Unary<String, MutablePair<String,String>>() {
                    @Override
                    public String call(MutablePair<String,String> row) {
                        return row.right;
                    }
                }, String.class)
        );
    }

    private void enableDisableComponents() {
        final int[] selectedRows = variableNamingTable.getSelectedRows();
        editParameterButton.setEnabled(selectedRows.length == 1);
        removeParameterButton.setEnabled(selectedRows.length > 0);
    }

    private void initialize() {
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


        //table
        variableNamingTableModel = buildVariableNamingTableModel();
        variableNamingTable.setModel(variableNamingTableModel);
        variableNamingTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        variableNamingTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        Utilities.setRowSorter(variableNamingTable, variableNamingTableModel, new int[] {0}, new boolean[] {true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<CassandraNamedParameter>>) variableNamingTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);



        addParameterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MutablePair<String, String> columnAlias = new MutablePair<>();
                ContextVariableNamingDialog dialog = new ContextVariableNamingDialog(CassandraAssertionPropertiesDialog.this, columnAlias);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    columnAlias = dialog.getData(columnAlias);
                    if(columnAlias != null) {
                        try {
                            variableNamingMap.put(columnAlias.getKey(), columnAlias.getValue());
                            variableNamingTableModel.setRows(getNamedMappingList());
                        } catch(Exception ex) {
                            JOptionPane.showMessageDialog(CassandraAssertionPropertiesDialog.this, "Failed to save new Cassandra named parameter.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        editParameterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int rowIndex = rowSorter.convertRowIndexToModel(variableNamingTable.getSelectedRow());
                MutablePair<String, String> columnAlias = variableNamingTableModel.getRowObject(rowIndex);
                ContextVariableNamingDialog dialog =
                        new ContextVariableNamingDialog(CassandraAssertionPropertiesDialog.this, columnAlias);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if(dialog.isConfirmed()){
                    columnAlias = dialog.getData(columnAlias);
                    variableNamingMap.put(columnAlias.getKey(), columnAlias.getValue()); //this is the problem
                    variableNamingTableModel.setRowObject(rowIndex, columnAlias);
                }
            }
        });

        removeParameterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int rowIndex = rowSorter.convertRowIndexToModel(variableNamingTable.getSelectedRow());
                MutablePair<String, String> cassandraNamedParameter = variableNamingTableModel.getRowObject(rowIndex);
                variableNamingTableModel.removeRow(cassandraNamedParameter);
                variableNamingMap.remove(cassandraNamedParameter.getKey());
                variableNamingTableModel.setRows(getNamedMappingList());
            }
        });

        cqlQueryTextArea.setDocument(new MaxLengthDocument(JdbcAdmin.MAX_QUERY_LENGTH));

        final RunOnChangeListener connectionListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
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

        okButton.addActionListener(new ActionListener() {
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

        populateConnectionCombobox();

        addParameterButton.setEnabled(true);

        setContentPane(mainPanel);
        pack();

        enableDisableComponents();
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
        prefixTextField.setText(assertion.getPrefix());

        variableNamingMap = assertion.getNamingMap();
        List<MutablePair<String, String>> namedMapingList = getNamedMappingList();
        variableNamingTableModel.setRows(namedMapingList);

    }

    private void viewToModel(final CassandraQueryAssertion assertion) {

        assertion.setConnectionName((String)connectionComboBox.getSelectedItem());

        assertion.setQueryDocument(cqlQueryTextArea.getText());

        assertion.setFailIfNoResults(failIfNoResultsCheckBox.isSelected());
        assertion.setPrefix(prefixTextField.getText());
        variableNamingMap = new HashMap<>();
        for(MutablePair<String,String> pair : variableNamingTableModel.getRows()) {
            variableNamingMap.put(pair.left, pair.right);
        }
        assertion.setNamingMap(variableNamingMap);

    }

    private void populateConnectionCombobox() {
        String selectedConnection = null;

        try {
           DefaultComboBoxModel model = new DefaultComboBoxModel();
           CassandraConnectionManagerAdmin cassandraConnectionEntityAdmin = Registry.getDefault().getCassandraConnectionAdmin();

           for(String connectionName :cassandraConnectionEntityAdmin.getAllCassandraConnectionNames()) {
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
        boolean enabled = isNonEmptyRequiredTextField(cqlQueryTextArea.getText()) &&
                connectionComboBox.getSelectedItem() != null &&
                isNonEmptyRequiredTextField(prefixTextField.getText());

        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private void doCancel() {
        CassandraAssertionPropertiesDialog.this.dispose();
    }

}