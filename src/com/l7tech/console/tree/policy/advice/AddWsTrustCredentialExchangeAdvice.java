package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.proxy.gui.dialogs.WsTrustCredentialExchangePropertiesDialog;

import javax.swing.JFrame;

/**
 * Invoked when a WS-Trust Credential Exchange Assertion is dropped to a policy tree to
 * initiate the properties dialog.
 * <p/>
 */
public class AddWsTrustCredentialExchangeAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof WsTrustCredentialExchange)) {
            throw new IllegalArgumentException();
        }
        WsTrustCredentialExchange assertion = (WsTrustCredentialExchange)assertions[0];
        JFrame f = TopComponents.getInstance().getMainWindow();

        WsTrustCredentialExchangePropertiesDialog dlg = new WsTrustCredentialExchangePropertiesDialog(assertion, f, true);
        Actions.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
//        dlg.setVisible(true);
        dlg.show();
        // check that user oked this dialog
        if (dlg.isAssertionChanged()) {
            pc.proceed();
        }
    }
}
