package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.policy.PolicyPositionAware;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 14-Apr-2009
 * Time: 8:50:24 PM
  */
public class XacmlRequestBuilderRequestPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JComboBox versionComboBox;
    private JPanel mainPanel;
    private JComboBox encapsulationComboBox;
    private JComboBox outputMessageComboBox;
    private JPanel outputMessageContextVarPanel;
    private TargetVariablePanel outputMessageContextVarField;
    private PolicyPositionAware.PolicyPosition policyPosition;

    private XacmlRequestBuilderAssertion assertion;

    public XacmlRequestBuilderRequestPanel( final XacmlRequestBuilderAssertion assertion , Assertion previousAssertion) {
        this.assertion = assertion;
        init(previousAssertion);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init(Assertion previousAssertion) {
        DefaultComboBoxModel model = new DefaultComboBoxModel(XacmlAssertionEnums.XacmlVersionType.values());
        versionComboBox.setModel(model);
        versionComboBox.setSelectedItem(assertion.getXacmlVersion());

        versionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                assertion.setXacmlVersion((XacmlAssertionEnums.XacmlVersionType)versionComboBox.getSelectedItem());
            }
        });

        model = new DefaultComboBoxModel(XacmlAssertionEnums.SoapVersion.values());
        encapsulationComboBox.setModel(model);
        encapsulationComboBox.setSelectedItem(assertion.getSoapEncapsulation());

        encapsulationComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                assertion.setSoapEncapsulation((XacmlAssertionEnums.SoapVersion)encapsulationComboBox.getSelectedItem());
            }
        });

        model = new DefaultComboBoxModel();
        model.addElement(XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST);
        model.addElement(XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE);
        model.addElement(XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE);
        outputMessageComboBox.setModel(model);
        outputMessageComboBox.setRenderer( new TextListCellRenderer<XacmlAssertionEnums.MessageLocation>( new Functions.Unary<String,XacmlAssertionEnums.MessageLocation>(){
            @Override
            public String call( final XacmlAssertionEnums.MessageLocation messageLocation ) {
                return messageLocation.getLocationName();
            }
        }) );

        switch(assertion.getOutputMessageDestination()) {
            case DEFAULT_REQUEST:
                outputMessageComboBox.setSelectedIndex(0);
                break;
            case DEFAULT_RESPONSE:
                outputMessageComboBox.setSelectedIndex(1);
                break;
            case CONTEXT_VARIABLE:
                outputMessageComboBox.setSelectedIndex(2);
                break;
            default:
                throw new IllegalStateException("Unsupported output message destination found");//only happen if enum changes
        }

        outputMessageContextVarField = new TargetVariablePanel();
        outputMessageContextVarPanel.setLayout(new BorderLayout());
        outputMessageContextVarPanel.add(outputMessageContextVarField, BorderLayout.CENTER);
        outputMessageContextVarField.setAssertion(assertion,previousAssertion);
        
        if(assertion.getOutputMessageDestination() != XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            outputMessageContextVarField.setEnabled(false);
        } else {
            outputMessageContextVarField.setEnabled(true);
            outputMessageContextVarField.setVariable(assertion.getOutputMessageVariableName());
        }

        outputMessageComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlAssertionEnums.MessageLocation entry =
                        (XacmlAssertionEnums.MessageLocation)outputMessageComboBox.getSelectedItem();
                outputMessageContextVarField.setEnabled(entry == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE);
                assertion.setOutputMessageDestination(entry);
            }
        });
    }

    @Override
    public boolean handleDispose(final XacmlRequestBuilderDialog builderDialog) {
        if(outputMessageComboBox.getSelectedItem()!= XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            return true;
        }

        if(outputMessageContextVarField.isEntryValid()) {
            assertion.setOutputMessageVariableName(outputMessageContextVarField.getVariable());
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Output message context variable name is invalid.", "Context Variable Name Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }


}
