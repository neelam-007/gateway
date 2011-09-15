package com.l7tech.external.assertions.uuidgenerator.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Dialog for editing UUIDGeneratorAssertion properties.
 */
public class UUIDGeneratorPropertiesDialog extends AssertionPropertiesOkCancelSupport<UUIDGeneratorAssertion> {
    private JPanel contentPane;
    private JPanel variableNamePanel;
    private JTextField amountTextField;
    private TargetVariablePanel targetVariablePanel;
    private InputValidator validators;

    public UUIDGeneratorPropertiesDialog(Frame parent, UUIDGeneratorAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void initComponents() {
        super.initComponents();

        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        variableNamePanel.setLayout(new BorderLayout());
        variableNamePanel.add(targetVariablePanel, BorderLayout.CENTER);

        validators = new InputValidator(this, getTitle());
        validators.disableButtonWhenInvalid(getOkButton());
        validators.constrainTextFieldToBeNonEmpty("amount", amountTextField, null);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    public void setData(UUIDGeneratorAssertion assertion) {
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        targetVariablePanel.setVariable(assertion.getTargetVariable());
        amountTextField.setText(assertion.getAmount());
    }

    public UUIDGeneratorAssertion getData(UUIDGeneratorAssertion assertion) {
        assertion.setTargetVariable(targetVariablePanel.getVariable());
        assertion.setAmount(amountTextField.getText());
        return assertion;
    }
}
