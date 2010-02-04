/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlissuer;

import com.l7tech.console.panels.Wizard;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.l7tech.console.panels.saml.*;
import org.junit.Ignore;

@Ignore
public class SamlIssuerAssertionWizardTest extends JFrame {
    public static void main(String[] args) {
        SamlIssuerAssertionWizardTest frame = new SamlIssuerAssertionWizardTest();
        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new VersionWizardStepPanel(
              new SelectStatementWizardStepPanel(
                  new AuthorizationStatementWizardStepPanel(
                    new AttributeStatementWizardStepPanel(
                        new SubjectConfirmationNameIdentifierWizardStepPanel(
                            new SubjectConfirmationWizardStepPanel(
                                new ConditionsWizardStepPanel(
                                    new SamlSignatureStepPanel(null, true), true, true), true), true), true), true), true), true), true);
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
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
