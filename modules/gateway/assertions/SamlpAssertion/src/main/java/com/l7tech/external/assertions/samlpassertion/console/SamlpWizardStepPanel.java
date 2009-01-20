package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;

/**
 * User: vchan
 */
public abstract class SamlpWizardStepPanel extends WizardStepPanel {

    private AssertionMode mode;

    protected SamlpWizardStepPanel(WizardStepPanel next, AssertionMode mode) {
        super(next);
        this.mode = mode;
    }

    protected SamlpWizardStepPanel(WizardStepPanel next, AssertionMode mode, boolean readOnly) {
        super(next, readOnly);
        this.mode = mode;
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

}
