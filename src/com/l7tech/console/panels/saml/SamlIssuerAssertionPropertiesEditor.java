/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.SamlIssuerAssertion;

import javax.swing.*;

/**
 * @author alex
 */
public class SamlIssuerAssertionPropertiesEditor implements AssertionPropertiesEditor<SamlIssuerAssertion> {
    private SamlPolicyAssertionWizard wizard;
    private boolean confirmed;
    private SamlIssuerAssertion assertion;

    public SamlIssuerAssertionPropertiesEditor() {
    }

    public JDialog getDialog() {
        return wizard;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(SamlIssuerAssertion assertion) {
        this.assertion = assertion;

        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new VersionWizardStepPanel(
              new SelectStatementWizardStepPanel(
                  new AuthorizationStatementWizardStepPanel(
                    new AttributeStatementWizardStepPanel(
                        new SubjectConfirmationNameIdentifierWizardStepPanel(
                            new SubjectConfirmationWizardStepPanel(
                                new ConditionsWizardStepPanel(
                                    new SamlSignatureStepPanel(null, true), true, true), true, true), true), true), true)), true), true);

        SamlPolicyAssertionWizard wiz = new SamlPolicyAssertionWizard(assertion, TopComponents.getInstance().getTopParent(), p, true);
        wiz.addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) { confirmed = true; }
        });
        wizard = wiz;
    }

    public SamlIssuerAssertion getData(SamlIssuerAssertion assertion) {
        return this.assertion;
    }
}
