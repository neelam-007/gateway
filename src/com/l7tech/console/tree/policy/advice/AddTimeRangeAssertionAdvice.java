package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.panels.TimeRangePropertiesDialog;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.common.gui.util.Utilities;

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
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof TimeRange)) {
            throw new IllegalArgumentException();
        }
        TimeRange subject = (TimeRange)assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        TimeRangePropertiesDialog dlg = new TimeRangePropertiesDialog(mw, true, subject);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        // check that user oked this dialog
        if (dlg.wasOked()) {
            pc.proceed();
        }
    }
}
