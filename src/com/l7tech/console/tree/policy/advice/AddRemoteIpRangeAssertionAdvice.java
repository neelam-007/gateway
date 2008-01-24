package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.RemoteIpRangePropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;

import java.awt.*;

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
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof RemoteIpRange)) {
            throw new IllegalArgumentException();
        }
        RemoteIpRange assertion = (RemoteIpRange)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final RemoteIpRangePropertiesDialog dlg = new RemoteIpRangePropertiesDialog(mw, true, false, assertion);
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
