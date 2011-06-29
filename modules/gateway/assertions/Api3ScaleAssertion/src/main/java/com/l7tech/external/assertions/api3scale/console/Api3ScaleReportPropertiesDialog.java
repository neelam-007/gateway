package com.l7tech.external.assertions.api3scale.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.api3scale.Api3ScaleReportAssertion;
import com.l7tech.external.assertions.api3scale.Api3ScaleTransactions;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import net.threescale.api.ApiTransaction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.transaction.Transaction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class Api3ScaleReportPropertiesDialog extends AssertionPropertiesOkCancelSupport<Api3ScaleReportAssertion> {
    private static final Logger logger = Logger.getLogger(Api3ScaleReportPropertiesDialog.class.getName());

    private JPanel propertyPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JTable usageTable;
    private JTextField privateKeyTextField;
    private JTextField appIdTextField;
    private JTextField serverTextField;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(Api3ScaleReportPropertiesDialog.class.getName());
    private Map<String,String> usages = new HashMap<String,String>();
    private List<String> metrics = new ArrayList<String>();
    private final UsageTableModel tableModel = new UsageTableModel();

    private InputValidator validators;

    public Api3ScaleReportPropertiesDialog(final Window parent, final Api3ScaleReportAssertion assertion) {
        super(Api3ScaleReportAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

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
                if (edit(metric,value)) {
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

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int sel = usageTable.getSelectedRow();
                if (sel == -1) return;
                if (edit(metrics.get(sel),usages.get(metrics.get(sel)))) tableModel.fireTableRowsUpdated(sel, sel);
            }
        });

        metrics.clear();
        usages.clear();

        validators = new InputValidator( this, getTitle() );
        validators.constrainTextFieldToBeNonEmpty(getPropertyValue("privateKey"), privateKeyTextField, null);
        validators.constrainTextFieldToBeNonEmpty(getPropertyValue("applicationId"), appIdTextField, null);
        validators.constrainTextFieldToBeNonEmpty(getPropertyValue("server"), serverTextField, null);
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (usageTable.getModel().getRowCount() < 1)
                    return resourceBundle.getString("usageTableEmptyError");
                return null;
            }
        });

        Utilities.setDoubleClickAction(usageTable, propertiesButton);
        enableDisableComponents();

    }

    private void enableDisableComponents() {
        final boolean attributeSelected = usageTable.getSelectedRow() != -1;
        propertiesButton.setEnabled(attributeSelected);
        removeButton.setEnabled(attributeSelected);
    }

    @Override
    public Api3ScaleReportAssertion getData(Api3ScaleReportAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }

        assertion.setTransactionUsages(usages);
        assertion.setApplicationId(appIdTextField.getText());
        assertion.setPrivateKey(privateKeyTextField.getText());
        assertion.setServer(serverTextField.getText());
        return assertion;
    }

    @Override
    public void setData(Api3ScaleReportAssertion assertion) {
        if(assertion.getTransactionUsages()!=null){
            usages = assertion.getTransactionUsages();
        }
        Collections.addAll(metrics,usages.keySet().toArray(new String[usages.size()]));

        privateKeyTextField.setText(assertion.getPrivateKey());
        appIdTextField.setText(assertion.getApplicationId());

        final String server = assertion.getServer();
        if(server != null && !server.trim().isEmpty()){
            serverTextField.setText(server);
        }else{
            ClusterProperty prop = null;
            try {
                prop = Registry.getDefault().getClusterStatusAdmin().findPropertyByName("gateway.3scale.reportingServer");
            } catch (FindException e) {
                logger.warning("Error getting server value: " + e.getMessage());
            }
            if (prop == null) {
                Collection<ClusterPropertyDescriptor> descriptors = Registry.getDefault().getClusterStatusAdmin().getAllPropertyDescriptors();
                for (ClusterPropertyDescriptor desc : descriptors) {
                    if (desc.getName().equals("gateway.3scale.reportingServer"))
                        serverTextField.setText(desc.getDefaultValue());
                }
            }else{
               serverTextField.setText(prop.getValue());
            }
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
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
                MessageFormat.format(getPropertyValue("remove.usage"), found),
                getPropertyValue("confirm.delete"),
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

    private String getPropertyValue(String propKey){
        String propertyName = resourceBundle.getString(propKey);
        if(propertyName.charAt(propertyName.length() - 1) == ':'){
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

    private class UsageTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return metrics.size();
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
                    throw new IllegalArgumentException(getPropertyValue("noColumn") + columnIndex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return getPropertyValue("metric");
                case 1:
                    return getPropertyValue("value");
                default:
                    throw new IllegalArgumentException(getPropertyValue("noColumn") + column);
            }
        }
    }



}
