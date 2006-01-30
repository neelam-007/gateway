/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 28, 2005<br/>
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.ComparisonAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ComparisonAssertion;

/**
 * Invoked when a {@link com.l7tech.policy.assertion.ComparisonAssertion} is added to
 * a policy tree to prompt admin for assertion properties.
 */
public class ComparisonAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof ComparisonAssertion)) {
            throw new IllegalArgumentException();
        }
        ComparisonAssertion subject = (ComparisonAssertion) assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        ComparisonAssertionDialog dlg = new ComparisonAssertionDialog(mw, subject);

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
