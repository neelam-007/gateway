package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.RemoteIpRangePropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;

/**
 * Invoked when a RemoteIpRange assertion node is dropped on a policy tree.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 24, 2004<br/>
 * $Id$<br/>
 *
 */
public class AddRemoteIpRangeAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof RemoteIpRange)) {
            throw new IllegalArgumentException();
        }
        RemoteIpRange assertion = (RemoteIpRange)assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        RemoteIpRangePropertiesDialog dlg = new RemoteIpRangePropertiesDialog(mw, true, assertion);
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
