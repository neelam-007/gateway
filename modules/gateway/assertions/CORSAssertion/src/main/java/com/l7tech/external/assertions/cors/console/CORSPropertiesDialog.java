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
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class CORSPropertiesDialog extends AssertionPropertiesOkCancelSupport<CORSAssertion> {

    private JPanel propertyPanel;
    private JTextField responseCacheAgeTextField;
    private JCheckBox responeCacheAgeCheckBox;
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
    private JRadioButton exposedHeadersAllRadioButton;
    private JRadioButton exposedHeadersListRadioButton;
    private JPanel exposedHeadersListPanel;
    private JButton exposedHeadersListAdd;
    private JButton exposedHeadersListRemove;
    private JTable exposedHeadersTable;

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

        ButtonGroup exposedHeadersButtonGroup = new ButtonGroup();
        exposedHeadersButtonGroup.add(exposedHeadersAllRadioButton);
        exposedHeadersButtonGroup.add(exposedHeadersListRadioButton);
        exposedHeadersAllRadioButton.addActionListener(enableDisableListener);
        exposedHeadersListRadioButton.addActionListener(enableDisableListener);
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

        responeCacheAgeCheckBox.addActionListener(enableDisableListener);

        variablePrefixTextField = new TargetVariablePanel();
        variablePrefixPanel.setLayout(new BorderLayout());
        variablePrefixPanel.add(variablePrefixTextField, BorderLayout.CENTER);
        variablePrefixTextField.setAcceptEmpty(true);

        validators = new InputValidator( this, getTitle() );
        validators.addRule(validators.constrainTextFieldToNumberRange(resourceBundle.getString("response.cache.age"), responseCacheAgeTextField,0,Long.MAX_VALUE));
        validators.addRule(new InputValidator.ComponentValidationRule(variablePrefixTextField) {
            @Override
            public String getValidationError() {
                return variablePrefixTextField.getErrorMessage();
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

        Utilities.setEnabled(exposedHeadersListPanel,exposedHeadersListRadioButton.isSelected());
        if(exposedHeadersListRadioButton.isSelected()){
            exposedHeadersListRemove.setEnabled(exposedHeadersTable.getSelectedRowCount()>0);
        }

        responseCacheAgeTextField.setEnabled(responeCacheAgeCheckBox.isSelected());
        responseCacheAgeUnit.setEnabled(responeCacheAgeCheckBox.isSelected());

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

        if(headersAllRadioButton.isSelected()){
            assertion.setAcceptedHeaders(null);
        }else{
            List<String> headers = new ArrayList<>();
            for(int i=0; i < headersTable.getRowCount(); i++){
                headers.add(headersTable.getValueAt(i,0).toString());
            }
            assertion.setAcceptedHeaders(headers);
        }

        if(exposedHeadersAllRadioButton.isSelected()){
            assertion.setExposedHeaders(null);
        }else{
            List<String> headers = new ArrayList<>();
            for(int i=0; i < exposedHeadersTable.getRowCount(); i++){
                headers.add(exposedHeadersTable.getValueAt(i,0).toString());
            }
            assertion.setExposedHeaders(headers);
        }

        assertion.setResponseCacheTime(responeCacheAgeCheckBox.isSelected()?Long.parseLong(responseCacheAgeTextField.getText()):null);
        assertion.setVariablePrefix(variablePrefixTextField.getVariable().trim());
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

        exposedHeadersAllRadioButton.setSelected(true);
        if(assertion.getExposedHeaders()!=null){
            exposedHeadersListRadioButton.setSelected(true);
            for(String origin: assertion.getExposedHeaders()) {
                exposedHeadersTableModel.addRow(new Object[]{origin});
            }
        }

        responeCacheAgeCheckBox.setSelected(assertion.getResponseCacheTime()!=null);
        responseCacheAgeTextField.setText(responeCacheAgeCheckBox.isSelected()?assertion.getResponseCacheTime().toString():"");

        this.variablePrefixTextField.setVariable( assertion.getVariablePrefix()==null ? "" : assertion.getVariablePrefix() );
        this.variablePrefixTextField.setAssertion(assertion,getPreviousAssertion());

        String[] suffixes = CORSAssertion.VARIABLE_SUFFIXES.toArray( new String[CORSAssertion.VARIABLE_SUFFIXES.size()] );
        variablePrefixTextField.setSuffixes(suffixes);

        enableDisableComponents();
        pack();
    }



}