package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseEvaluationAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlpAuthorizationStatement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: megery
 * Date: Nov 7, 2008
 */
public class AuthorizationRequestWizardStepPanel extends SamlpWizardStepPanel {

    private static final String DECISION_PERMIT = "Permit";
    private static final String DECISION_DENY = "Deny";
    private static final String DECISION_INDETERMINATE = "Indeterminate";
    private static final String[] EXPECTED_DECISION_VALUES =
            new String[] {DECISION_PERMIT, DECISION_DENY, DECISION_INDETERMINATE};

    private JPanel mainPanel;
    private JCheckBox failIfAuthzDecisionMismatch;
    private JComboBox expectedAuthzDecision;

    public AuthorizationRequestWizardStepPanel(WizardStepPanel next) {
        super(next, AssertionMode.RESPONSE);
        initialize();
    }


    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlpResponseEvaluationAssertion assertion = (SamlpResponseEvaluationAssertion) settings;

        assertion.setAuthzDecisionFalsifyAssertion(failIfAuthzDecisionMismatch.isSelected());
        if (failIfAuthzDecisionMismatch.isSelected()) {
            assertion.setAuthzDecisionOption(1);
            assertion.setAuthzDecisionVariable(expectedAuthzDecision.getSelectedItem().toString());
        } else {
            assertion.setAuthzDecisionOption(0);
            assertion.setAuthzDecisionVariable(null);
        }
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlpResponseEvaluationAssertion assertion = (SamlpResponseEvaluationAssertion) settings;

        // skip this step if Authorization is not selected
        SamlpAuthorizationStatement statement = assertion.getAuthorizationStatement();
        setSkipped(statement == null);
        if (statement == null) {
            return;
        }

        failIfAuthzDecisionMismatch.setSelected(assertion.isAuthzDecisionFalsifyAssertion());
        if (assertion.isAuthzDecisionFalsifyAssertion()) {
            if (DECISION_INDETERMINATE.equals(assertion.getAuthzDecisionVariable())) {
                expectedAuthzDecision.setSelectedItem(DECISION_INDETERMINATE);
            } else if (DECISION_DENY.equals(assertion.getAuthzDecisionVariable())) {
                expectedAuthzDecision.setSelectedItem(DECISION_DENY);
            } else {
                expectedAuthzDecision.setSelectedItem(DECISION_PERMIT);
            }
        }

        enableDisableComponents();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        failIfAuthzDecisionMismatch.setSelected(true);

        ComboBoxModel cbModel = new DefaultComboBoxModel(EXPECTED_DECISION_VALUES);
        expectedAuthzDecision.setModel(cbModel);

        failIfAuthzDecisionMismatch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });
    }

    private void enableDisableComponents() {
        // enable/disable combo box based on checkbox selected
        expectedAuthzDecision.setEnabled(failIfAuthzDecisionMismatch.isSelected());
    }

    public boolean canAdvance() {
        return false;
    }

    public boolean canFinish() {
        return true;
    }

    public String getStepLabel() {
        return "Authorization Decision";
    }

    public String getDescription() {
        return "<html>Specify whether the assertion will fail when the authorization decision does not match the expected result.</html>";
    }
}
