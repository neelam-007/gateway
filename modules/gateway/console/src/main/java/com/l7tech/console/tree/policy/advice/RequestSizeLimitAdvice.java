package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.panels.RequestSizeLimitDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSizeLimit;

import java.awt.*;

/**
 * User: megery
 */
public class RequestSizeLimitAdvice implements Advice{
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof RequestSizeLimit)) {
            throw new IllegalArgumentException();
        }
        final AssertionTreeNode<RequestSizeLimit> subject = pc.getNewChild();
        final RequestSizeLimit ass = subject.asAssertion();
        final Frame mw = TopComponents.getInstance().getTopParent();
        final RequestSizeLimitDialog dlg = new RequestSizeLimitDialog(mw, ass, true, false);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.wasConfirmed()) {
                    subject.setUserObject(dlg.getAssertion());
                    pc.proceed();
                }
            }
        });
    }
}
