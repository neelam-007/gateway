package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.RegexDialog;
import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import java.util.Collection;
import java.util.ArrayList;
import java.awt.*;

/**
 * Invoked when a regex Assertion is dropped to a policy tree to  initiate the
 * Regex properties dialog.
 * <p/>
 */
public class RegexAdvice extends AddContextSensitiveAssertionAdvice {
    private void showDialog(final PolicyChange pc, Regex regexAssertion, boolean postRouting) {
        Frame f = TopComponents.getInstance().getTopParent();
        RegexDialog rd = new RegexDialog(f, regexAssertion, postRouting, false);        
        rd.setModal(true);
        final Collection result = new ArrayList();
        rd.getBeanEditSupport().addBeanListener(new BeanAdapter() {
            public void onEditAccepted(Object source, Object bean) {
                result.add(Boolean.TRUE);
            }
        });

        Utilities.setEscKeyStrokeDisposes(rd);
        rd.pack();
        Utilities.centerOnScreen(rd);
        DialogDisplayer.display(rd, new Runnable() {
            public void run() {
                if (!result.isEmpty()) {
                    pc.proceed();
                }
            }
        });
    }

    protected void notifyPreRouting(PolicyChange pc, Assertion assertion) {
        Regex regexAssertion = (Regex) assertion;
        showDialog(pc, regexAssertion, false);
    }

    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) {
        Regex regexAssertion = (Regex) assertion;
        showDialog(pc, regexAssertion, true);
    }
}
