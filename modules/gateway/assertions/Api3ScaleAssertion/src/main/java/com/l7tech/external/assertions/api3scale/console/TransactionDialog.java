package com.l7tech.external.assertions.api3scale.console;

import com.l7tech.external.assertions.api3scale.Api3ScaleTransaction;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
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

/**
 * Something to edit a mapping between an LDAP attribute name and a context variable.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 7, 2007<br/>
 */
public class TransactionDialog extends JDialog {
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(TransactionDialog.class.getName());

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton OKButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton addButton;
    private JTextField applicationIdTextField;
    private JTextField timestampTextField;
    private JTable usageTable;
    private final UsageTableModel tableModel = new UsageTableModel();
    
    private Map<String,String> usages = new HashMap<String,String>();
    private List<String> metrics = new ArrayList<String>();

    private Api3ScaleTransaction data;
    private boolean wasOKed = false;
    private InputValidator validator;


    public TransactionDialog(Dialog owner, Api3ScaleTransaction data) throws HeadlessException {
        super(owner, "Transaction", true);
        this.data = data;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        Utilities.setEscKeyStrokeDisposes( this );

        validator = new InputValidator(this, "Transaction");
        validator.attachToButton(OKButton, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewToModel(); 
                ok();
            }
        });

        //validator.constrainTextFieldToBeNonEmpty(getPropertyValue("appID"), applicationIdTextField, null);
        validator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if(usages.isEmpty()){
                    return "Usages must contain values.";
                }
                return null;
            }
        });

        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });

        usageTable.setModel(tableModel);
        usageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e){
                enableDisableComponents();
            }
        });

        addButton.addActionListener(new RunOnChangeListener(){
            @Override
            public void run(){
                final String metric = null, value = null;
                if (edit(metric, value)) {
                    final int last = usages.size() - 1;
                    tableModel.fireTableRowsInserted(last, last);
                }
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDeleteUsage();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int sel = usageTable.getSelectedRow();
                if (sel == -1) return;
                if (edit(metrics.get(sel),usages.get(metrics.get(sel)))) tableModel.fireTableRowsUpdated(sel, sel);
            }
        });

        Utilities.setDoubleClickAction(usageTable, editButton);
        metrics.clear();
        usages.clear();
        
        modelToView();
        validator.validate();
        enableDisableComponents();

    }

    private void enableDisableComponents() {
        final int sel = usageTable.getSelectedRow();
        editButton.setEnabled(sel >= 0);
        removeButton.setEnabled(sel >= 0);
    }

    private void ok() {
        wasOKed = true;
        dispose();
    }

    private void modelToView() {
        applicationIdTextField.setText(data.getAppId());
        applicationIdTextField.setEnabled(false);
        timestampTextField.setText(data.getTimestamp());
        usages = data.getMetrics();
        Collections.addAll(metrics,usages.keySet().toArray(new String[usages.size()]));
    }

    public void viewToModel() {        
        validator.validate();
        
        data.setAppId(applicationIdTextField.getText());
        data.setTimestamp(timestampTextField.getText());
        data.setMetrics(usages);
    }

    public boolean isWasOKed() {
        return wasOKed;
    }


    private String getPropertyValue(String propKey){
        String propertyName = resourceBundle.getString(propKey);
        if(propertyName.charAt(propertyName.length() - 1) == ':'){
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

    private boolean edit(String metric, String value) {
        UsageDialog dlg = new UsageDialog(this, metric, value);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setVisible(true);
        if(dlg.isWasOKed()){
            String newMetric = dlg.getMetric();
            String newValue = dlg.getValue();
            if(metric != null){
                metrics.set(metrics.indexOf(metric),newMetric);
                usages.remove(metric);
                usages.put(newMetric,newValue);
            }
            else {
                usages.put(newMetric,newValue);
                metrics.add(newMetric);
            }
            return true;
        }
        return false;
    }

    private void doDeleteUsage() {
        final int sel = usageTable.getSelectedRow();
        if (sel == -1) return;
        final String found = metrics.get(sel);
        DialogDisplayer.showConfirmDialog(this,
                        MessageFormat.format("Are you sure you want to remove the usage for \"{0}\"", found),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            metrics.remove(sel);
                            usages.remove(found);
                            tableModel.fireTableRowsDeleted(sel, sel);
                        }
                    }
                });
    }

    private class UsageTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return data.getMetrics().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String metric = metrics.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return metric;
                case 1:
                    return usages.get(metric);
                default:
                    throw new IllegalArgumentException("No such column " + columnIndex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return "Metric";
                case 1:
                    return "Value";
                default:
                    throw new IllegalArgumentException("No such column " + column);
            }
        }
    }
}
