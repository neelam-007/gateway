/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.FaultLevelPropertiesAction;
import com.l7tech.policy.assertion.FaultLevel;

import javax.swing.*;

/**
 * Node in the policy tree that represents the {@link FaultLevel} assertion
 */
public class FaultLevelTreeNode extends LeafAssertionTreeNode {
    public FaultLevelTreeNode(FaultLevel assertion) {
        super(assertion);
    }

    public String getName() {
        return "Override SOAP Fault";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/disconnect.gif";
    }

    public Action getPreferredAction() {
        return new FaultLevelPropertiesAction(this);
    }
}
