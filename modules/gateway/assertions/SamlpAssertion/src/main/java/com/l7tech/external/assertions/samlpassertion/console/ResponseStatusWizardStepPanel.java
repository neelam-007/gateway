package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseEvaluationAssertion;

import javax.swing.*;
import java.awt.*;

/**
 * User: megery
 * Date: Nov 7, 2008
 */
public class ResponseStatusWizardStepPanel extends SamlpWizardStepPanel {
    private JCheckBox failIfNotSuccess;
    private JPanel mainPanel;

    private Boolean moreSteps;

    protected ResponseStatusWizardStepPanel(WizardStepPanel next, AssertionMode mode) {
        super(next, mode);
        initialize();
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlpResponseEvaluationAssertion assertion = (SamlpResponseEvaluationAssertion) settings;
        assertion.setResponseStatusFalsifyAssertion(failIfNotSuccess.isSelected());
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlpResponseEvaluationAssertion assertion = (SamlpResponseEvaluationAssertion) settings;
        failIfNotSuccess.setSelected(assertion.isResponseStatusFalsifyAssertion());

        // only allow to continue if the response is either Authz or Attribute query
        moreSteps = (assertion.getAuthenticationStatement() == null);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    public String getStepLabel() {
        return "Response Status";
    }

    public boolean canAdvance() {
        return (moreSteps != null ? moreSteps : super.canAdvance());
    }

    public boolean canFinish() {
        return !canAdvance();
    }

    public String getDescription() {
        return "<html>Specify whether the assertion will fail when the response status code is not \"Success\".</html>";
    }
}
