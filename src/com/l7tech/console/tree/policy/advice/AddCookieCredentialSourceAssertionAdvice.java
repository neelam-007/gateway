/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.panels.CookieCredentialSourceAssertionPropertiesDialog;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.common.gui.util.Utilities;

/**
 * Advice that displays cookie assertion property dialog.
 */
public class AddCookieCredentialSourceAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof CookieCredentialSourceAssertion)) {
            throw new IllegalArgumentException();
        }
        CookieCredentialSourceAssertion assertion = (CookieCredentialSourceAssertion)assertions[0];

        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        CookieCredentialSourceAssertionPropertiesDialog dlg =
                new CookieCredentialSourceAssertionPropertiesDialog(mw, true, assertion);

        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            dlg.getData(assertion);
            pc.proceed();
        }
    }
}
