package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.RegexDialog;
import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Invoked when a regex Assertion is dropped to a policy tree to  initiate the
 * Regex properties dialog.
 * <p/>
 */
public class RegexAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof Regex)) {
            throw new IllegalArgumentException();
        }
        JFrame f = TopComponents.getInstance().getMainWindow();
        Regex r = (Regex)assertions[0];
        RegexDialog rd = new RegexDialog(f, r);
        rd.setModal(true);
        final Collection result = new ArrayList();
        rd.getBeanEditSupport().addBeanListener(new BeanAdapter() {
            public void onEditAccepted(Object source, Object bean) {
                result.add(Boolean.TRUE);
            }
        });

        Actions.setEscKeyStrokeDisposes(rd);
        rd.pack();
        rd.setSize(800, 600);
        Utilities.centerOnScreen(rd);
        rd.setVisible(true);
        if (!result.isEmpty()) {
            pc.proceed();
        }
    }
}
