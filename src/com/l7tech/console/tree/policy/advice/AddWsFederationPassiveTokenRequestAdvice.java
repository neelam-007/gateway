package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WsTrustCredentialExchangePropertiesDialog;
import com.l7tech.console.panels.WsFederationPassiveTokenRequestPropertiesDialog;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;

/**
 * Advice that is run to add a new WS-Federation PRP assertion.
 *
 * @author $Author$
 * @version $Revision$
 */
public class AddWsFederationPassiveTokenRequestAdvice implements Advice {

    //- PUBLIC

    /**
     *
     */
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof WsFederationPassiveTokenRequest)) {
            throw new IllegalArgumentException();
        }
        WsFederationPassiveTokenRequest assertion = (WsFederationPassiveTokenRequest) assertions[0];
        JFrame f = TopComponents.getInstance().getMainWindow();

        WsFederationPassiveTokenRequestPropertiesDialog dlg = new WsFederationPassiveTokenRequestPropertiesDialog(assertion, f, true);
        Actions.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // check that user oked this dialog
        if (dlg.isAssertionChanged()) {
            pc.proceed();
        }
    }
}
