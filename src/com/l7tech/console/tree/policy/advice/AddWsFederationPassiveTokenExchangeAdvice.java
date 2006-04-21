package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WsFederationPassiveTokenExchangePropertiesDialog;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;

/**
 * Advice that is run to add a new WS-Federation PRP assertion.
 *
 * @author $Author$
 * @version $Revision$
 */
public class AddWsFederationPassiveTokenExchangeAdvice implements Advice {

    //- PUBLIC

    /**
     *
     */
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof WsFederationPassiveTokenExchange)) {
            throw new IllegalArgumentException();
        }
        WsFederationPassiveTokenExchange assertion = (WsFederationPassiveTokenExchange) assertions[0];
        JFrame f = TopComponents.getInstance().getMainWindow();

        WsFederationPassiveTokenExchangePropertiesDialog dlg = new WsFederationPassiveTokenExchangePropertiesDialog(assertion, f, true);
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // check that user oked this dialog
        if (dlg.isAssertionChanged()) {
            pc.proceed();
        }
    }
}
