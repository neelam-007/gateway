package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.*;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RequestWssSamlWizardTest extends JFrame {

    public static void main(String[] args) {
       RequestWssSamlWizardTest frame = new RequestWssSamlWizardTest();
        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new AuthenticationMethodsWizardStepPanel(
              new SubjectConfirmationWizardStepPanel(
                new SubjectConfirmationNameIdentifierWizardStepPanel(
                new ConditionsWizardStepPanel(null)))));
        RequestWssSaml assertion;
        assertion = new RequestWssSaml();
        Wizard w = new RequestWssSamlStatementWizard(assertion, frame, p);
        w.pack();
        w.show();
        w.addWindowListener(
          new WindowAdapter() {
              public void windowClosing(WindowEvent e) {
                  System.exit(0);
              }
          }
        );
    }
}
