package com.l7tech.external.assertions.validatenonsoapsaml.console;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.saml.*;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.validatenonsoapsaml.ValidateNonSoapSamlAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;

import java.awt.*;

public class AddValidateNonSoapSamlAssertionAdvice implements Advice {

    @Override
    public void proceed(final PolicyChange pc) {
        final boolean[] proceed = { false };
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof ValidateNonSoapSamlAssertion)) {
            throw new IllegalArgumentException();
        }
        final ValidateNonSoapSamlAssertion assertion = (ValidateNonSoapSamlAssertion)assertions[0];
        Frame f = TopComponents.getInstance().getTopParent();

        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new VersionWizardStepPanel(
              new SelectStatementWizardStepPanel(
                new AuthenticationMethodsNewWizardStepPanel(
                  new AuthorizationStatementWizardStepPanel(
                    new AttributeStatementWizardStepPanel(
                      new SubjectConfirmationWizardStepPanel(
                        new SubjectConfirmationNameIdentifierWizardStepPanel(
                          new ConditionsWizardStepPanel(
                            new RequireEmbeddedSignatureWizardStepPanel(null))))))))));

        final Wizard w = new SamlPolicyAssertionWizard(assertion, f, p, false);
        w.addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) {
                proceed[0] = true;
            }
        });

        Utilities.setEscKeyStrokeDisposes(w);
        w.pack();
        Utilities.centerOnScreen(w);
        DialogDisplayer.display(w, new Runnable() {
            @Override
            public void run() {
                if (proceed[0]) {
                    pc.getNewChild().setUserObject(new ValidateNonSoapSamlAssertion(assertion));
                    pc.proceed();
                }
            }
        });
    }
}
