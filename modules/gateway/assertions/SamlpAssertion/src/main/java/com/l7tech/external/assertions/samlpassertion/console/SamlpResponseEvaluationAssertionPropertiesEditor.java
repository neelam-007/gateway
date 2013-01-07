package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseEvaluationAssertion;

/**
 * User: vchan
 */
public class SamlpResponseEvaluationAssertionPropertiesEditor
        extends SamlpAssertionPropertiesEditor<SamlpResponseEvaluationAssertion>
{

    /**
     * @see SamlpAssertionPropertiesEditor#getMode()
     */
    protected AssertionMode getMode() {
        return AssertionMode.RESPONSE;
    }

    /**
     * @see SamlpAssertionPropertiesEditor#createAssertionWizard(com.l7tech.policy.assertion.Assertion)
     */
    protected SamlpAssertionWizard createAssertionWizard(SamlpResponseEvaluationAssertion assertion) {

        AssertionMode mode = getMode();

        AttributeQueryWizardStepPanel aqsp = new AttributeQueryWizardStepPanel(null, true, mode, getPreviousAssertion());
        AuthorizationRequestWizardStepPanel arsp = new AuthorizationRequestWizardStepPanel(aqsp, getPreviousAssertion());
        ResponseStatusWizardStepPanel rssp = new ResponseStatusWizardStepPanel(arsp, mode, getPreviousAssertion());
        SelectSamlpQueryWizardStepPanel sqsp = new SelectSamlpQueryWizardStepPanel(rssp, mode, getPreviousAssertion());
        SamlVersionWizardStepPanel svsp = new SamlVersionWizardStepPanel(sqsp, mode, getPreviousAssertion());
        TargetMessageWizardStepPanel tmsp = new TargetMessageWizardStepPanel(svsp, mode, getPreviousAssertion());
        IntroductionWizardStepPanel p = new IntroductionWizardStepPanel(tmsp, mode, getPreviousAssertion());

        final SamlpResponseEvaluationAssertion workingCopy = (SamlpResponseEvaluationAssertion) assertion.clone();
        SamlpAssertionWizard wiz = new SamlpAssertionWizard(workingCopy,
                TopComponents.getInstance().getTopParent(),
                p,
                mode,
                readOnly);

        wiz.addWizardListener(new WizardAdapter() {
           @Override
           public void wizardFinished(WizardEvent e) {
               confirmed = true;
               SamlpResponseEvaluationAssertionPropertiesEditor.this.assertion = workingCopy;
           }
        });

        return wiz;
    }
}