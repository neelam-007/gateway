package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.saml.IssuerWizardStepPanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;

/**
 * User: vchan
 */
public class SamlpRequestBuilderAssertionPropertiesEditor extends SamlpAssertionPropertiesEditor<SamlpRequestBuilderAssertion> {


    /**
     * @see SamlpAssertionPropertiesEditor#getMode()
     */
    protected AssertionMode getMode() {
        return AssertionMode.REQUEST;
    }

    /**
     * @see SamlpAssertionPropertiesEditor#createAssertionWizard(com.l7tech.policy.assertion.Assertion)
     */
    @Override
    protected SamlpAssertionWizard createAssertionWizard(SamlpRequestBuilderAssertion assertion) {

        AssertionMode mode = getMode();

        IntroductionWizardStepPanel p;

        p = new IntroductionWizardStepPanel(
              new TargetMessageWizardStepPanel(
                new SamlVersionWizardStepPanel(
                 new IssuerWizardStepPanel(
                  new SelectSamlpQueryWizardStepPanel(
                    new AuthorizationStatementWizardStepPanel(
                      new AttributeQueryWizardStepPanel(
                        new SubjectConfirmationNameIdentifierWizardStepPanel(
                          new SubjectConfirmationWizardStepPanel(
                            new SamlSignatureStepPanel(null, true, getPreviousAssertion()), true, mode, getPreviousAssertion()), getPreviousAssertion()), mode, getPreviousAssertion()), mode, getPreviousAssertion()), mode, getPreviousAssertion())), mode, getPreviousAssertion()), true, mode, getPreviousAssertion()), mode, getPreviousAssertion());

        SamlpAssertionWizard wiz = new SamlpAssertionWizard(assertion, TopComponents.getInstance().getTopParent(), p, mode, readOnly);
        wiz.addWizardListener(new WizardAdapter() {
           @Override
           public void wizardFinished(WizardEvent e) { confirmed = true; }
        });
        wiz.addValidationRulesDefinedInWizardStepPanel(SubjectConfirmationWizardStepPanel.class, p);
        return wiz;
    }
}
