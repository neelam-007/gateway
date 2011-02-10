package com.l7tech.external.assertions.api3scale.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.api3scale.Api3ScaleReportAssertion;
import com.l7tech.external.assertions.api3scale.Api3ScaleTransaction;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class Api3ScaleReportPropertiesDialog extends AssertionPropertiesOkCancelSupport<Api3ScaleReportAssertion> {

    public Api3ScaleReportPropertiesDialog(final Window parent, final Api3ScaleReportAssertion assertion) {
        super(Api3ScaleReportAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        transactionTable.setModel(tableModel);
        transactions.clear();
        transactionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e){
                enableDisableComponents();
            }
        });

        addButton.addActionListener(new RunOnChangeListener(){
            @Override
            public void run(){
                final Api3ScaleTransaction transaction = new Api3ScaleTransaction();
                if (edit(transaction)) {
                    transactions.add(transaction);
                    final int last = transactions.size() - 1;
                    tableModel.fireTableRowsInserted(last, last);
                }
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDeleteTransaction();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int sel = transactionTable.getSelectedRow();
                if (sel == -1) return;
                if (edit(transactions.get(sel))) tableModel.fireTableRowsUpdated(sel, sel);
            }
        });

        Utilities.setDoubleClickAction(transactionTable, propertiesButton);
        enableDisableComponents();

    }

    private void enableDisableComponents() {
        final boolean attributeSelected = transactionTable.getSelectedRow() != -1;
        propertiesButton.setEnabled(attributeSelected);
        removeButton.setEnabled(attributeSelected);
    }

    @Override
    public Api3ScaleReportAssertion getData(Api3ScaleReportAssertion assertion) throws ValidationException {
        validateData();
        assertion.setTransactions(transactions.toArray(new Api3ScaleTransaction[transactions.size()]));
        return assertion;
    }

    @Override
    public void setData(Api3ScaleReportAssertion assertion) {
        transactions.clear();
        Collections.addAll(transactions,assertion.getTransactions());
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }

    private void validateData() throws ValidationException {
        if(transactions.isEmpty())
            throw new ValidationException("Transactions cannot be empty.");
    }

    private boolean edit(Api3ScaleTransaction trans) {
        TransactionDialog dlg = new TransactionDialog(this, trans);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setVisible(true);
        return dlg.isWasOKed();
    }

    private String getPropertyValue(String propKey){
        String propertyName = resourceBundle.getString(propKey);
        if(propertyName.charAt(propertyName.length() - 1) == ':'){
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

    private void doDeleteTransaction() {
        final int sel = transactionTable.getSelectedRow();
        if (sel == -1) return;
        final Api3ScaleTransaction found = transactions.get(sel);
        DialogDisplayer.showConfirmDialog(this,
                        MessageFormat.format("Are you sure you want to remove the transaction for \"{0}\"", found.getAppId()),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            transactions.remove(sel);
                            tableModel.fireTableRowsDeleted(sel, sel);
                        }
                    }
                });
    }

    private class TransactionTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return transactions.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Api3ScaleTransaction transaction = transactions.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return transaction.getAppId();
                case 1:
                    Set<String> keys = transaction.getMetrics().keySet();
                    StringBuilder builder = new StringBuilder();
                    for(String key: keys )
                    {
                        builder.append(keys);
                        builder.append("=");
                        builder.append(transaction.getMetrics().get(key));
                        builder.append(";");
                    }
                    return builder.toString();
                case 2:
                    return transaction.getTimestamp();
                default:
                    throw new IllegalArgumentException("No such column " + columnIndex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return "Application ID";
                case 1:
                    return "Usage";
                case 2:
                    return "Timestamp";
                default:
                    throw new IllegalArgumentException("No such column " + column);
            }
        }
    }

    private JPanel propertyPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JTable transactionTable;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(Api3ScaleReportPropertiesDialog.class.getName());
    private List<Api3ScaleTransaction> transactions = new ArrayList<Api3ScaleTransaction>();
    private final TransactionTableModel tableModel = new TransactionTableModel();
}
