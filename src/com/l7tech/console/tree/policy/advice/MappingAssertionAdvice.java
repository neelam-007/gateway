package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.MappingAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MappingAssertion;

/**
 * Advice for dragging a MappingAssertion into the tree.
 */
public class MappingAssertionAdvice implements Advice{
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof MappingAssertion)) {
            throw new IllegalArgumentException();
        }
        MappingAssertion subject = (MappingAssertion) assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        MappingAssertionDialog dlg = new MappingAssertionDialog(mw, subject, true);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // check that user oked this dialog
        if (dlg.isModified()) {
            pc.proceed();
        }
    }
}
