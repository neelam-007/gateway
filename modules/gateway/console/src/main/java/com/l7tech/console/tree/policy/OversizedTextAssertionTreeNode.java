/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.OversizedTextDialogAction;
import com.l7tech.policy.assertion.OversizedTextAssertion;

import javax.swing.*;

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
        return "Document Structure Threats";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/OversizedElement16.gif";
    }

    public Action getPreferredAction() {
        return new OversizedTextDialogAction(this);
    }
}
