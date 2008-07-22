package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WsFederationPassiveTokenRequestPropertiesDialog;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import java.awt.*;

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
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof WsFederationPassiveTokenRequest)) {
            throw new IllegalArgumentException();
        }
        final WsFederationPassiveTokenRequest assertion = (WsFederationPassiveTokenRequest) assertions[0];
        Frame f = TopComponents.getInstance().getTopParent();

        final WsFederationPassiveTokenRequestPropertiesDialog dlg = new WsFederationPassiveTokenRequestPropertiesDialog(assertion, true, f, true, false);
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.isAssertionChanged()) {
                    if (!dlg.isTokenRequest()) {
                        WsFederationPassiveTokenExchange wsFederationPassiveTokenExchange = new WsFederationPassiveTokenExchange();
                        wsFederationPassiveTokenExchange.copyFrom(assertion);
                        pc.getNewChild().setUserObject(wsFederationPassiveTokenExchange);
                    }
                    pc.proceed();
                }
            }
        });
    }
}
