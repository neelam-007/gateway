package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.saml.*;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;

import javax.swing.*;

/**
 * Invoked when a SAML Authentication Assertion is dropped to a policy tree to
 * initiate the authentication statement wizard.
 * <p/>
 */
public class AddSamlAuthorizationStatementAssertionAdvice implements Advice {
    boolean proceed = false;

    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SamlAuthorizationStatement)) {
            throw new IllegalArgumentException();
        }
        SamlAuthorizationStatement subject = (SamlAuthorizationStatement)assertions[0];
        JFrame f = TopComponents.getInstance().getMainWindow();

        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(new
            AuthorizationStatementWizardStepPanel(
              new SubjectConfirmationWizardStepPanel(
                new ConditionsWizardStepPanel(null))));

        Wizard w = new AuthenticationStatementWizard(f, p);
        w.addWizardListener(new WizardAdapter() {
            public void wizardFinished(WizardEvent e) {
                proceed = true;
            }
        });

        Actions.setEscKeyStrokeDisposes(w);
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
