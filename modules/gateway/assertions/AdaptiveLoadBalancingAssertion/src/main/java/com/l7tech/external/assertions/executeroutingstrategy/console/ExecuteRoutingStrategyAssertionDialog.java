package com.l7tech.external.assertions.executeroutingstrategy.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.executeroutingstrategy.ExecuteRoutingStrategyAssertion;

import javax.swing.*;
import java.awt.*;

public class ExecuteRoutingStrategyAssertionDialog extends AssertionPropertiesOkCancelSupport<ExecuteRoutingStrategyAssertion> {

    private JPanel contentPane;
    private JPanel prefixPanel;
    private JPanel routePanel;
    private JPanel feedbackPanel;
    private TargetVariablePanel strategyField;
    private TargetVariablePanel routeField;
    private TargetVariablePanel feedbackField;

    public ExecuteRoutingStrategyAssertionDialog(final Frame parent, final ExecuteRoutingStrategyAssertion assertion) {
        super(ExecuteRoutingStrategyAssertion.class, parent, assertion, true);
        initComponents();

        strategyField = new TargetVariablePanel();
        strategyField.setValueWillBeRead(true);
        strategyField.setValueWillBeWritten(false);
        prefixPanel.setLayout(new BorderLayout());
        prefixPanel.add(strategyField, BorderLayout.CENTER);
        prefixPanel.add(strategyField, BorderLayout.CENTER);
        strategyField.setAssertion(assertion, getPreviousAssertion());


        routeField = new TargetVariablePanel();
        routePanel.setLayout(new BorderLayout());
        routePanel.add(routeField, BorderLayout.CENTER);
        routeField.setAssertion(assertion, getPreviousAssertion());

        feedbackField = new TargetVariablePanel();
        feedbackPanel.setLayout(new BorderLayout());
        feedbackPanel.add(feedbackField, BorderLayout.CENTER);
        feedbackField.setAssertion(assertion, getPreviousAssertion());

    }

    @Override
    public void setData(ExecuteRoutingStrategyAssertion assertion) {
        strategyField.setVariable(assertion.getStrategy());
        strategyField.setAssertion(assertion, getPreviousAssertion());

        routeField.setVariable(assertion.getRoute());
        routeField.setAssertion(assertion, getPreviousAssertion());

        feedbackField.setVariable(assertion.getFeedback());
        feedbackField.setAssertion(assertion, getPreviousAssertion());
    }

    @Override
    public ExecuteRoutingStrategyAssertion getData(ExecuteRoutingStrategyAssertion assertion) throws ValidationException {
        assertion.setStrategy(strategyField.getVariable());
        assertion.setRoute(routeField.getVariable());
        assertion.setFeedback(feedbackField.getVariable());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
