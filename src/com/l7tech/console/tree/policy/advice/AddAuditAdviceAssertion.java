package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.AuditDetailAssertionPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.common.gui.util.Utilities;

/**
 * Invoked when an Audit Detail assertion is added to a policy tree
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AddAuditAdviceAssertion implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof AuditDetailAssertion)) {
            throw new IllegalArgumentException();
        }

        AuditDetailAssertion subject = (AuditDetailAssertion)assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        AuditDetailAssertionPropertiesDialog aad = new AuditDetailAssertionPropertiesDialog(mw, subject);
        aad.pack();
        Utilities.centerOnScreen(aad);
        Utilities.setEscKeyStrokeDisposes(aad);
        aad.setVisible(true);
        if (aad.isModified()) {
            pc.proceed();
        }
    }
}
