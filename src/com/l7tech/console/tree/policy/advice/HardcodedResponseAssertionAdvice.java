package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.HardcodedResponseDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;

import java.awt.*;

/**
 * Advice for dragging a Hardcoded Response assertion into the tree.
 */
public class HardcodedResponseAssertionAdvice implements Advice{
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof HardcodedResponseAssertion)) {
            throw new IllegalArgumentException();
        }
        HardcodedResponseAssertion subject = (HardcodedResponseAssertion) assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final HardcodedResponseDialog dlg = new HardcodedResponseDialog(mw, subject, true);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.wasConfirmed()) {
                    pc.proceed();
                }
            }
        });
    }
}
