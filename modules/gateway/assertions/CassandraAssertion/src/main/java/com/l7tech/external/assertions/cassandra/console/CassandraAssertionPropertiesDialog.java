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

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
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
    private JTable parameterTable;
    private TableRowSorter<SimpleTableModel<CassandraNamedParameter>> rowSorter;
    private SimpleTableModel<CassandraNamedParameter> parameterTableModel;
    private JButton addParameterButton;
    private JButton removeParameterButton;
    private JButton editParameterButton;
    private CassandraQueryAssertion assertion;
    private boolean confirmed;

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

    private SimpleTableModel<CassandraNamedParameter> buildServersTableModel() {
        return TableUtil.configureTable(
                parameterTable,
                TableUtil.column("Name", 40, 200, 1000000, new Functions.Unary<String, CassandraNamedParameter>() {
                    @Override
                    public String call(CassandraNamedParameter cassandraNamedParameter) {
                        return cassandraNamedParameter.getParameterName();
                    }
                }, String.class),
                TableUtil.column("Value", 40, 100, 180, new Functions.Unary<String, CassandraNamedParameter>() {
                    @Override
                    public String call(CassandraNamedParameter cassandraNamedParameter) {
                        return cassandraNamedParameter.getParameterValue();
                    }
                }, String.class),
                TableUtil.column("Type", 40, 100, 180, new Functions.Unary<String, CassandraNamedParameter>() {
                    @Override
                    public String call(CassandraNamedParameter cassandraNamedParameter) {
                        return cassandraNamedParameter.getParameterDataType();
                    }
                }, String.class)
        );
    }

    private void enableDisableComponents() {
        final int[] selectedRows = parameterTable.getSelectedRows();
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
        parameterTableModel = buildServersTableModel();
        parameterTable.setModel(parameterTableModel);
        parameterTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        parameterTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        Utilities.setRowSorter(parameterTable, parameterTableModel, new int[] {0}, new boolean[] {true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<CassandraNamedParameter>>)parameterTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);



        addParameterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CassandraNamedParameter cassandraNamedParameter = new CassandraNamedParameter();
                CassandraNamedParameterDialog dialog = new CassandraNamedParameterDialog(CassandraAssertionPropertiesDialog.this, cassandraNamedParameter);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    cassandraNamedParameter = dialog.getData(cassandraNamedParameter);
                    if(cassandraNamedParameter != null) {
                        try {
                            assertion.getNamedParameterList().add(cassandraNamedParameter);
                            parameterTableModel.setRows(assertion.getNamedParameterList());
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
                CassandraNamedParameter cassandraNamedParameter = parameterTableModel.getRowObject(rowSorter.convertRowIndexToModel(parameterTable.getSelectedRow()));
                CassandraNamedParameterDialog dialog =
                        new CassandraNamedParameterDialog(CassandraAssertionPropertiesDialog.this, cassandraNamedParameter);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if(dialog.isConfirmed()){
                    cassandraNamedParameter = dialog.getData(cassandraNamedParameter);
                    assertion.getNamedParameterList().set(rowSorter.convertRowIndexToModel(parameterTable.getSelectedRow()), cassandraNamedParameter); //this is the problem
                    parameterTableModel.setRows(assertion.getNamedParameterList());
                }
            }
        });

        removeParameterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CassandraNamedParameter cassandraNamedParameter = parameterTableModel.getRowObject(rowSorter.convertRowIndexToModel(parameterTable.getSelectedRow()));
                parameterTableModel.removeRow(cassandraNamedParameter);
                assertion.getNamedParameterList().remove(cassandraNamedParameter);
                parameterTableModel.setRows(assertion.getNamedParameterList());
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

        parameterTableModel.setRows(assertion.getNamedParameterList());
        enableDisableComponents();
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

    }

    private void viewToModel(final CassandraQueryAssertion assertion) {

        assertion.setConnectionName((String)connectionComboBox.getSelectedItem());

        assertion.setQueryDocument(cqlQueryTextArea.getText());

        assertion.setFailIfNoResults(failIfNoResultsCheckBox.isSelected());
        assertion.setPrefix(prefixTextField.getText());

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