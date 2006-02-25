/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.OversizedTextDialogAction;
import com.l7tech.policy.assertion.OversizedTextAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Policy tree node for OversizedTextAssertion.
 */
public class OversizedTextAssertionTreeNode extends LeafAssertionTreeNode{
    /**
     * Instantiate the new <code>LeafAssertionTreeNode</code>
     * with the given assertion.
     *
     * @param assertion the assertion
     */
    public OversizedTextAssertionTreeNode(OversizedTextAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "Oversized Element Protection";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/SQLProtection16x16.gif";
    }

    public Action getPreferredAction() {
        return new OversizedTextDialogAction(this);
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        List list = new ArrayList();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[list.size()]);
    }

}
