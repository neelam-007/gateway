package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.SqlAttackDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SqlAttackAssertion;

import java.awt.*;

/**
 * User: megery
 */
public class SqlAttackAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SqlAttackAssertion)) {
            throw new IllegalArgumentException();
        }
        SqlAttackAssertion subject = (SqlAttackAssertion) assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final SqlAttackDialog dlg = new SqlAttackDialog(mw, subject, true, false);

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
