/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.OversizedTextDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.common.gui.util.Utilities;

/**
 * Triggered when OversizedTextAssertion added to policy.
 */
public class OversizedTextAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof OversizedTextAssertion)) {
            throw new IllegalArgumentException();
        }
        OversizedTextAssertion subject = (OversizedTextAssertion) assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        OversizedTextDialog dlg = new OversizedTextDialog(mw, subject, true);

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
