package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.*;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SamlAuthenticationWizardTest extends JFrame {

    public static void main(String[] args) {
       SamlAuthenticationWizardTest frame = new SamlAuthenticationWizardTest();
        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new AuthenticationMethodsWizardStepPanel(
              new SubjectConfirmationWizardStepPanel(
                new ConditionsWizardStepPanel(null))));
        Wizard w = new AuthenticationStatementWizard(frame, p);
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
