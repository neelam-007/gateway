package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.HttpFormPostDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpFormPost;

import javax.swing.*;

/**
 * Invoked when an {@link HttpFormPost} Assertion is dropped to a policy tree to  initiate the
 * HTTP Form Post properties dialog.
 * <p/>
 */
public class HttpFormPostAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof HttpFormPost)) {
            throw new IllegalArgumentException();
        }
        JFrame f = TopComponents.getInstance().getMainWindow();
        HttpFormPost hfp = (HttpFormPost)assertions[0];
        HttpFormPostDialog hfpd = new HttpFormPostDialog(f, hfp);
        hfpd.setModal(true);
        Actions.setEscKeyStrokeDisposes(hfpd);
        hfpd.pack();
        Utilities.centerOnScreen(hfpd);
        hfpd.setVisible(true);
        if (hfpd.isAssertionModified()) {
            pc.proceed();
        }
    }
}
