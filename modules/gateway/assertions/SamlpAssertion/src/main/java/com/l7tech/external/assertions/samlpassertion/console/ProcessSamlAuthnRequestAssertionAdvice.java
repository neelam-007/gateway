package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;

import java.awt.*;

/**
 * ProcessSamlAuthnRequestAssertionAdvice sets the new default value for the ProcessSamlAuthnRequestAssertion
 */
public class ProcessSamlAuthnRequestAssertionAdvice implements Advice {
    @Override
    public void proceed(PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();

        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof ProcessSamlAuthnRequestAssertion)) {
            throw new IllegalArgumentException();
        }

        ProcessSamlAuthnRequestAssertion subject = (ProcessSamlAuthnRequestAssertion) assertions[0];

        // DE220639 - Defaults new instance of the ProcessSamlAuthnRequestAssertion to not require AssertionConsumerServiceURL
        subject.setRequiredAssertionConsumerServiceURL(false);

        final Frame mw = TopComponents.getInstance().getTopParent();
        final ProcessSamlAuthnRequestPropertiesDialog dlg = new ProcessSamlAuthnRequestPropertiesDialog(mw, subject);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, () -> {
            // check that user confirmed the new properties
            if (dlg.isConfirmed()) {
                pc.proceed();
            }
        });
    }
}
