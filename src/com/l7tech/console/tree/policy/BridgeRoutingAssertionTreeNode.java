/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.BridgeRoutingAssertionPropertiesAction;
import com.l7tech.console.action.HttpRoutingAssertionPropertiesAction;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Represents a policy tree node for a BridgeRoutingAssertion.
 */
public class BridgeRoutingAssertionTreeNode extends HttpRoutingAssertionTreeNode {
    public BridgeRoutingAssertionTreeNode(RoutingAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return super.getName() + " using SecureSpan Bridge";
    }

    public Action getPreferredAction() {
        return new BridgeRoutingAssertionPropertiesAction(this);
    }

    public Action[] getActions() {
        java.util.List list = new ArrayList(Arrays.asList(super.getActions()));
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            Action action = (Action) iterator.next();
            if (action instanceof HttpRoutingAssertionPropertiesAction) {
                list.remove(action);
                break;
            }
        }
        list.add(0, getPreferredAction());
        return (Action[])list.toArray(new Action[]{});
    }
}
