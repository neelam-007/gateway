/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.panels.OversizedTextDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

import java.awt.*;

/**
 * Triggered when OversizedTextAssertion added to policy.
 */
public class OversizedTextAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof OversizedTextAssertion)) {
            throw new IllegalArgumentException();
        }
        OversizedTextAssertion subject = (OversizedTextAssertion) assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final OversizedTextDialog dlg = new OversizedTextDialog(mw, subject, true, false);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.wasConfirmed()) {
                    pc.proceed();
                }
            }
        });
    }
}
