package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Regex;

import javax.swing.*;

/**
 * Invoked when a regex Assertion is dropped to a policy tree to  initiate the
 * Regex properties dialog.
 * <p/>
 */
public class RegexAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof Regex)) {
            throw new IllegalArgumentException();
        }
        JFrame f = TopComponents.getInstance().getMainWindow();
        /*

        Actions.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        //        dlg.setVisible(true);
        dlg.show();
        // check that user oked this dialog
        if (dlg.isAssertionChanged()) {
            pc.proceed();
        }*/
        pc.proceed();
    }
}
