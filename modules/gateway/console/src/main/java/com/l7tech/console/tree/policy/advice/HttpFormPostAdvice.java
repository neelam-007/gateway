package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.HttpFormPostDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpFormPost;

import java.awt.*;

/**
 * Invoked when an {@link HttpFormPost} Assertion is dropped to a policy tree to  initiate the
 * HTTP Form Post properties dialog.
 * <p/>
 */
public class HttpFormPostAdvice implements Advice {
    @Override
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof HttpFormPost)) {
            throw new IllegalArgumentException();
        }
        Frame f = TopComponents.getInstance().getTopParent();
        HttpFormPost hfp = (HttpFormPost)assertions[0];
        final HttpFormPostDialog hfpd = new HttpFormPostDialog(f, hfp, false);
        hfpd.setModal(true);
        hfpd.pack();
        Utilities.centerOnScreen(hfpd);
        DialogDisplayer.display(hfpd, new Runnable() {
            @Override
            public void run() {
                if (hfpd.isAssertionModified()) {
                    pc.proceed();
                }
            }
        });
    }
}
