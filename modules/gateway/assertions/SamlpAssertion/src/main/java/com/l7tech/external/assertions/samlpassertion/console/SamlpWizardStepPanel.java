package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.Assertion;

/**
 * User: vchan
 */
public abstract class SamlpWizardStepPanel extends WizardStepPanel {

    private AssertionMode mode;
    private Assertion prevAssertion;

    protected SamlpWizardStepPanel(WizardStepPanel next, AssertionMode mode,  Assertion prevAssertion) {
        super(next);
        this.mode = mode;
        this.prevAssertion = prevAssertion;
    }

    protected SamlpWizardStepPanel(WizardStepPanel next, AssertionMode mode, boolean readOnly,  Assertion prevAssertion) {
        super(next, readOnly);
        this.mode = mode;
        this.prevAssertion = prevAssertion;
    }

    protected boolean isRequestMode() {
        return (AssertionMode.REQUEST.equals(this.mode));
    }

    protected boolean isResponseMode() {
        return (AssertionMode.RESPONSE.equals(this.mode));
    }

    protected AssertionMode getMode() {
        return mode;
    }

    protected Assertion getPreviousAssertion() {
        return prevAssertion;
    }

}
