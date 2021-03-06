package com.l7tech.external.assertions.samlissuer;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.external.assertions.samlissuer.console.SamlIssuerAssertionPropertiesEditor;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

/**
 * @author: ghuang
 */
public class AddSamlIssuerAssertionAdvice implements Advice {
    @Override
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SamlIssuerAssertion)) {
            throw new IllegalArgumentException();
        }

        final SamlIssuerAssertionPropertiesEditor propertiesEditor = new SamlIssuerAssertionPropertiesEditor();
        SamlIssuerAssertion assertion = (SamlIssuerAssertion)assertions[0];
        propertiesEditor.setData(assertion);
        Wizard wizard = (Wizard)propertiesEditor.getDialog();

        // show the wizard
        wizard.pack();
        Utilities.centerOnScreen(wizard);
        DialogDisplayer.display(wizard, new Runnable() {
            @Override
            public void run() {
                // check that user finished the wizard
                if (propertiesEditor.isConfirmed())
                    pc.proceed();
            }
        });
    }
}
