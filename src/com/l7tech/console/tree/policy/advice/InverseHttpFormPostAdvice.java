package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.InverseHttpFormPostDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.InverseHttpFormPost;

import javax.swing.*;

/**
 * Invoked when an {@link com.l7tech.policy.assertion.HttpFormPost} Assertion is dropped to a policy tree to  initiate the
 * HTTP Form Post properties dialog.
 * <p/>
 */
public class InverseHttpFormPostAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof InverseHttpFormPost)) {
            throw new IllegalArgumentException();
        }
        JFrame f = TopComponents.getInstance().getMainWindow();
        InverseHttpFormPost hfp = (InverseHttpFormPost)assertions[0];
        InverseHttpFormPostDialog hfpd = new InverseHttpFormPostDialog(f, hfp);
        hfpd.setModal(true);
        Utilities.setEscKeyStrokeDisposes(hfpd);
        hfpd.pack();
        Utilities.centerOnScreen(hfpd);
        hfpd.setVisible(true);
        if (hfpd.isAssertionModified()) {
            pc.proceed();
        }
    }
}
