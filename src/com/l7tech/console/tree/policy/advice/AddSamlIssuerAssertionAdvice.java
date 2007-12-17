package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.panels.saml.SamlIssuerAssertionPropertiesEditor;
import com.l7tech.console.panels.Wizard;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

/**
 * @author: ghuang
 */
public class AddSamlIssuerAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SamlIssuerAssertion)) {
            throw new IllegalArgumentException();
        }
        SamlIssuerAssertion subject = (SamlIssuerAssertion)assertions[0];
        SamlIssuerAssertionPropertiesEditor editor = new SamlIssuerAssertionPropertiesEditor();
        editor.setData(subject);
        final Wizard wizard = (Wizard)editor.getDialog();

        // show the wizard
        wizard.pack();
        Utilities.centerOnScreen(wizard);
        DialogDisplayer.display(wizard, new Runnable() {
            public void run() {
                // check that user finished the wizard
                if (wizard.isWizardFinished())
                    pc.proceed();
            }
        });
    }
}
