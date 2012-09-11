package com.l7tech.external.assertions.processroutingstrategyresult.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.processroutingstrategyresult.ProcessRoutingStrategyResultAssertion;

import javax.swing.*;
import java.awt.*;

public class ProcessRoutingStrategyResultAssertionDialog extends AssertionPropertiesOkCancelSupport<ProcessRoutingStrategyResultAssertion> {
    private JPanel contentPane;
    private JPanel strategyPanel;
    private JPanel feedbackPanel;
    private TargetVariablePanel strategyField;
    private TargetVariablePanel feedbackField;

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
}
