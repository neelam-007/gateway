/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.CodeInjectionProtectionAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;

import java.awt.*;

/**
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        final Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof CodeInjectionProtectionAssertion)) {
            throw new IllegalArgumentException();
        }

        final CodeInjectionProtectionAssertion assertion = (CodeInjectionProtectionAssertion) assertions[0];
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        final CodeInjectionProtectionAssertionDialog dialog = new CodeInjectionProtectionAssertionDialog(mainWindow, assertion, false);

        // Shows the dialog.
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            public void run() {
                // Checks that user OKed this dialog.
                if (dialog.isAssertionModified()) {
                    pc.proceed();
                }
            }
        });
    }
}
