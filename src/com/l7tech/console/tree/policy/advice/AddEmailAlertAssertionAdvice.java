package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.EmailAlertPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

/**
 * Invoked when a EmailAlertAssertion is dropped to a policy tree to prompt
 * an administrator for a value.
 */
public class AddEmailAlertAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof EmailAlertAssertion)) {
            throw new IllegalArgumentException();
        }
        EmailAlertAssertion subject = (EmailAlertAssertion)assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        EmailAlertPropertiesDialog dlg = new EmailAlertPropertiesDialog(mw, subject);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        // check that user oked this dialog
        if (dlg.getResult() != null) {
            pc.proceed();
        }
    }
}
