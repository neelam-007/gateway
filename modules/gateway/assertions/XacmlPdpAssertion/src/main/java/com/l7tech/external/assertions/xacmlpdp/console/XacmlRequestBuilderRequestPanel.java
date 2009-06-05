package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 14-Apr-2009
 * Time: 8:50:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderRequestPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    public static class MessageOutputEntry {
        private XacmlRequestBuilderAssertion.MessageTarget messageTarget;

        public MessageOutputEntry(XacmlRequestBuilderAssertion.MessageTarget messageTarget) {
            this.messageTarget = messageTarget;
        }

        public XacmlRequestBuilderAssertion.MessageTarget getMessageTarget() {
            return messageTarget;
        }

        public String toString() {
            switch(messageTarget) {
                case REQUEST_MESSAGE:
                    return "Default Request";
                case RESPONSE_MESSAGE:
                    return "Default Response";
                case MESSAGE_VARIABLE:
                    return "Message Variable:";
            }
            throw new IllegalStateException("Unknown messge target");
        }
    }

    private JComboBox versionComboBox;
    private JPanel mainPanel;
    private JComboBox encapsulationComboBox;
    private JComboBox outputMessageComboBox;
    private JTextField outputMessageContextVarField;

    private XacmlRequestBuilderAssertion assertion;

    public XacmlRequestBuilderRequestPanel(XacmlRequestBuilderAssertion assertion) {
        this.assertion = assertion;
        init();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init() {
        DefaultComboBoxModel model = new DefaultComboBoxModel(XacmlRequestBuilderAssertion.XacmlVersionType.values());
        versionComboBox.setModel(model);
        versionComboBox.setSelectedItem(assertion.getXacmlVersion());

        versionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                assertion.setXacmlVersion((XacmlRequestBuilderAssertion.XacmlVersionType)versionComboBox.getSelectedItem());
            }
        });

        model = new DefaultComboBoxModel(XacmlRequestBuilderAssertion.SoapEncapsulationType.values());
        encapsulationComboBox.setModel(model);
        encapsulationComboBox.setSelectedItem(assertion.getSoapEncapsulation());

        encapsulationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                assertion.setSoapEncapsulation((XacmlRequestBuilderAssertion.SoapEncapsulationType)encapsulationComboBox.getSelectedItem());
            }
        });

        model = new DefaultComboBoxModel();
        model.addElement(new MessageOutputEntry(XacmlRequestBuilderAssertion.MessageTarget.REQUEST_MESSAGE));
        model.addElement(new MessageOutputEntry(XacmlRequestBuilderAssertion.MessageTarget.RESPONSE_MESSAGE));
        model.addElement(new MessageOutputEntry(XacmlRequestBuilderAssertion.MessageTarget.MESSAGE_VARIABLE));
        outputMessageComboBox.setModel(model);

        switch(assertion.getOutputMessageDestination()) {
            case REQUEST_MESSAGE:
                outputMessageComboBox.setSelectedIndex(0);
                break;
            case RESPONSE_MESSAGE:
                outputMessageComboBox.setSelectedIndex(1);
                break;
            case MESSAGE_VARIABLE:
                outputMessageComboBox.setSelectedIndex(2);
                break;
        }

        if(assertion.getOutputMessageDestination() != XacmlRequestBuilderAssertion.MessageTarget.MESSAGE_VARIABLE) {
            outputMessageContextVarField.setEnabled(false);
        } else {
            outputMessageContextVarField.setEnabled(true);
            outputMessageContextVarField.setText(assertion.getOutputMessageVariableName());
        }

        outputMessageComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MessageOutputEntry entry = (MessageOutputEntry)outputMessageComboBox.getSelectedItem();
                outputMessageContextVarField.setEnabled(entry.getMessageTarget() == XacmlRequestBuilderAssertion.MessageTarget.MESSAGE_VARIABLE);
                assertion.setOutputMessageDestination(entry.getMessageTarget());
            }
        });

        /*outputMessageContextVarField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                assertion.setOutputMessageVariableName(outputMessageContextVarField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                assertion.setOutputMessageVariableName(outputMessageContextVarField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                assertion.setOutputMessageVariableName(outputMessageContextVarField.getText().trim());
            }
        });*/
    }

    public boolean handleDispose() {
        if(((MessageOutputEntry)outputMessageComboBox.getSelectedItem()).getMessageTarget()
                != XacmlRequestBuilderAssertion.MessageTarget.MESSAGE_VARIABLE) {
            return true;
        }

        if(VariableMetadata.isNameValid(outputMessageContextVarField.getText().trim())) {
            assertion.setOutputMessageVariableName(outputMessageContextVarField.getText().trim());
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Output message context variable name is invalid.", "Context Variable Name Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
