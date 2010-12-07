package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.xmlsec.VariableCredentialSourceAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class VariableCredentialSourceAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<VariableCredentialSourceAssertion> {
    private JPanel contentPane;
    private JPanel variableNamePanel;
    private TargetVariablePanel variableNameField;

    public VariableCredentialSourceAssertionPropertiesDialog(Frame parent, VariableCredentialSourceAssertion assertion) {
        super(assertion.getClass(), parent, (String)assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();

        variableNameField = new TargetVariablePanel();
        variableNamePanel.setLayout(new BorderLayout());
        variableNamePanel.add(variableNameField, BorderLayout.CENTER);
        variableNameField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(variableNameField.isEntryValid());
            }
        });

        setData(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    public void setData(VariableCredentialSourceAssertion assertion) {
        variableNameField.setVariable(assertion.getVariableName());
        variableNameField.setAssertion(assertion);
    }

    @Override
    public VariableCredentialSourceAssertion getData(VariableCredentialSourceAssertion assertion) throws ValidationException {
        String variableName = variableNameField.getVariable();
        assertion.setVariableName(VariablePrefixUtil.fixVariableName(variableName));
        return assertion;
    }
}
