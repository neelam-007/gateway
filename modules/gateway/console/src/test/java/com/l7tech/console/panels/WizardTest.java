package com.l7tech.console.panels;

import org.junit.Ignore;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
@Ignore
public class WizardTest extends JFrame {

    public static void main(String[] args) throws Exception {
        WizardTest t = new WizardTest();
        WsdlDefinitionPanel defPanel =
          new WsdlDefinitionPanel(
            new WsdlMessagesPanel(
              new WsdlPortTypePanel(
                new WsdlPortTypeBindingPanel(new WsdlServicePanel(null)))
            )
          );
        WsdlCreateOverviewPanel p = new WsdlCreateOverviewPanel(defPanel);
        Wizard w = new WsdlCreateWizard(t, p);
        w.pack();
        w.setVisible(true);
        w.addWindowListener(
          new WindowAdapter() {
              public void windowClosing(WindowEvent e) {
                  System.exit(0);
              }
          }
        );
    }
}
