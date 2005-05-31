/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 28, 2005<br/>
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.ThroughputQuotaForm;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.common.gui.util.Utilities;

/**
 * Invoked when a throughput quota assertion is added to
 * a policy tree to prompt admin for assertion properties.
 *
 * @author flascelles@layer7-tech.com
 */
public class AddThroughputQuotaAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof ThroughputQuota)) {
            throw new IllegalArgumentException();
        }
        ThroughputQuota subject = (ThroughputQuota)assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        ThroughputQuotaForm dlg = new ThroughputQuotaForm(mw, subject, pc.getService());

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        // check that user oked this dialog
        if (dlg.wasOKed()) {
            pc.proceed();
        }
    }
}
