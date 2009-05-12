package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.saml.*;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml2;

import java.awt.*;

/**
 * Invoked when a SAML Authentication Assertion is dropped to a policy tree to
 * initiate the authentication statement wizard.
 * <p/>
 */
public class AddRequestWssSamlAdvice implements Advice {

    @Override
    public void proceed(final PolicyChange pc) {
        final boolean[] proceed = { false };
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof RequestWssSaml)) {
            throw new IllegalArgumentException();
        }
        final RequestWssSaml assertion = (RequestWssSaml)assertions[0];
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
                          new ConditionsWizardStepPanel(null)))))))));

        final Wizard w = new SamlPolicyAssertionWizard(assertion, f, p, false, false);
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
                // check that user oked this dialog
                if ( proceed[0] ) {
                    if (assertion.getVersion()==null ||
                        assertion.getVersion() ==1) {
                        pc.getNewChild().setUserObject(new RequestWssSaml(assertion));
                    }
                    else {
                        pc.getNewChild().setUserObject(new RequestWssSaml2(assertion));
                    }

                    pc.proceed();
                }
            }
        });
    }
}
