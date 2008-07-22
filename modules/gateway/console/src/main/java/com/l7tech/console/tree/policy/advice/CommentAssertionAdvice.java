/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 28, 2005<br/>
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.CommentAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;

import java.awt.*;

/**
 * Invoked when a {@link com.l7tech.policy.assertion.CommentAssertion} is added to
 * a policy tree to prompt admin for assertion properties.
 */
public class CommentAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof CommentAssertion)) {
            throw new IllegalArgumentException();
        }
        CommentAssertion subject = (CommentAssertion) assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final CommentAssertionDialog dlg = new CommentAssertionDialog(mw, subject, false);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.isAssertionModified()) {
                    pc.proceed();
                }
            }
        });
    }
}
