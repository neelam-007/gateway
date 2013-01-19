package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.panels.SqlAttackDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.policy.assertion.TargetMessageType;

import java.awt.*;

/**
 * @author jwilliams@layer7tech.com
 */
public class SqlAttackAssertionAdvice implements Advice {

    @Override
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();

        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SqlAttackAssertion)) {
            throw new IllegalArgumentException();
        }

        SqlAttackAssertion subject = (SqlAttackAssertion) assertions[0];

        // Set body inclusion to true as default action for new assertions
        subject.setIncludeBody(true);

        // Set the includeRequestUrl switch to true for new Request-targeted assertions, false otherwise
        subject.setIncludeUrl(subject.getTarget() == TargetMessageType.REQUEST);

        final Frame mw = TopComponents.getInstance().getTopParent();
        final SqlAttackDialog dlg = new SqlAttackDialog(mw, subject);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                // check that user confirmed the new properties
                if (dlg.isConfirmed()) {
                    pc.proceed();
                }
            }
        });
    }
}
