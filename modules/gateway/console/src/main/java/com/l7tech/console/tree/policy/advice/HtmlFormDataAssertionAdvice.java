/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.HtmlFormDataAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;

import java.awt.*;

/**
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class HtmlFormDataAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        final Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof HtmlFormDataAssertion)) {
            throw new IllegalArgumentException();
        }

        final HtmlFormDataAssertion assertion = (HtmlFormDataAssertion) assertions[0];
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        final HtmlFormDataAssertionDialog dialog = new HtmlFormDataAssertionDialog(mainWindow, assertion);

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
