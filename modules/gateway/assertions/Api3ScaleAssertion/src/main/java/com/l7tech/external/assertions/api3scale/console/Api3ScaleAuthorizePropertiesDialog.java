package com.l7tech.external.assertions.api3scale.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.ResolveContextVariablesPanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.api3scale.Api3ScaleAdmin;
import com.l7tech.external.assertions.api3scale.Api3ScaleAuthorizeAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

public class Api3ScaleAuthorizePropertiesDialog extends AssertionPropertiesOkCancelSupport<Api3ScaleAuthorizeAssertion> {

    private JPanel propertyPanel;
    private JTextField privateKeyTextField;
    private TargetVariablePanel targetVariablePanel;
    private JTextField applicationIdTextField;
    private JTextField applicationKeyTextField;
    private JCheckBox usageCheckBox;
    private JTable usageTable;
    private JButton usageAddButton;
    private JButton usageRemoveButton;
    private JButton usageEditButton;
    private JButton testButton;
    private JScrollPane usageScrollPane;
    private JTextField serverTextField;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(Api3ScaleAuthorizePropertiesDialog.class.getName());
    private InputValidator validators;

    private Map<String,String> usages = new HashMap<String,String>();
    private java.util.List<String> metrics = new ArrayList<String>();
    private final UsageTableModel tableModel = new UsageTableModel();
    private static final Logger logger = Logger.getLogger(Api3ScaleAuthorizePropertiesDialog.class.getName());



    public Api3ScaleAuthorizePropertiesDialog(final Window parent, final Api3ScaleAuthorizeAssertion assertion) {
        super(Api3ScaleAuthorizeAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        targetVariablePanel.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                endableDisableComponents();
            }
        });
        targetVariablePanel.setAcceptEmpty(true);

        usageCheckBox.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                endableDisableComponents();
            }
        });
        usageAddButton.addActionListener(new RunOnChangeListener(){
            @Override
            public void run(){
                final String metric = null, value = null;
                if (edit(metric,value)) {
                    final int last = metrics.size() - 1;
                    tableModel.fireTableRowsInserted(last, last);
                }
            }
        });
        usageRemoveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDeleteUsage();
            }
        });
        usageEditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int sel = usageTable.getSelectedRow();
                if (sel == -1) return;
                if (edit(metrics.get(sel),usages.get(metrics.get(sel)))) tableModel.fireTableRowsUpdated(sel, sel);
            }
        });
        getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
//        testButton.setVisible(false);
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doTest();
            }
        });

        usageTable.setModel(tableModel);
        usageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e){
                endableDisableComponents();
            }
        });
        Utilities.setDoubleClickAction(usageTable, usageEditButton);
        metrics.clear();
        usages.clear();

        validators = new InputValidator( this, getTitle() );
        validators.constrainTextFieldToBeNonEmpty(getPropertyValue("server"), serverTextField, null);
        validators.constrainTextFieldToBeNonEmpty(getPropertyValue("privateKey"), privateKeyTextField, null);
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return targetVariablePanel.getErrorMessage();
            }
        });
        validators.constrainTextFieldToBeNonEmpty(getPropertyValue("applicationId"),applicationIdTextField, null);
        validators.constrainTextFieldToBeNonEmpty(getPropertyValue("applicationKey"),applicationKeyTextField, null);
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!usageCheckBox.isSelected())
                    return null;
                if (usageTable.getModel().getRowCount() < 1)
                    return resourceBundle.getString("usageTableEmptyError");
                return null;
            }
        });
    }

    private void doTest() {
        final Api3ScaleAuthorizeAssertion ass = new Api3ScaleAuthorizeAssertion();
        ass.setApplicationID(applicationIdTextField.getText());
        ass.setPrivateKey(privateKeyTextField.getText());
        ass.setApplicationKey(applicationKeyTextField.getText());
        Map<String, Object> contextVars = null;
        if(ass.getVariablesUsed().length > 0){
            ResolveContextVariablesPanel dlg = new ResolveContextVariablesPanel(this,ass.getVariablesUsed());
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
            dlg.setVisible(true);
            if(!dlg.getWasOked())
                return;
            contextVars = dlg.getValues();
        }
        try {
            Api3ScaleAdmin admin = Registry.getDefault().getExtensionInterface(Api3ScaleAdmin.class, null);
            String strResponse = admin.testAuthorize(ass,contextVars);
            DialogDisplayer.showMessageDialog(testButton,
                getPropertyValue("test.message.success")+'\n'+strResponse,
                getPropertyValue("test.title"),
                JOptionPane.INFORMATION_MESSAGE, null);
        } catch (Exception e) {                                //Api3ScaleAdmin.Api3ScaleTestException
            DialogDisplayer.showMessageDialog(testButton,
                 getPropertyValue("test.message.fail")+ '\n' + e.getMessage(),
                 getPropertyValue("test.title"),
                 JOptionPane.ERROR_MESSAGE, null);
        }

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

    private void endableDisableComponents() {
        usageScrollPane.setEnabled(usageCheckBox.isSelected());
        usageTable.setEnabled(usageCheckBox.isSelected());
        final int sel = usageTable.getSelectedRow();
        usageAddButton.setEnabled(usageCheckBox.isSelected());
        usageRemoveButton.setEnabled(usageCheckBox.isSelected() && sel >= 0);
        usageEditButton.setEnabled(usageCheckBox.isSelected() && sel >= 0);
    }

    @Override
    public Api3ScaleAuthorizeAssertion getData(Api3ScaleAuthorizeAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }
        assertion.setServer(serverTextField.getText());
        assertion.setPrivateKey(privateKeyTextField.getText());
        assertion.setApplicationID(applicationIdTextField.getText());
        assertion.setApplicationKey(applicationKeyTextField.getText());
        assertion.setOutputPrefix(VariablePrefixUtil.fixVariableName(targetVariablePanel.getVariable()));

        if(usageCheckBox.isSelected()){
            assertion.setUsage(usages);
        }else {
            assertion.setUsage(null);
        }

        return assertion;
    }

    @Override
    public void setData(Api3ScaleAuthorizeAssertion assertion) {
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

        final String privateKey = assertion.getPrivateKey();
        if(privateKey != null && !privateKey.trim().isEmpty()){
            privateKeyTextField.setText(privateKey);
        }

        final String appId = assertion.getApplicationID();
        if(appId != null && !appId.trim().isEmpty()){
            applicationIdTextField.setText(appId);
        }

        final String appKey = assertion.getApplicationKey();
        if(appKey != null && !appKey.trim().isEmpty()){
            applicationKeyTextField.setText(appKey);
        }

        targetVariablePanel.setVariable(assertion.getPrefix());

        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());

        if(assertion.getUsage()!=null){
            usageCheckBox.setSelected(true);
            usages = assertion.getUsage();
        }
        Collections.addAll(metrics,usages.keySet().toArray(new String[usages.size()]));
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
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
