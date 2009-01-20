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

        AttributeQueryWizardStepPanel aqsp = new AttributeQueryWizardStepPanel(null, true, mode);
        AuthorizationRequestWizardStepPanel arsp = new AuthorizationRequestWizardStepPanel(aqsp);
        ResponseStatusWizardStepPanel rssp = new ResponseStatusWizardStepPanel(arsp, mode);
        SelectSamlpQueryWizardStepPanel sqsp = new SelectSamlpQueryWizardStepPanel(rssp, mode);
        SamlVersionWizardStepPanel svsp = new SamlVersionWizardStepPanel(sqsp, mode);
        TargetMessageWizardStepPanel tmsp = new TargetMessageWizardStepPanel(svsp, mode);
        IntroductionWizardStepPanel p = new IntroductionWizardStepPanel(tmsp, mode);

        SamlpAssertionWizard wiz = new SamlpAssertionWizard(assertion, 
                TopComponents.getInstance().getTopParent(),
                p,
                mode,
                readOnly);

        wiz.addWizardListener(new WizardAdapter() {
           @Override
           public void wizardFinished(WizardEvent e) { confirmed = true; }
        });

        return wiz;
    }
}