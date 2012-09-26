package com.l7tech.external.assertions.processroutingstrategyresult.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.adaptiveloadbalancing.console.TargetVariablePanelValidationRule;
import com.l7tech.external.assertions.processroutingstrategyresult.ProcessRoutingStrategyResultAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ProcessRoutingStrategyResultAssertionDialog extends AssertionPropertiesOkCancelSupport<ProcessRoutingStrategyResultAssertion> {
    private JPanel contentPane;
    private JPanel strategyPanel;
    private JPanel feedbackPanel;
    private JLabel routingStrategyPrefixLabel;
    private JLabel feedbackListLabel;
    private TargetVariablePanel strategyField;
    private TargetVariablePanel feedbackField;
    private final InputValidator inputValidator;


    public ProcessRoutingStrategyResultAssertionDialog(final Frame parent, final ProcessRoutingStrategyResultAssertion assertion) {
        super(ProcessRoutingStrategyResultAssertion.class, parent, assertion, true);
        initComponents();

        strategyField = new TargetVariablePanel();
        strategyField.setValueWillBeRead(true);
        strategyField.setValueWillBeWritten(false);
        strategyPanel.setLayout(new BorderLayout());
        strategyPanel.add(strategyField, BorderLayout.CENTER);
        strategyPanel.add(strategyField, BorderLayout.CENTER);
        strategyField.setAssertion(assertion, getPreviousAssertion());

        feedbackField = new TargetVariablePanel();
        feedbackField.setValueWillBeRead(true);
        feedbackField.setValueWillBeWritten(false);
        feedbackPanel.setLayout(new BorderLayout());
        feedbackPanel.add(feedbackField, BorderLayout.CENTER);
        feedbackField.setAssertion(assertion, getPreviousAssertion());

        inputValidator = new InputValidator(this, getTitle());

        inputValidator.addRule(new TargetVariablePanelValidationRule(strategyField, routingStrategyPrefixLabel.getText()));
        inputValidator.addRule(new TargetVariablePanelValidationRule(feedbackField, feedbackListLabel.getText()));

        inputValidator.attachToButton(getOkButton(), super.createOkAction());

    }

    @Override
    public void setData(ProcessRoutingStrategyResultAssertion assertion) {
        strategyField.setVariable(assertion.getStrategy());
        strategyField.setAssertion(assertion, getPreviousAssertion());
        feedbackField.setVariable(assertion.getFeedback());
        feedbackField.setAssertion(assertion, getPreviousAssertion());
    }

    @Override
    public ProcessRoutingStrategyResultAssertion getData(ProcessRoutingStrategyResultAssertion assertion) throws ValidationException {
        assertion.setStrategy(strategyField.getVariable());
        assertion.setFeedback(feedbackField.getVariable());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected ActionListener createOkAction() {
        return new RunOnChangeListener();
    }


}
