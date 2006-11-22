package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.AuditDetailAssertionPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.awt.*;

/**
 * Invoked when an Audit Detail assertion is added to a policy tree
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AddAuditAdviceAssertion implements Advice {
    private static final Logger logger = Logger.getLogger(AddAuditAdviceAssertion.class.getName());

    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof AuditDetailAssertion)) {
            throw new IllegalArgumentException();
        }

        Level serverLevel;
        try {
            serverLevel = Registry.getDefault().getAuditAdmin().serverDetailAuditThreshold();
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Couldn't get server's detail threshold; using default: " + Level.INFO.getName(), e);
            serverLevel = Level.INFO;
        }

        AuditDetailAssertion subject = (AuditDetailAssertion)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final AuditDetailAssertionPropertiesDialog aad = new AuditDetailAssertionPropertiesDialog(mw, subject, serverLevel);
        aad.pack();
        Utilities.centerOnScreen(aad);
        Utilities.setEscKeyStrokeDisposes(aad);
        DialogDisplayer.display(aad, new Runnable() {
            public void run() {
                if (aad.isModified())
                    pc.proceed();
            }
        });
    }
}
