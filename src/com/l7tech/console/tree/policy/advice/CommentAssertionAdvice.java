/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 28, 2005<br/>
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.CommentAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;

/**
 * Invoked when a {@link com.l7tech.policy.assertion.CommentAssertion} is added to
 * a policy tree to prompt admin for assertion properties.
 */
public class CommentAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof CommentAssertion)) {
            throw new IllegalArgumentException();
        }
        CommentAssertion subject = (CommentAssertion) assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        CommentAssertionDialog dlg = new CommentAssertionDialog(mw, subject);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // check that user oked this dialog
        if (dlg.isAssertionModified()) {
            pc.proceed();
        }
    }
}
