package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.SqlAttackDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SqlAttackAssertion;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 29, 2005
 * Time: 9:51:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class SqlAttackAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SqlAttackAssertion)) {
            throw new IllegalArgumentException();
        }
        SqlAttackAssertion subject = (SqlAttackAssertion) assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        SqlAttackDialog dlg = new SqlAttackDialog(mw, subject, true);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // check that user oked this dialog
        if (dlg.wasConfirmed()) {
            pc.proceed();
        }
    }
}
