/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 28, 2005<br/>
 */
package com.l7tech.external.assertions.throughputquota.console.advice;

import com.l7tech.console.policy.PolicyPositionAware;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.throughputquota.ThroughputQuotaAssertion;
import com.l7tech.external.assertions.throughputquota.console.ThroughputQuotaForm;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;

import java.awt.*;

/**
 * Invoked when a throughput quota assertion is added to
 * a policy tree to prompt admin for assertion properties.
 *
 * @author flascelles@layer7-tech.com
 */
public class AddThroughputQuotaAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof ThroughputQuotaAssertion)) {
            throw new IllegalArgumentException();
        }
        final ThroughputQuotaAssertion subject = (ThroughputQuotaAssertion)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final ThroughputQuotaForm dlg = new ThroughputQuotaForm(mw, subject, false);
        dlg.setPolicyPosition( new PolicyPositionAware.PolicyPosition( pc.getParent().asAssertion(), pc.getChildLocation()));
        subject.setSynchronous(false); // sync defaults to false for newly-created assertions going forward
        subject.setReadSynchronous(false); //readSync defaults to false for newly-created assertions going forward
        dlg.setData(subject);
        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.wasOKed()) {
                    dlg.getData(subject);
                    pc.proceed();
                }
            }
        });
    }
}
