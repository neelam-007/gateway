package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.panels.TimeRangePropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import java.awt.*;

/**
 * Invoked when a TimeRange assertion is dropped to a policy tree to prompt
 * an administrator for a value.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 27, 2004<br/>
 * $Id$<br/>
 */
public class AddTimeRangeAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof TimeRange)) {
            throw new IllegalArgumentException();
        }
        TimeRange subject = (TimeRange)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final TimeRangePropertiesDialog dlg = new TimeRangePropertiesDialog(mw, true, false, subject);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.wasOked()) {
                    pc.proceed();
                }
            }
        });
    }
}
