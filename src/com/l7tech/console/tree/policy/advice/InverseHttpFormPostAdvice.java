package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.InverseHttpFormPostDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.InverseHttpFormPost;

import javax.swing.*;
import java.awt.*;

/**
 * Invoked when an {@link com.l7tech.policy.assertion.HttpFormPost} Assertion is dropped to a policy tree to  initiate the
 * HTTP Form Post properties dialog.
 * <p/>
 */
public class InverseHttpFormPostAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof InverseHttpFormPost)) {
            throw new IllegalArgumentException();
        }
        Frame f = TopComponents.getInstance().getTopParent();
        InverseHttpFormPost hfp = (InverseHttpFormPost)assertions[0];
        final InverseHttpFormPostDialog hfpd = new InverseHttpFormPostDialog(f, hfp, false);
        hfpd.setModal(true);
        Utilities.setEscKeyStrokeDisposes(hfpd);
        hfpd.pack();
        Utilities.centerOnScreen(hfpd);
        DialogDisplayer.display(hfpd, new Runnable() {
            public void run() {
                if (hfpd.isAssertionModified()) {
                    pc.proceed();
                }
            }
        });
    }
}
