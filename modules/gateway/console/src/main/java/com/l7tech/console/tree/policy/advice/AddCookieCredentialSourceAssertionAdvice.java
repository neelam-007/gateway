/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.panels.CookieCredentialSourceAssertionPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import java.awt.*;

/**
 * Advice that displays cookie assertion property dialog.
 */
public class AddCookieCredentialSourceAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof CookieCredentialSourceAssertion)) {
            throw new IllegalArgumentException();
        }
        final CookieCredentialSourceAssertion assertion = (CookieCredentialSourceAssertion)assertions[0];

        final Frame mw = TopComponents.getInstance().getTopParent();
        final CookieCredentialSourceAssertionPropertiesDialog dlg =
                new CookieCredentialSourceAssertionPropertiesDialog(mw, true, assertion, false);

        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isConfirmed()) {
                    dlg.getData(assertion);
                    pc.proceed();
                }
            }
        });
    }
}
