package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.*;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RequireWssSamlWizardTest extends JFrame {
    public static void main(String[] args) {
        RequireWssSamlWizardTest frame = new RequireWssSamlWizardTest();
        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new SelectStatementWizardStepPanel(
              new AuthenticationMethodsWizardStepPanel(
                new SubjectConfirmationWizardStepPanel(
                  new SubjectConfirmationNameIdentifierWizardStepPanel(
                    new ConditionsWizardStepPanel(null))))));
        RequireWssSaml assertion;
        assertion = new RequireWssSaml();
        Wizard w = new SamlPolicyAssertionWizard(assertion, frame, p, false);
        w.pack();
        w.setVisible(true);
        w.addWindowListener(
          new WindowAdapter() {
              @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        }
        );
    }
}
