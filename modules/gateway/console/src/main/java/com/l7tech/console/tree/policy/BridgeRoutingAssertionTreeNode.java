/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.BridgeRoutingAssertionPropertiesAction;
import com.l7tech.console.action.HttpRoutingAssertionPropertiesAction;
import com.l7tech.policy.assertion.HttpRoutingAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a policy tree node for a BridgeRoutingAssertion.
 */
public class BridgeRoutingAssertionTreeNode extends HttpRoutingAssertionTreeNode {
    public BridgeRoutingAssertionTreeNode(HttpRoutingAssertion assertion) {
        super(assertion);
    }

    @Override
    public String getName() {
        return super.getName(" using SecureSpan Bridge");
    }

    @Override
    public Action getPreferredAction() {
        return new BridgeRoutingAssertionPropertiesAction(this);
    }

    @Override
    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>(Arrays.asList(super.getActions()));
        for (Action action : list) {
            if (action instanceof HttpRoutingAssertionPropertiesAction) {
                list.remove(action);
                break;
            }
        }
        return list.toArray(new Action[list.size()]);
    }

    @Override
    protected boolean isUsingPrivateKey() {
        return true;
    }
}
