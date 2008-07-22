/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.SetVariableAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetVariableAssertion;

import java.awt.*;

/**
 * Invoked when a {@link com.l7tech.policy.assertion.SetVariableAssertion} is added to
 * a policy tree to prompt admin for assertion properties.
 */
public class SetVariableAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SetVariableAssertion)) {
            throw new IllegalArgumentException();
        }
        SetVariableAssertion subject = (SetVariableAssertion) assertions[0];

        int newLocation = pc.getChildLocation();
        AssertionTreeNode parentNode = pc.getParent();
        Assertion beingInsertedAfter;

        if (newLocation == 0) {
            beingInsertedAfter = parentNode.asAssertion();
        } else {
            beingInsertedAfter = parentNode.getChildCount() >= newLocation ?
                ((AssertionTreeNode)parentNode.getChildAt(newLocation-1)).asAssertion() :
                ((AssertionTreeNode)parentNode.getChildAt(parentNode.getChildCount()-1)).asAssertion();
        }

        final Frame mw = TopComponents.getInstance().getTopParent();
        final SetVariableAssertionDialog dlg = new SetVariableAssertionDialog(mw, false, subject, beingInsertedAfter);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.isAssertionModified()) {
                    pc.proceed();
                }
            }
        });
    }
}
