package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.saml.*;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

import javax.swing.*;

/**
 * Invoked when a SAML Authentication Assertion is dropped to a policy tree to
 * initiate the authentication statement wizard.
 * <p/>
 */
public class AddRequestWssSamlAdvice implements Advice {
    boolean proceed = false;

    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof RequestWssSaml)) {
            throw new IllegalArgumentException();
        }
        RequestWssSaml assertion = (RequestWssSaml)assertions[0];
        JFrame f = TopComponents.getInstance().getMainWindow();

        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new SelectStatementWizardStepPanel(
              new AuthenticationMethodsWizardStepPanel(
                new AuthorizationStatementWizardStepPanel(
                  new AttributeStatementWizardStepPanel(
                    new SubjectConfirmationWizardStepPanel(
                      new SubjectConfirmationNameIdentifierWizardStepPanel(
                        new ConditionsWizardStepPanel(null))))))));

        final Wizard w = new RequestWssSamlStatementWizard(assertion, f, p);
        w.addWizardListener(new WizardAdapter() {
            public void wizardFinished(WizardEvent e) {
                proceed = true;
            }
        });

        Utilities.setEscKeyStrokeDisposes(w);
        w.pack();
        w.setSize(850, 500);
        Utilities.centerOnScreen(w);
        w.setVisible(true);
        // check that user oked this dialog
        if (proceed) {
            pc.proceed();
        }
    }
}
