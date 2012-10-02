package com.l7tech.external.assertions.comparison.console;

import com.l7tech.console.panels.EmailAlertPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import java.awt.*;

/**
 * Sets the treatVariableAsExpression setting to true for new assertions.
 *
 * @author jwilliams@layer7tech.com
 */
public class ComparisonAssertionAdvice implements Advice {

    @Override
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();

        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof ComparisonAssertion)) {
            throw new IllegalArgumentException();
        }

        ComparisonAssertion subject = (ComparisonAssertion) assertions[0];

        // Set the treatVariableAsExpression switch to true for new assertions
        subject.setTreatVariableAsExpression(true);

        final Frame mw = TopComponents.getInstance().getTopParent();
        final ComparisonPropertiesDialog dlg = new ComparisonPropertiesDialog(mw, subject);

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
