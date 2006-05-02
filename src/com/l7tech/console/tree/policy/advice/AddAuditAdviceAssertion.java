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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

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

    public void proceed(PolicyChange pc) throws PolicyException {
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
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        AuditDetailAssertionPropertiesDialog aad = new AuditDetailAssertionPropertiesDialog(mw, subject, serverLevel);
        aad.pack();
        Utilities.centerOnScreen(aad);
        Utilities.setEscKeyStrokeDisposes(aad);
        aad.setVisible(true);
        if (aad.isModified()) {
            pc.proceed();
        }
    }
}
