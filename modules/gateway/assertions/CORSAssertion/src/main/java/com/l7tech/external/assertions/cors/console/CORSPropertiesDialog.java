package com.l7tech.external.assertions.cors.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CORSPropertiesDialog extends AssertionPropertiesOkCancelSupport<CORSAssertion> {

    private JPanel propertyPanel;
    private JTextField responseCacheAgeTextField;
    private JCheckBox responseCacheAgeCheckBox;
    private JRadioButton originsAllRadioButton;
    private JRadioButton originsListRadioButton;
    private JTable originsTable;
    private JButton originsListRemove;
    private JButton originsListAdd;
    private JPanel originsListPanel;
    private JLabel responseCacheAgeUnit;
    private JRadioButton headersAllRadioButton;
    private JRadioButton headersListRadioButton;
    private JTable headersTable;
    private JPanel headersListPanel;
    private JButton headersListAdd;
    private JButton headersListRemove;
    private JPanel variablePrefixPanel;
    private JButton exposedHeadersListAdd;
    private JButton exposedHeadersListRemove;
    private JTable exposedHeadersTable;
    private JCheckBox requireCorsCheckBox;
    private JCheckBox getCheckBox;
    private JCheckBox putCheckBox;
    private JCheckBox postCheckBox;
    private JCheckBox headCheckBox;
    private JCheckBox deleteCheckBox;
    private JCheckBox optionsCheckBox;
    private JCheckBox patchCheckBox;
    private JCheckBox supportsCredentialsCheckBox;

    private ResourceBundle resourceBundle = ResourceBundle.getBundle(CORSPropertiesDialog.class.getName());
    private InputValidator validators;
    private TargetVariablePanel variablePrefixTextField;
    private DefaultTableModel originsTableModel;
    private DefaultTableModel headersTableModel;
    private DefaultTableModel exposedHeadersTableModel;

    public CORSPropertiesDialog(final Window parent, final CORSAssertion assertion) {
        super(CORSAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {

        RunOnChangeListener enableDisableListener = new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        });

        ButtonGroup originsButtonGroup = new ButtonGroup();
        originsButtonGroup.add(originsAllRadioButton);
        originsButtonGroup.add(originsListRadioButton);
        originsAllRadioButton.addActionListener(enableDisableListener);
        originsListRadioButton.addActionListener(enableDisableListener);
        originsTableModel = new DefaultTableModel(0,1);
        originsTable.setModel(originsTableModel);
        originsTable.setShowGrid(false);
        originsTable.setTableHeader(null);
        originsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        originsTable.setCellSelectionEnabled(true);
        originsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        originsTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        originsListAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAdd(originsTableModel,originsTable);
            }
        });
        originsListRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemove(originsTableModel,originsTable);
            }
        });

        ButtonGroup headersButtonGroup = new ButtonGroup();
        headersButtonGroup.add(headersAllRadioButton);
        headersButtonGroup.add(headersListRadioButton);
        headersAllRadioButton.addActionListener(enableDisableListener);
        headersListRadioButton.addActionListener(enableDisableListener);
        headersTableModel = new DefaultTableModel(0,1);
        headersTable.setModel(headersTableModel);
        headersTable.setShowGrid(false);
        headersTable.setTableHeader(null);
        headersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        headersTable.setCellSelectionEnabled(true);
        headersTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        headersTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        headersListAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAdd(headersTableModel,headersTable);
            }
        });
        headersListRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemove(headersTableModel,headersTable);
            }
        });

        exposedHeadersTableModel = new DefaultTableModel(0,1);
        exposedHeadersTable.setModel(exposedHeadersTableModel);
        exposedHeadersTable.setShowGrid(false);
        exposedHeadersTable.setTableHeader(null);
        exposedHeadersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        exposedHeadersTable.setCellSelectionEnabled(true);
        exposedHeadersTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        exposedHeadersTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        exposedHeadersListAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAdd(exposedHeadersTableModel,exposedHeadersTable);
            }
        });
        exposedHeadersListRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemove(exposedHeadersTableModel,exposedHeadersTable);
            }
        });

        responseCacheAgeCheckBox.addActionListener(enableDisableListener);

        variablePrefixTextField = new TargetVariablePanel();
        variablePrefixPanel.setLayout(new BorderLayout());
        variablePrefixPanel.add(variablePrefixTextField, BorderLayout.CENTER);
        variablePrefixTextField.setAcceptEmpty(true);

        validators = new InputValidator( this, getTitle() );
        validators.addRule(validators.constrainTextFieldToNumberRange(resourceBundle.getString("responseCacheAgeTitle"), responseCacheAgeTextField,0,Long.MAX_VALUE));
        validators.addRule(new InputValidator.ComponentValidationRule(variablePrefixTextField) {
            @Override
            public String getValidationError() {
                return variablePrefixTextField.getErrorMessage();
            }
        });

        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!getCheckBox.isSelected() && !putCheckBox.isSelected() && !postCheckBox.isSelected()
                        && !headCheckBox.isSelected() && !deleteCheckBox.isSelected()
                        && !patchCheckBox.isSelected() && !optionsCheckBox.isSelected()) {
                    return resourceBundle.getString("noMethodsEnabledError");
                }

                return null;
            }
        });

        return propertyPanel;
    }

    private void onRemove(DefaultTableModel tableModel, JTable table) {
        int idx = table.getSelectedRow();
        table.clearSelection();
        tableModel.removeRow(idx);
    }

    private void onAdd(DefaultTableModel tableModel, JTable table) {
        int index = tableModel.getRowCount();
        tableModel.insertRow(index, new Object[]{""});
        table.changeSelection(index,0,false,false);
        table.editCellAt(index, 0);
        table.requestFocus();
    }

    private void enableDisableComponents() {
        Utilities.setEnabled(headersListPanel,headersListRadioButton.isSelected());
        if(headersListRadioButton.isSelected()){
            headersListRemove.setEnabled(headersTable.getSelectedRowCount()>0);
        }

        Utilities.setEnabled(originsListPanel,originsListRadioButton.isSelected());
        if(originsListRadioButton.isSelected()){
            originsListRemove.setEnabled(originsTable.getSelectedRowCount() > 0);
        }

        exposedHeadersListRemove.setEnabled(exposedHeadersTable.getSelectedRowCount() > 0);

        responseCacheAgeTextField.setEnabled(responseCacheAgeCheckBox.isSelected());
        responseCacheAgeUnit.setEnabled(responseCacheAgeCheckBox.isSelected());
    }

    @Override
    public CORSAssertion getData( final CORSAssertion assertion ) throws ValidationException {
        final String error = validators.validate();

        if(error != null) {
            throw new ValidationException(error);
        }

        if(originsAllRadioButton.isSelected()){
            assertion.setAcceptedOrigins(null);
        }else{
            List<String> origins = new ArrayList<>();
            for(int i=0; i < originsTable.getRowCount(); i++){
                origins.add(originsTable.getValueAt(i,0).toString());
            }
            assertion.setAcceptedOrigins(origins);
        }

        if (headersAllRadioButton.isSelected()) {
            assertion.setAcceptedHeaders(null);
        } else {
            List<String> headers = new ArrayList<>();
            for(int i=0; i < headersTable.getRowCount(); i++){
                headers.add(headersTable.getValueAt(i,0).toString());
            }
            assertion.setAcceptedHeaders(headers);
        }

        if (0 == exposedHeadersTable.getRowCount()) {
            assertion.setExposedHeaders(null);
        } else {
            List<String> headers = new ArrayList<>();
            for(int i=0; i < exposedHeadersTable.getRowCount(); i++){
                headers.add(exposedHeadersTable.getValueAt(i,0).toString());
            }
            assertion.setExposedHeaders(headers);
        }

        assertion.setResponseCacheTime(responseCacheAgeCheckBox.isSelected()?Long.parseLong(responseCacheAgeTextField.getText()):null);
        assertion.setVariablePrefix(variablePrefixTextField.getVariable().trim());
        assertion.setRequireCors(requireCorsCheckBox.isSelected());
        assertion.setSupportsCredentials(supportsCredentialsCheckBox.isSelected());

        ArrayList<String> methods = new ArrayList<>();

        if (getCheckBox.isSelected())
            methods.add("GET");
        if (putCheckBox.isSelected())
            methods.add("PUT");
        if (postCheckBox.isSelected())
            methods.add("POST");
        if (headCheckBox.isSelected())
            methods.add("HEAD");
        if (deleteCheckBox.isSelected())
            methods.add("DELETE");
        if (patchCheckBox.isSelected())
            methods.add("PATCH");
        if (optionsCheckBox.isSelected())
            methods.add("OPTIONS");

        assertion.setAcceptedMethods(methods);

        return assertion;
    }

    @Override
    public void setData( final CORSAssertion assertion ) {
        headersAllRadioButton.setSelected(true);
        if(assertion.getAcceptedHeaders()!=null){
            headersListRadioButton.setSelected(true);
            for(String header: assertion.getAcceptedHeaders()) {
                headersTableModel.addRow(new Object[]{header});
            }
        }

        originsAllRadioButton.setSelected(true);
        if(assertion.getAcceptedOrigins()!=null){
            originsListRadioButton.setSelected(true);
            for(String origin: assertion.getAcceptedOrigins()) {
                originsTableModel.addRow(new Object[]{origin});
            }
        }

        if (null != assertion.getExposedHeaders()) {
            for(String origin: assertion.getExposedHeaders()) {
                exposedHeadersTableModel.addRow(new Object[]{origin});
            }
        }

        responseCacheAgeCheckBox.setSelected(assertion.getResponseCacheTime() != null);
        responseCacheAgeTextField.setText(responseCacheAgeCheckBox.isSelected()?assertion.getResponseCacheTime().toString():"");

        this.variablePrefixTextField.setVariable( assertion.getVariablePrefix()==null ? "" : assertion.getVariablePrefix() );
        this.variablePrefixTextField.setAssertion(assertion,getPreviousAssertion());

        String[] suffixes = CORSAssertion.VARIABLE_SUFFIXES.toArray( new String[CORSAssertion.VARIABLE_SUFFIXES.size()] );
        variablePrefixTextField.setSuffixes(suffixes);

        requireCorsCheckBox.setSelected(assertion.isRequireCors());
        supportsCredentialsCheckBox.setSelected(assertion.isSupportsCredentials());

        if (assertion.getAcceptedMethods() != null) {
            getCheckBox.setSelected(assertion.getAcceptedMethods().contains("GET"));
            putCheckBox.setSelected(assertion.getAcceptedMethods().contains("PUT"));
            postCheckBox.setSelected(assertion.getAcceptedMethods().contains("POST"));
            headCheckBox.setSelected(assertion.getAcceptedMethods().contains("HEAD"));
            deleteCheckBox.setSelected(assertion.getAcceptedMethods().contains("DELETE"));
            patchCheckBox.setSelected(assertion.getAcceptedMethods().contains("PATCH"));
            optionsCheckBox.setSelected(assertion.getAcceptedMethods().contains("OPTIONS"));
        }

        enableDisableComponents();
        pack();
    }
}
